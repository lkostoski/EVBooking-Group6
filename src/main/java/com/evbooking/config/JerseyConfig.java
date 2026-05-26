package com.evbooking.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import jakarta.ws.rs.ApplicationPath;

@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        packages("com.evbooking.resource");
        packages("com.evbooking.filter");
        packages("com.evbooking.config");
        packages("com.evbooking.exception");

        // Honour @RolesAllowed / @PermitAll / @DenyAll on resource methods
        register(RolesAllowedDynamicFeature.class);

        // Route 4xx/5xx through Jersey's response pipeline (and our ExceptionMapper)
        // instead of letting the servlet container substitute its default HTML error page.
        property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);

        // Surface Bean Validation messages to clients (default is to hide details).
        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
    }
}
