package com.evbooking.resource;

import com.evbooking.dto.ConfigResponse;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource {

    @GET
    @PermitAll
    public ConfigResponse getConfig() {
        return new ConfigResponse(
            envOrEmpty("GOOGLE_MAPS_KEY"),
            envOrEmpty("GOOGLE_CLIENT_ID")
        );
    }

    private String envOrEmpty(String name) {
        String v = System.getenv(name);
        return v == null ? "" : v;
    }
}
