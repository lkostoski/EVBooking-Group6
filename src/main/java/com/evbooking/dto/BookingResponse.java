package com.evbooking.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.evbooking.model.Booking;

/**
 * Flat view of a booking exposed through the API. Keeps the persistence model
 * decoupled from the API contract and avoids accidental leakage of entity
 * internals (lazy proxies, bidirectional graphs, password hashes, etc.).
 */
public class BookingResponse {

    private Long bookingId;
    private String username;
    private Long stationId;
    private String stationName;
    private Long connectorId;
    private String connectorType;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;

    public BookingResponse() {}

    public static BookingResponse from(Booking b) {
        BookingResponse r = new BookingResponse();
        r.bookingId     = b.getBookingId();
        r.username      = b.getUser().getUsername();
        r.stationId     = b.getChargingStation().getStationId();
        r.stationName   = b.getChargingStation().getName();
        r.connectorId   = b.getConnector().getConnectorId();
        r.connectorType = b.getConnector().getConnectorType();
        r.date          = b.getDate();
        r.startTime     = b.getStartTime();
        r.endTime       = b.getEndTime();
        r.status        = b.getStatus().name();
        return r;
    }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public Long getConnectorId() { return connectorId; }
    public void setConnectorId(Long connectorId) { this.connectorId = connectorId; }

    public String getConnectorType() { return connectorType; }
    public void setConnectorType(String connectorType) { this.connectorType = connectorType; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
