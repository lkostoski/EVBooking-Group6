package com.evbooking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.evbooking.dto.BookingRequest;
import com.evbooking.dto.BookingResponse;
import com.evbooking.dto.BookingUpdateRequest;
import com.evbooking.exception.BadRequestException;
import com.evbooking.exception.ForbiddenException;
import com.evbooking.model.Booking;
import com.evbooking.model.BookingStatus;
import com.evbooking.model.ChargingStation;
import com.evbooking.model.Connector;
import com.evbooking.model.Role;
import com.evbooking.model.User;
import com.evbooking.repository.BookingDAO;
import com.evbooking.repository.ChargingStationDAO;
import com.evbooking.repository.ConnectorDAO;
import com.evbooking.repository.UserDAO;

class BookingServiceFlowTest {

    @Test
    @DisplayName("driver listing returns only that driver's bookings; admin listing returns all")
    void listHonoursDriverVsAdminVisibility() {
        Fixture f = new Fixture();

        assertEquals(1, f.service.listForUser("nikos", false).size());
        assertEquals(2, f.service.listForUser("admin", true).size());
    }

    @Test
    @DisplayName("driver cannot view another driver's booking")
    void getByIdRejectsDifferentDriver() {
        Fixture f = new Fixture();

        assertThrows(ForbiddenException.class,
            () -> f.service.getById(2L, "nikos", false));
    }

    @Test
    @DisplayName("admin can view any booking")
    void adminCanViewAnyBooking() {
        Fixture f = new Fixture();

        BookingResponse response = f.service.getById(2L, "admin", true);

        assertEquals("maria", response.getUsername());
    }

    @Test
    @DisplayName("create rejects a connector that does not belong to the selected station")
    void createRejectsConnectorFromDifferentStation() {
        Fixture f = new Fixture();
        BookingRequest req = request(99L, 10L);

        assertThrows(BadRequestException.class,
            () -> f.service.create(req, "nikos"));
    }

    @Test
    @DisplayName("create delegates valid booking to atomic DAO operation")
    void createUsesAtomicDaoForValidBooking() {
        Fixture f = new Fixture();

        BookingResponse response = f.service.create(request(1L, 10L), "nikos");

        assertEquals("nikos", response.getUsername());
        assertEquals("CCS", response.getConnectorType());
        assertEquals(3, f.bookingDAO.bookings.size());
    }

    @Test
    @DisplayName("admin can create a booking for another driver")
    void adminCanCreateBookingForAnotherDriver() {
        Fixture f = new Fixture();
        BookingRequest req = request(1L, 10L);
        req.setDriverUsername("maria");

        BookingResponse response = f.service.create(req, "admin", true);

        assertEquals("maria", response.getUsername());
    }

    @Test
    @DisplayName("driver cannot create a booking for another driver by setting driverUsername")
    void driverCreateIgnoresSubmittedDriverUsername() {
        Fixture f = new Fixture();
        BookingRequest req = request(1L, 10L);
        req.setDriverUsername("maria");

        BookingResponse response = f.service.create(req, "nikos", false);

        assertEquals("nikos", response.getUsername());
    }

    @Test
    @DisplayName("admin create fails clearly when requested driver does not exist")
    void adminCreateRejectsMissingDriver() {
        Fixture f = new Fixture();
        BookingRequest req = request(1L, 10L);
        req.setDriverUsername("missing");

        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> f.service.create(req, "admin", true));

