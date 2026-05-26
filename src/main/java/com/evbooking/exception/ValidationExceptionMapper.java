package com.evbooking.exception;

import java.util.stream.Collectors;

import com.evbooking.dto.MessageResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps Bean Validation failures to a clean 400 JSON response. Jersey's default
 * handling for @Valid violations is to short-circuit with an empty 400; this
 * mapper intercepts it first and surfaces the actual validation messages.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining("; "));
        if (message.isEmpty()) message = "Validation failed";
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new MessageResponse(message))
                .build();
    }
}
