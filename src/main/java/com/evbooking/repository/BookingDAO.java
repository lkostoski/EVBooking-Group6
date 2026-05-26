package com.evbooking.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.evbooking.exception.BadRequestException;
import com.evbooking.exception.BookingConflictException;
import com.evbooking.exception.ForbiddenException;
import com.evbooking.exception.NotFoundException;
import com.evbooking.model.Booking;
import com.evbooking.model.BookingStatus;
import com.evbooking.model.ChargingStation;
import com.evbooking.model.Connector;
import com.evbooking.model.User;
import com.evbooking.util.HibernateUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;

public class BookingDAO {

    private final EntityManagerFactory emf;

    public BookingDAO() {
        this(HibernateUtil.getEntityManagerFactory());
    }

    public BookingDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public Booking findById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            // Eager-fetch related entities so the DTO mapper does not trigger lazy loads
            // after the EntityManager has closed.
            List<Booking> result = em.createQuery(
                "SELECT b FROM Booking b " +
                "JOIN FETCH b.user " +
                "JOIN FETCH b.chargingStation " +
                "JOIN FETCH b.connector " +
                "WHERE b.bookingId = :id",
                Booking.class)
                .setParameter("id", id)
                .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    public List<Booking> findByUsername(String username) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT b FROM Booking b " +
                "JOIN FETCH b.user " +
                "JOIN FETCH b.chargingStation " +
                "JOIN FETCH b.connector " +
                "WHERE b.user.username = :username " +
                "ORDER BY b.date DESC, b.startTime DESC",
                Booking.class)
                .setParameter("username", username)
                .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Booking> findAll() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                "SELECT b FROM Booking b " +
                "JOIN FETCH b.user " +
                "JOIN FETCH b.chargingStation " +
                "JOIN FETCH b.connector " +
                "ORDER BY b.date DESC, b.startTime DESC",
                Booking.class).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Atomic booking creation. The overlap check, driver-overlap check, slot membership
     * check and the insert all run inside a single transaction with a pessimistic lock
     * on the connector row, which serialises concurrent attempts and closes the
     * check-then-act race that an unlocked check + persist would leave open.
     */
    public Booking createAtomic(User user,
                                ChargingStation station,
                                Connector connectorRef,
                                LocalDate date,
                                LocalTime startTime,
                                LocalTime endTime) {

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            // 1a. Lock the user row first. This serialises ALL concurrent booking
            //     attempts by the same driver, regardless of connector — closing the
            //     cross-connector race that an isolated connector lock cannot.
            //     Locking user-before-connector is a consistent order: no deadlocks.
            User managedUser = em.find(
                User.class,
                user.getUsername(),
                LockModeType.PESSIMISTIC_WRITE
            );
            if (managedUser == null) {
                throw new NotFoundException("User not found");
            }

            // 1b. Lock the connector row. Two concurrent attempts on the SAME
            //     connector (by different drivers) serialise here.
            Connector connector = em.find(
                Connector.class,
                connectorRef.getConnectorId(),
                LockModeType.PESSIMISTIC_WRITE
            );
            if (connector == null) {
                throw new NotFoundException("Connector not found");
            }

            // 2. Booking must fall within a published available slot for this
            //    connector/date.
            Long slotMatches = em.createQuery(
                "SELECT COUNT(s) FROM AvailableSlot s " +
                "WHERE s.connector.connectorId = :connectorId " +
                "AND s.date = :date " +
                "AND s.startTime <= :startTime " +
                "AND s.endTime   >= :endTime",
                Long.class)
                .setParameter("connectorId", connector.getConnectorId())
                .setParameter("date", date)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getSingleResult();
            if (slotMatches == 0) {
                throw new BadRequestException(
                    "Requested time does not fall within a published available slot");
            }

            // 3. No other ACTIVE booking on this connector may overlap.
            Long connectorOverlap = em.createQuery(
                "SELECT COUNT(b) FROM Booking b " +
                "WHERE b.connector.connectorId = :connectorId " +
                "AND b.date = :date " +
                "AND b.status = :status " +
                "AND b.startTime < :endTime " +
                "AND b.endTime   > :startTime",
                Long.class)
                .setParameter("connectorId", connector.getConnectorId())
                .setParameter("date", date)
                .setParameter("status", BookingStatus.ACTIVE)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getSingleResult();
            if (connectorOverlap > 0) {
                throw new BookingConflictException("Slot already booked");
            }

            // 4. The same driver may not hold two overlapping ACTIVE bookings,
            //    regardless of which connector they are on (business rule from
            //    the brief).
            Long driverOverlap = em.createQuery(
                "SELECT COUNT(b) FROM Booking b " +
                "WHERE b.user.username = :username " +
                "AND b.date = :date " +
                "AND b.status = :status " +
                "AND b.startTime < :endTime " +
                "AND b.endTime   > :startTime",
                Long.class)
                .setParameter("username", user.getUsername())
                .setParameter("date", date)
                .setParameter("status", BookingStatus.ACTIVE)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getSingleResult();
            if (driverOverlap > 0) {
                throw new BookingConflictException(
                    "You already have another booking in this time period");
            }

            ChargingStation managedStation = em.find(ChargingStation.class, station.getStationId());

            Booking booking = new Booking(
                managedUser, managedStation, connector,
                date, startTime, endTime,
                BookingStatus.ACTIVE
            );

            em.persist(booking);
            em.getTransaction().commit();
            return booking;

        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    /**
     * Atomic update with the same race-safety guarantees as createAtomic plus
     * driver/admin checks and "cannot modify after start time" enforced inside the
     * transaction.
     */
    public Booking updateAtomic(Long bookingId,
                                LocalDate newDate,
                                LocalTime newStart,
                                LocalTime newEnd,
                                String requestingUsername,
                                boolean isAdmin) {

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Booking booking = em.find(Booking.class, bookingId, LockModeType.PESSIMISTIC_WRITE);
            if (booking == null) {
                throw new NotFoundException("Booking not found");
            }
            if (!isAdmin && !booking.getUser().getUsername().equals(requestingUsername)) {
                throw new ForbiddenException("Access denied");
            }
            if (booking.getStatus() != BookingStatus.ACTIVE) {
                throw new BadRequestException("Only active bookings can be modified");
            }
            if (booking.getDate().atTime(booking.getStartTime())
                    .isBefore(java.time.LocalDateTime.now())) {
                throw new BadRequestException("Cannot modify a booking that has already started");
            }

            // Lock the booking owner's user row so that concurrent updates and
            // creates for this driver across any connector serialise here.
            em.find(User.class, booking.getUser().getUsername(), LockModeType.PESSIMISTIC_WRITE);

            Long connectorOverlap = em.createQuery(
                "SELECT COUNT(b) FROM Booking b " +
                "WHERE b.connector.connectorId = :connectorId " +
                "AND b.bookingId != :id " +
                "AND b.date = :date " +
                "AND b.status = :status " +
                "AND b.startTime < :endTime " +
                "AND b.endTime   > :startTime",
                Long.class)
                .setParameter("connectorId", booking.getConnector().getConnectorId())
                .setParameter("id", bookingId)
                .setParameter("date", newDate)
                .setParameter("status", BookingStatus.ACTIVE)
                .setParameter("startTime", newStart)
                .setParameter("endTime", newEnd)
                .getSingleResult();
            if (connectorOverlap > 0) {
                throw new BookingConflictException("Slot already booked");
            }

            Long driverOverlap = em.createQuery(
                "SELECT COUNT(b) FROM Booking b " +
                "WHERE b.user.username = :username " +
                "AND b.bookingId != :id " +
                "AND b.date = :date " +
                "AND b.status = :status " +
                "AND b.startTime < :endTime " +
                "AND b.endTime   > :startTime",
                Long.class)
                .setParameter("username", booking.getUser().getUsername())
                .setParameter("id", bookingId)
                .setParameter("date", newDate)
                .setParameter("status", BookingStatus.ACTIVE)
                .setParameter("startTime", newStart)
                .setParameter("endTime", newEnd)
                .getSingleResult();
            if (driverOverlap > 0) {
                throw new BookingConflictException(
                    "You already have another booking in this time period");
            }

            booking.setDate(newDate);
            booking.setStartTime(newStart);
            booking.setEndTime(newEnd);

            em.getTransaction().commit();

            // Reload with eager fetch so the DTO mapper can read related entities
            // without lazy loading.
            return findById(bookingId);

        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public void cancel(Long bookingId, String requestingUsername, boolean isAdmin) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Booking booking = em.find(Booking.class, bookingId, LockModeType.PESSIMISTIC_WRITE);
            if (booking == null) {
                throw new NotFoundException("Booking not found");
            }
            if (!isAdmin && !booking.getUser().getUsername().equals(requestingUsername)) {
                throw new ForbiddenException("Access denied");
            }
            if (booking.getDate().atTime(booking.getStartTime())
                    .isBefore(java.time.LocalDateTime.now())) {
                throw new BadRequestException("Cannot cancel a booking that has already started");
            }
            booking.setStatus(BookingStatus.CANCELLED);

            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally {
            em.close();
        }
    }
}
