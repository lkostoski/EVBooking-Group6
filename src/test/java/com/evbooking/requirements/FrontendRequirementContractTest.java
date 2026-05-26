package com.evbooking.requirements;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FrontendRequirementContractTest {

    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    @DisplayName("frontend authenticates through /api/auth/me rather than readable cookies")
    void frontendUsesServerSideSessionIdentity() throws IOException {
        String auth = read("js/auth.js");
        String index = read("index.html");

        assertTrue(auth.contains("api/auth/me"));
        assertTrue(index.contains("api/auth/me"));
        assertFalse(auth.contains("document.cookie"));
    }

    @Test
    @DisplayName("station browser includes map view and station/connector API calls")
    void stationBrowserHasMapAndApiCalls() throws IOException {
        String stations = read("stations.html");

        assertTrue(stations.contains("id=\"station-map\""));
        assertTrue(stations.contains("google.maps.Map"));
        assertTrue(stations.contains("api/stations"));
        assertTrue(stations.contains("/connectors"));
    }

    @Test
    @DisplayName("driver UI supports create, modify, cancel and view bookings")
    void driverBookingWorkflowsExist() throws IOException {
        String station = read("station.html");
        String bookings = read("bookings.html");

        assertTrue(station.contains("api/bookings"));
        assertTrue(station.contains("api/connectors/${selectedConn.id}/slots"));
        assertTrue(bookings.contains("api/bookings"));
        assertTrue(bookings.contains("method: 'PUT'"));
        assertTrue(bookings.contains("method: 'DELETE'"));
        assertTrue(bookings.contains("modify-btn"));
        assertTrue(bookings.contains("cancel-btn"));
    }

    @Test
    @DisplayName("admin UI exposes station, connector, slot and booking management calls")
    void adminWorkflowsExist() throws IOException {
        String admin = read("admin.html");

        assertTrue(admin.contains("api/stations"));
        assertTrue(admin.contains("/connectors"));
        assertTrue(admin.contains("edit-conn-btn"));
        assertTrue(admin.contains("`api/stations/${selectedConnSid}/connectors/${id}`"));
        assertTrue(admin.contains("/slots"));
        assertTrue(admin.contains("api/bookings"));
        assertTrue(admin.contains("id=\"add-booking-btn\""));
        assertTrue(admin.contains("admin-edit-booking-btn"));
        assertTrue(admin.contains("driverUsername"));
        assertTrue(admin.contains("method: 'POST'"));
        assertTrue(admin.contains("method: 'PUT'"));
        assertTrue(admin.contains("method: 'DELETE'"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(WEBAPP.resolve(relative));
    }
}
