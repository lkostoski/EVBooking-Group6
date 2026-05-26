package com.evbooking.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.evbooking.exception.BadRequestException;

class AvailableSlotResourceTest {

    @Test
    @DisplayName("slot date query parameter must be ISO yyyy-MM-dd")
    void invalidSlotDateIsRejectedBeforeServiceCall() {
        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> new AvailableSlotResource().list(1L, "26/05/2026"));

        assertEquals("Invalid date format, expected YYYY-MM-DD", ex.getMessage());
    }
}
