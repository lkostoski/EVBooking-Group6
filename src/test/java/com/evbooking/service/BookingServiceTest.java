package com.evbooking.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.evbooking.exception.BadRequestException;

/**
 * Pure-logic tests for the booking validation guards.
 *
 * These tests exercise the helpers directly so that the booking
 * business rules can be verified without a database, mock framework,
 * or container.
 */
class BookingServiceTest {

    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    // ── validateTimes ──────────────────────────────────────────────────────

    @Test
    @DisplayName("validateTimes accepts a well-formed window")
    void validateTimesAcceptsValid() {
        assertDoesNotThrow(() -> BookingService.validateTimes(
            TOMORROW, LocalTime.of(8, 0), LocalTime.of(8, 30)));
    }

    @Test
    @DisplayName("validateTimes rejects null date")
    void validateTimesRejectsNullDate() {
        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> BookingService.validateTimes(null, LocalTime.of(8, 0), LocalTime.of(9, 30)));
        assertEquals("date, startTime and endTime are required", ex.getMessage());
    }

    @Test
    @DisplayName("validateTimes rejects null startTime")
    void validateTimesRejectsNullStart() {
        assertThrows(BadRequestException.class,
            () -> BookingService.validateTimes(TOMORROW, null, LocalTime.of(9, 30)));
    }

    @Test
    @DisplayName("validateTimes rejects endTime equal to startTime")
    void validateTimesRejectsEqualTimes() {
        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> BookingService.validateTimes(TOMORROW,
                LocalTime.of(9, 0), LocalTime.of(9, 0)));
        assertEquals("startTime must be before endTime", ex.getMessage());
    }

    @Test
    @DisplayName("validateTimes rejects endTime before startTime")
    void validateTimesRejectsInvertedTimes() {
        assertThrows(BadRequestException.class,
            () -> BookingService.validateTimes(TOMORROW,
                LocalTime.of(10, 0), LocalTime.of(9, 0)));
    }

    // ── validateNotInPast ──────────────────────────────────────────────────

    @Test
    @DisplayName("validateNotInPast accepts tomorrow")
    void validateNotInPastAcceptsTomorrow() {
        assertDoesNotThrow(() -> BookingService.validateNotInPast(TOMORROW, LocalTime.of(0, 0)));
    }

    @Test
    @DisplayName("validateNotInPast rejects yesterday")
    void validateNotInPastRejectsYesterday() {
        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> BookingService.validateNotInPast(
                LocalDate.now().minusDays(1), LocalTime.of(8, 0)));
        assertEquals("Cannot book a date in the past", ex.getMessage());
    }

    @Test
    @DisplayName("validateNotInPast rejects today + a time already past")
    void validateNotInPastRejectsTodayWithPastTime() {
        // Yesterday is unambiguously past; using "now - 1 hour today" would be brittle
        // when run at midnight. Instead, check that the rule fires for any past instant.
        LocalDate today = LocalDate.now();
        if (LocalTime.now().isAfter(LocalTime.of(0, 5))) {
            BadRequestException ex = assertThrows(BadRequestException.class,
                () -> BookingService.validateNotInPast(today, LocalTime.of(0, 0)));
            assertEquals("Cannot book a time in the past", ex.getMessage());
        }
    }
}
