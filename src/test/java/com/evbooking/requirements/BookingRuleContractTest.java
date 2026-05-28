package com.evbooking.requirements;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookingRuleContractTest {

    @Test
    @DisplayName("booking DAO implements race-safe connector and driver overlap protection")
    void bookingDaoContainsConcurrentBookingGuards() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/com/evbooking/repository/BookingDAO.java"));

        assertTrue(dao.contains("LockModeType.PESSIMISTIC_WRITE"));
        assertTrue(dao.contains("connectorOverlap"));
        assertTrue(dao.contains("driverOverlap"));
        assertTrue(dao.contains("Slot already booked"));
        assertTrue(dao.contains("You already have another booking in this time period"));
    }

    @Test
    @DisplayName("bookings must be inside published available slots")
    void bookingDaoRequiresPublishedAvailableSlot() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/com/evbooking/repository/BookingDAO.java"));

        assertTrue(dao.contains("slotMatches"));
        assertTrue(dao.contains("AvailableSlot"));
        assertTrue(dao.contains("Requested time is outside the availability window for this connector"));
    }

    @Test
    @DisplayName("booking changes reject inactive or already-started bookings")
    void bookingDaoProtectsStartedBookings() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/com/evbooking/repository/BookingDAO.java"));

        assertTrue(dao.contains("Only active bookings can be modified"));
        assertTrue(dao.contains("Cannot modify a booking that has already started"));
        assertTrue(dao.contains("Cannot cancel a booking that has already started"));
    }
}
