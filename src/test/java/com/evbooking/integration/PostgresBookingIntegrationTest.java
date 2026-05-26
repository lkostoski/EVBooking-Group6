package com.evbooking.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.evbooking.exception.BookingConflictException;
import com.evbooking.model.AvailableSlot;
import com.evbooking.model.Booking;
import com.evbooking.model.BookingStatus;
import com.evbooking.model.ChargingStation;
import com.evbooking.model.Connector;
import com.evbooking.model.Role;
import com.evbooking.model.User;
import com.evbooking.model.UserSession;
import com.evbooking.repository.BookingDAO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

class PostgresBookingIntegrationTest {

    private static EntityManagerFactory emf;
    private static BookingDAO bookingDAO;

    @BeforeAll
    static void beforeAll() throws Exception {
        assumeTrue(runIntegrationTests(),
            "Set RUN_DB_INTEGRATION_TESTS=true and TEST_JDBC_URL to run PostgreSQL integration tests");

        applySchema();
        emf = Persistence.createEntityManagerFactory("evbooking-pu", jpaProps());
        bookingDAO = new BookingDAO(emf);
    }

    @AfterAll
    static void afterAll() {
        if (emf != null && emf.isOpen()) emf.close();
    }

    @BeforeEach
    void resetData() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("DELETE FROM bookings").executeUpdate();
            em.createNativeQuery("DELETE FROM available_slots").executeUpdate();
            em.createNativeQuery("DELETE FROM connectors").executeUpdate();
            em.createNativeQuery("DELETE FROM charging_stations").executeUpdate();
            em.createNativeQuery("DELETE FROM user_sessions").executeUpdate();
            em.createNativeQuery("DELETE FROM users").executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    @Test
    @DisplayName("schema supports server-side session persistence")
    void schemaPersistsServerSideSessions() {
        User user = persistUser("nikos", Role.DRIVER);

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(new UserSession("session-1", em.merge(user), java.time.LocalDateTime.now().plusHours(1)));
            em.getTransaction().commit();

            assertEquals(1L, em.createQuery("SELECT COUNT(s) FROM UserSession s", Long.class).getSingleResult());
        } finally {
            em.close();
        }
    }

    @Test
    @DisplayName("BookingDAO persists a valid booking against PostgreSQL")
    void bookingDaoPersistsValidBooking() {
        Seed seed = seedConnectorWithSlot("nikos", "maria");

        Booking created = bookingDAO.createAtomic(
            seed.nikos, seed.station, seed.connectorA,
            seed.date, LocalTime.of(8, 0), LocalTime.of(9, 0)
        );

        assertTrue(created.getBookingId() > 0);
        assertEquals(1, bookingDAO.findAll().size());
        assertEquals("nikos", bookingDAO.findById(created.getBookingId()).getUser().getUsername());
    }

    @Test
    @DisplayName("same driver cannot hold overlapping active bookings on different connectors")
    void sameDriverOverlapIsRejectedByPostgresDao() {
        Seed seed = seedTwoConnectorsWithSlots("nikos", "maria");
        bookingDAO.createAtomic(seed.nikos, seed.station, seed.connectorA,
            seed.date, LocalTime.of(8, 0), LocalTime.of(9, 0));

        org.junit.jupiter.api.Assertions.assertThrows(BookingConflictException.class,
            () -> bookingDAO.createAtomic(seed.nikos, seed.station, seed.connectorB,
                seed.date, LocalTime.of(8, 30), LocalTime.of(9, 30)));
    }

    @Test
    @DisplayName("concurrent attempts to book the same connector/time allow only one success")
    void concurrentSameConnectorBookingAllowsOnlyOneSuccess() throws Exception {
        Seed seed = seedConnectorWithSlot("nikos", "maria");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Boolean> nikosAttempt = () -> attemptBooking(start, seed.nikos, seed);
            Callable<Boolean> mariaAttempt = () -> attemptBooking(start, seed.maria, seed);

            Future<Boolean> a = pool.submit(nikosAttempt);
            Future<Boolean> b = pool.submit(mariaAttempt);
            start.countDown();

            int successes = (a.get() ? 1 : 0) + (b.get() ? 1 : 0);
            assertEquals(1, successes);

            List<Booking> bookings = bookingDAO.findAll();
            assertEquals(1, bookings.size());
            assertEquals(BookingStatus.ACTIVE, bookings.get(0).getStatus());
        } finally {
            pool.shutdownNow();
        }
    }

    private static boolean attemptBooking(CountDownLatch start, User user, Seed seed) throws Exception {
        start.await();
        try {
            bookingDAO.createAtomic(user, seed.station, seed.connectorA,
                seed.date, LocalTime.of(8, 0), LocalTime.of(9, 0));
            return true;
        } catch (BookingConflictException expected) {
            return false;
        }
    }

    private static Seed seedConnectorWithSlot(String firstDriver, String secondDriver) {
        return seed(false, firstDriver, secondDriver);
    }

    private static Seed seedTwoConnectorsWithSlots(String firstDriver, String secondDriver) {
        return seed(true, firstDriver, secondDriver);
    }

    private static Seed seed(boolean secondConnector, String firstDriver, String secondDriver) {
        LocalDate date = LocalDate.now().plusDays(1);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            User nikos = new User(firstDriver, "hash", Role.DRIVER);
            User maria = new User(secondDriver, "hash", Role.DRIVER);
            em.persist(nikos);
            em.persist(maria);

            ChargingStation station = new ChargingStation("Central", "Central address", 40.0, 22.0);
            em.persist(station);

            Connector connectorA = new Connector("CCS", station);
            em.persist(connectorA);
            em.persist(new AvailableSlot(connectorA, date, LocalTime.of(8, 0), LocalTime.of(10, 0)));

            Connector connectorB = null;
            if (secondConnector) {
                connectorB = new Connector("Type 2", station);
                em.persist(connectorB);
                em.persist(new AvailableSlot(connectorB, date, LocalTime.of(8, 0), LocalTime.of(10, 0)));
            }

            em.getTransaction().commit();
            return new Seed(nikos, maria, station, connectorA, connectorB, date);
        } finally {
            em.close();
        }
    }

    private static User persistUser(String username, Role role) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            User user = new User(username, "hash", role);
            em.persist(user);
            em.getTransaction().commit();
            return user;
        } finally {
            em.close();
        }
    }

    private static void applySchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                require("TEST_JDBC_URL"),
                envOrDefault("TEST_DB_USER", ""),
                envOrDefault("TEST_DB_PASSWORD", ""));
             Statement statement = connection.createStatement()) {
            for (String sql : Files.readString(Path.of("schema.sql")).split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) statement.execute(trimmed);
            }
        }
    }

    private static Map<String, String> jpaProps() {
        Map<String, String> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", require("TEST_JDBC_URL"));
        props.put("jakarta.persistence.jdbc.user", envOrDefault("TEST_DB_USER", ""));
        props.put("jakarta.persistence.jdbc.password", envOrDefault("TEST_DB_PASSWORD", ""));
        props.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        props.put("hibernate.hbm2ddl.auto", "validate");
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.hikari.maximumPoolSize", "5");
        return props;
    }

    private static boolean runIntegrationTests() {
        return "true".equalsIgnoreCase(System.getenv("RUN_DB_INTEGRATION_TESTS"))
            && System.getenv("TEST_JDBC_URL") != null
            && !System.getenv("TEST_JDBC_URL").isBlank();
    }

    private static String require(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value;
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

    private static final class Seed {
        final User nikos;
        final User maria;
        final ChargingStation station;
        final Connector connectorA;
        final Connector connectorB;
        final LocalDate date;

        Seed(User nikos, User maria, ChargingStation station,
             Connector connectorA, Connector connectorB, LocalDate date) {
            this.nikos = nikos;
            this.maria = maria;
            this.station = station;
            this.connectorA = connectorA;
            this.connectorB = connectorB;
            this.date = date;
        }
    }
}
