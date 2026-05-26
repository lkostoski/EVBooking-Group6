package com.evbooking.filter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Emits a single structured log line per HTTP request including the six
 * fields required by the coursework brief:
 *   timestamp, instance, HTTP method, URI, status, processing time (ms).
 *
 * The instance identifier comes from the DYNO env var on Heroku
 * (a unique value per dyno) and falls back to "local-instance" off-platform.
 * The single-line format is friendly to Heroku Logplex / generic log aggregators.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger("evbooking.request");
    private static final String START_TIME_PROPERTY = "startTime";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String INSTANCE_ID =
        System.getenv("DYNO") != null ? System.getenv("DYNO") : "local-instance";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);
        long processingMs = startTime != null ? System.currentTimeMillis() - startTime : 0;

        String line = String.format(
            "[%s] INSTANCE: %s | METHOD: %s | URI: %s | STATUS: %d | TIME: %dms",
            LocalDateTime.now().format(TS),
            INSTANCE_ID,
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri(),
            responseContext.getStatus(),
            processingMs
        );

        Level level = responseContext.getStatus() >= 500 ? Level.SEVERE
                    : responseContext.getStatus() >= 400 ? Level.WARNING
                    : Level.INFO;
        LOG.log(level, line);
    }
}
