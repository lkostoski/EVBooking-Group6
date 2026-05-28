package com.evbooking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.evbooking.dto.BookingRequest;
import com.evbooking.dto.BookingResponse;
import com.evbooking.dto.BookingUpdateRequest;
import com.evbooking.exception.BadRequestException;
import com.evbooking.exception.ForbiddenException;
import com.evbooking.exception.NotFoundException;
import com.evbooking.model.Booking;
import com.evbooking.model.ChargingStation;
import com.evbooking.model.Connector;
import com.evbooking.model.User;
import com.evbooking.repository.BookingDAO;
import com.evbooking.repository.ChargingStationDAO;
import com.evbooking.repository.ConnectorDAO;
import com.evbooking.repository.UserDAO;

/**
 * Orchestrates booking operations: cross-cutting validation, entity lookups
 * and delegation to the DAO for the atomic transactional part.
 *
 * Resources stay thin (HTTP shaping); the DAO stays focused on persistence
 * with race-safe transactions.
 */
public class BookingService {

    private final BookingDAO bookingDAO;
    private final UserDAO userDAO;
    private final ChargingStationDAO stationDAO;
    private final ConnectorDAO connectorDAO;

    public BookingService() {
        this(new BookingDAO(), new UserDAO(), new ChargingStationDAO(), new ConnectorDAO());
    }

    /** Constructor for testing — accepts mock DAOs. */
    public BookingService(BookingDAO bookingDAO, UserDAO userDAO,
                          ChargingStationDAO stationDAO, ConnectorDAO connectorDAO) {
        this.bookingDAO   = bookingDAO;
        this.userDAO      = userDAO;
        this.stationDAO   = stationDAO;
        this.connectorDAO = connectorDAO;
    }

    public List<BookingResponse> listForUser(String username, boolean isAdmin) {
        List<Booking> bookings = isAdmin ? bookingDAO.findAll() : bookingDAO.findByUsername(username);
        return bookings.stream().map(BookingResponse::from).collect(Collectors.toList());
    }

    public BookingResponse getById(Long id, String requestingUsername, boolean isAdmin) {
        Booking b = bookingDAO.findById(id);
        if (b == null) {
            throw new NotFoundException("Booking not found");
        }
        if (!isAdmin && !b.getUser().getUsername().equals(requestingUsername)) {
            throw new ForbiddenException("Access denied");
        }
        return BookingResponse.from(b);
    }

    public BookingResponse create(BookingRequest req, String username) {
        return create(req, username, false);
    }

    public BookingResponse create(BookingRequest req, String requestingUsername, boolean isAdmin) {
        validateTimes(req.getDate(), req.getStartTime(), req.getEndTime());
        validateNotInPast(req.getDate(), req.getStartTime());

        String bookingUsername = requestingUsername;
        if (isAdmin && req.getDriverUsername() != null && !req.getDriverUsername().isBlank()) {
            bookingUsername = req.getDriverUsername().trim();
        }

        User user = userDAO.findByUsername(bookingUsername);
        if (user == null) {
            throw new BadRequestException(
                isAdmin ? "Driver user not found" : "Authenticated user not found");
        }
        ChargingStation station = stationDAO.findById(req.getStationId());
        if (station == null) {
            throw new NotFoundException("Station not found");
        }
        Connector connector = connectorDAO.findById(req.getConnectorId());
        if (connector == null) {
            throw new NotFoundException("Connector not found");
        }
        if (!connector.getChargingStation().getStationId().equals(station.getStationId())) {
            throw new BadRequestException("Connector does not belong to the given station");
        }

        Booking persisted = bookingDAO.createAtomic(
            user, station, connector,
            req.getDate(), req.getStartTime(), req.getEndTime()
        );
        return BookingResponse.from(persisted);
    }

    public BookingResponse update(Long bookingId, BookingUpdateRequest req,
                                  String requestingUsername, boolean isAdmin) {
        validateTimes(req.getDate(), req.getStartTime(), req.getEndTime());

        Booking persisted = bookingDAO.updateAtomic(
            bookingId,
            req.getDate(), req.getStartTime(), req.getEndTime(),
            requestingUsername, isAdmin
        );
        return BookingResponse.from(persisted);
    }

    public void cancel(Long bookingId, String requestingUsername, boolean isAdmin) {
        bookingDAO.cancel(bookingId, requestingUsername, isAdmin);
    }

    // ── validation helpers ─────────────────────────────────────────────────
    // Package-private so unit tests can drive them directly.

    static void validateTimes(LocalDate date, LocalTime start, LocalTime end) {
        if (date == null || start == null || end == null) {
            throw new BadRequestException("date, startTime and endTime are required");
        }
        if (!start.isBefore(end)) {
            throw new BadRequestException("startTime must be before endTime");
        }
        long minutes = ChronoUnit.MINUTES.between(start, end);
        if (minutes < 20) {
            throw new BadRequestException("Minimum booking duration is 20 minutes");
        }
        if (minutes > 40) {
            throw new BadRequestException("Maximum booking duration is 40 minutes");
        }
    }

    static void validateNotInPast(LocalDate date, LocalTime start) {
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot book a date in the past");
        }
        if (date.equals(LocalDate.now())
                && LocalDateTime.of(date, start).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot book a time in the past");
        }
    }
}
