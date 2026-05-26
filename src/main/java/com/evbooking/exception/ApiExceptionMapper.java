package com.evbooking.exception;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.evbooking.dto.MessageResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Centralised exception → HTTP response mapping.
 *
 * Each application exception type maps to a specific HTTP status code; Bean
 * Validation failures become 400 with the field message; anything else is
 * logged at SEVERE and returned as a generic 500 so we don't leak internals.
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {

        if (ex instanceof BookingConflictException) {
            return error(Response.Status.CONFLICT, ex.getMessage());
        }
        if (ex instanceof NotFoundException) {
            return error(Response.Status.NOT_FOUND, ex.getMessage());
        }
        if (ex instanceof ForbiddenException) {
            return error(Response.Status.FORBIDDEN, ex.getMessage());
        }
        if (ex instanceof UnauthorizedException) {
            return error(Response.Status.UNAUTHORIZED, ex.getMessage());
        }
        if (ex instanceof BadRequestException) {
            return error(Response.Status.BAD_REQUEST, ex.getMessage());
        }
        if (ex instanceof ConstraintViolationException) {
            return error(Response.Status.BAD_REQUEST, formatViolations((ConstraintViolationException) ex));
        }
        if (ex instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) ex;
            Response.StatusType st = wae.getResponse().getStatusInfo();
            String msg = wae.getMessage() != null ? wae.getMessage() : st.getReasonPhrase();
            return error(Response.Status.fromStatusCode(st.getStatusCode()), msg);
        }

        // Anything else is unexpected — log full stack trace so the operator can find it,
        // but only return a generic message to the client.
        LOG.log(Level.SEVERE, "Unhandled exception", ex);
        return error(Response.Status.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new MessageResponse(message == null ? status.getReasonPhrase() : message))
                .build();
    }

    private String formatViolations(ConstraintViolationException ex) {
        List<String> messages = ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toList());
        return String.join("; ", messages);
    }
}