        assertEquals("Driver user not found", ex.getMessage());
    }

    @Test
    @DisplayName("admin can modify another driver's active booking")
    void adminCanModifyAnyBooking() {
        Fixture f = new Fixture();
        BookingUpdateRequest req = updateRequest(LocalTime.of(10, 0), LocalTime.of(11, 0));

        BookingResponse response = f.service.update(2L, req, "admin", true);

        assertEquals("maria", response.getUsername());
        assertEquals(LocalTime.of(10, 0), response.getStartTime());
        assertEquals(LocalTime.of(11, 0), response.getEndTime());
    }

    @Test
    @DisplayName("driver cannot modify another driver's active booking")
    void driverCannotModifyAnotherDriversBooking() {
        Fixture f = new Fixture();
        BookingUpdateRequest req = updateRequest(LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertThrows(ForbiddenException.class,
            () -> f.service.update(2L, req, "nikos", false));
    }

    private static BookingRequest request(Long stationId, Long connectorId) {
        BookingRequest req = new BookingRequest();
        req.setStationId(stationId);
        req.setConnectorId(connectorId);
        req.setDate(LocalDate.now().plusDays(1));
        req.setStartTime(LocalTime.of(8, 0));
        req.setEndTime(LocalTime.of(9, 0));
        return req;
    }

    private static BookingUpdateRequest updateRequest(LocalTime start, LocalTime end) {
        BookingUpdateRequest req = new BookingUpdateRequest();
        req.setDate(LocalDate.now().plusDays(1));
        req.setStartTime(start);
        req.setEndTime(end);
        return req;
    }

    private static final class Fixture {
        final User nikos = new User("nikos", "hash", Role.DRIVER);
        final User maria = new User("maria", "hash", Role.DRIVER);
        final User admin = new User("admin", "hash", Role.ADMIN);
        final ChargingStation station = station(1L, "Syntagma");
        final ChargingStation otherStation = station(99L, "Airport");
        final Connector connector = connector(10L, "CCS", station);
        final FakeBookingDAO bookingDAO = new FakeBookingDAO();
        final BookingService service;

        Fixture() {
            bookingDAO.bookings.add(booking(1L, nikos, station, connector));
            bookingDAO.bookings.add(booking(2L, maria, station, connector));
            service = new BookingService(
                bookingDAO,
                new FakeUserDAO(List.of(nikos, maria, admin)),
                new FakeStationDAO(List.of(station, otherStation)),
                new FakeConnectorDAO(List.of(connector))
            );
        }
    }

    private static ChargingStation station(Long id, String name) {
        ChargingStation station = new ChargingStation(name, name + " address", 40.0, 22.0);
        station.setStationId(id);
        return station;
    }

    private static Connector connector(Long id, String type, ChargingStation station) {
        Connector connector = new Connector(type, station);
        connector.setConnectorId(id);
        return connector;
    }

    private static Booking booking(Long id, User user, ChargingStation station, Connector connector) {
        Booking booking = new Booking(
            user, station, connector,
            LocalDate.now().plusDays(1),
            LocalTime.of(8, 0),
            LocalTime.of(9, 0),
            BookingStatus.ACTIVE
        );
        booking.setBookingId(id);
        return booking;
    }

    private static final class FakeBookingDAO extends BookingDAO {
        final List<Booking> bookings = new ArrayList<>();

        FakeBookingDAO() {
            super(null);
        }

        @Override
        public List<Booking> findByUsername(String username) {
            return bookings.stream()
                .filter(b -> b.getUser().getUsername().equals(username))
                .toList();
        }

        @Override
        public List<Booking> findAll() {
            return bookings;
        }

        @Override
        public Booking findById(Long id) {
            return bookings.stream()
                .filter(b -> b.getBookingId().equals(id))
                .findFirst()
                .orElse(null);
        }

        @Override
        public Booking createAtomic(User user, ChargingStation station, Connector connector,
                                    LocalDate date, LocalTime startTime, LocalTime endTime) {
            Booking booking = new Booking(user, station, connector, date, startTime, endTime, BookingStatus.ACTIVE);
            booking.setBookingId((long) bookings.size() + 1);
            bookings.add(booking);
            return booking;
        }

        @Override
        public Booking updateAtomic(Long bookingId, LocalDate date, LocalTime startTime, LocalTime endTime,
                                    String requestingUsername, boolean isAdmin) {
            Booking booking = findById(bookingId);
            if (booking == null) {
                return null;
            }
            if (!isAdmin && !booking.getUser().getUsername().equals(requestingUsername)) {
                throw new ForbiddenException("Access denied");
            }
            booking.setDate(date);
            booking.setStartTime(startTime);
            booking.setEndTime(endTime);
            return booking;
        }
    }

    private static final class FakeUserDAO extends UserDAO {
        private final List<User> users;
        FakeUserDAO(List<User> users) { this.users = users; }
        @Override
        public User findByUsername(String username) {
            return users.stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(null);
        }
    }

    private static final class FakeStationDAO extends ChargingStationDAO {
        private final List<ChargingStation> stations;
        FakeStationDAO(List<ChargingStation> stations) { this.stations = stations; }
        @Override
        public ChargingStation findById(Long id) {
            return stations.stream().filter(s -> s.getStationId().equals(id)).findFirst().orElse(null);
        }
    }

    private static final class FakeConnectorDAO extends ConnectorDAO {
        private final List<Connector> connectors;
        FakeConnectorDAO(List<Connector> connectors) { this.connectors = connectors; }
        @Override
        public Connector findById(Long id) {
            return connectors.stream().filter(c -> c.getConnectorId().equals(id)).findFirst().orElse(null);
        }
    }
}
