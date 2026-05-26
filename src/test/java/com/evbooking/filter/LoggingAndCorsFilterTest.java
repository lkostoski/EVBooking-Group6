package com.evbooking.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

class LoggingAndCorsFilterTest {

    @Test
    @DisplayName("request logging includes required coursework fields")
    void loggingIncludesRequiredFields() throws Exception {
        Logger logger = Logger.getLogger("evbooking.request");
        CapturingHandler handler = new CapturingHandler();
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        try {
            LoggingFilter filter = new LoggingFilter();
            ContainerRequestContext request = request("POST", "https://example.test/api/bookings", null);
            ContainerResponseContext response = response(201);

            filter.filter(request);
            filter.filter(request, response);

            String line = handler.records.get(0).getMessage();
            assertTrue(line.contains("INSTANCE: "));
            assertTrue(line.contains("METHOD: POST"));
            assertTrue(line.contains("URI: https://example.test/api/bookings"));
            assertTrue(line.contains("STATUS: 201"));
            assertTrue(line.contains("TIME: "));
            assertEquals(Level.INFO, handler.records.get(0).getLevel());
        } finally {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(true);
        }
    }

    @Test
    @DisplayName("CORS credentials never use wildcard origin")
    void corsCredentialsEchoOriginNotWildcard() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        new CorsFilter().filter(
            request("OPTIONS", "https://example.test/api/bookings", "https://client.test"),
            response(204, headers)
        );

        assertEquals("https://client.test", headers.getFirst("Access-Control-Allow-Origin"));
        assertEquals("true", headers.getFirst("Access-Control-Allow-Credentials"));
    }

    private static ContainerRequestContext request(String method, String uri, String origin) {
        AtomicReference<Object> startTime = new AtomicReference<>();
        return (ContainerRequestContext) Proxy.newProxyInstance(
            ContainerRequestContext.class.getClassLoader(),
            new Class<?>[] { ContainerRequestContext.class },
            (proxy, methodRef, args) -> {
                switch (methodRef.getName()) {
                    case "getMethod":
                        return method;
                    case "getUriInfo":
                        return uriInfo(uri);
                    case "setProperty":
                        if ("startTime".equals(args[0])) startTime.set(args[1]);
                        return null;
                    case "getProperty":
                        return "startTime".equals(args[0]) ? startTime.get() : null;
                    case "getHeaderString":
                        return "Origin".equals(args[0]) ? origin : null;
                    default:
                        throw new UnsupportedOperationException(methodRef.getName());
                }
            });
    }

    private static UriInfo uriInfo(String uri) {
        return (UriInfo) Proxy.newProxyInstance(
            UriInfo.class.getClassLoader(),
            new Class<?>[] { UriInfo.class },
            (proxy, methodRef, args) -> {
                if ("getRequestUri".equals(methodRef.getName())) return URI.create(uri);
                throw new UnsupportedOperationException(methodRef.getName());
            });
    }

    private static ContainerResponseContext response(int status) {
        return response(status, new MultivaluedHashMap<>());
    }

    private static ContainerResponseContext response(int status, MultivaluedMap<String, Object> headers) {
        return (ContainerResponseContext) Proxy.newProxyInstance(
            ContainerResponseContext.class.getClassLoader(),
            new Class<?>[] { ContainerResponseContext.class },
            (proxy, methodRef, args) -> {
                switch (methodRef.getName()) {
                    case "getStatus":
                        return status;
                    case "getHeaders":
                        return headers;
                    default:
                        throw new UnsupportedOperationException(methodRef.getName());
                }
            });
    }

    private static final class CapturingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();
        @Override public void publish(LogRecord record) { records.add(record); }
        @Override public void flush() {}
        @Override public void close() {}
    }
}
