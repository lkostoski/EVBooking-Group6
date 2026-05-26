package com.evbooking.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.evbooking.config.JacksonConfig;
import com.evbooking.dto.MessageResponse;
import com.evbooking.exception.ApiExceptionMapper;
import com.evbooking.security.AuthenticatedSecurityContext;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

class JerseyHttpWiringTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig()
            .register(ConfigResource.class)
            .register(ChargingStationResource.class)
            .register(AvailableSlotResource.class)
            .register(ApiExceptionMapper.class)
            .register(JacksonConfig.class)
            .register(RolesAllowedDynamicFeature.class)
            .register(HeaderAuthFilter.class);
    }

    @Test
    @DisplayName("GET /config is public and serializes JSON through Jersey")
    void configEndpointIsPublicJson() {
        Response response = target("config").request().get();

        assertEquals(200, response.getStatus());
        String body = response.readEntity(String.class);
        assertTrue(body.contains("mapsKey"));
        assertTrue(body.contains("clientId"));
    }

    @Test
    @DisplayName("Jersey enforces @RolesAllowed before admin station creation reaches service code")
    void driverCannotCreateStation() {
        Response response = target("stations")
            .request()
            .header("X-Test-User", "nikos")
            .header("X-Test-Role", "DRIVER")
            .post(Entity.entity(
                "{\"name\":\"Demo\",\"address\":\"Demo address\",\"latitude\":40.0,\"longitude\":22.0}",
                MediaType.APPLICATION_JSON
            ));

        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("invalid slot date is mapped to JSON 400 through Jersey exception mapper")
    void invalidSlotDateReturnsJson400() {
        Response response = target("connectors/1/slots")
            .queryParam("date", "26/05/2026")
            .request()
            .header("X-Test-User", "nikos")
            .header("X-Test-Role", "DRIVER")
            .get();

        assertEquals(400, response.getStatus());
        assertEquals("Invalid date format, expected YYYY-MM-DD",
            response.readEntity(MessageResponse.class).getMessage());
    }

    @Provider
    @Priority(Priorities.AUTHENTICATION)
    public static class HeaderAuthFilter implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            String username = requestContext.getHeaderString("X-Test-User");
            String role = requestContext.getHeaderString("X-Test-Role");
            if (username != null && role != null) {
                requestContext.setSecurityContext(
                    new AuthenticatedSecurityContext(username, role, true)
                );
            }
        }
    }
}
