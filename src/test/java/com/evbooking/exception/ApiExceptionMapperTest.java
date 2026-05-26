package com.evbooking.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.evbooking.dto.MessageResponse;

import jakarta.ws.rs.core.Response;

class ApiExceptionMapperTest {

    private final ApiExceptionMapper mapper = new ApiExceptionMapper();

    @Test
    @DisplayName("BookingConflictException -> 409 with message body")
    void conflictMapsTo409() {
        Response r = mapper.toResponse(new BookingConflictException("already booked"));
        assertEquals(409, r.getStatus());
        assertEquals("already booked", ((MessageResponse) r.getEntity()).getMessage());
    }

    @Test
    @DisplayName("NotFoundException -> 404")
    void notFoundMapsTo404() {
        Response r = mapper.toResponse(new NotFoundException("missing"));
        assertEquals(404, r.getStatus());
    }

    @Test
    @DisplayName("ForbiddenException -> 403")
    void forbiddenMapsTo403() {
        Response r = mapper.toResponse(new ForbiddenException("nope"));
        assertEquals(403, r.getStatus());
    }

    @Test
    @DisplayName("UnauthorizedException -> 401")
    void unauthorizedMapsTo401() {
        Response r = mapper.toResponse(new UnauthorizedException("login first"));
        assertEquals(401, r.getStatus());
    }

    @Test
    @DisplayName("BadRequestException -> 400")
    void badRequestMapsTo400() {
        Response r = mapper.toResponse(new BadRequestException("invalid"));
        assertEquals(400, r.getStatus());
    }

    @Test
    @DisplayName("Unknown exception -> 500 with generic message (does not leak)")
    void unknownMapsTo500() {
        Response r = mapper.toResponse(new RuntimeException("connection refused on internal db"));
        assertEquals(500, r.getStatus());
        // Internal details must not be exposed:
        assertEquals("An unexpected error occurred",
            ((MessageResponse) r.getEntity()).getMessage());
    }
}
