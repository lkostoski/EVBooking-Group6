package com.evbooking.resource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

class ResourceSecurityAnnotationTest {

    @Test
    @DisplayName("auth endpoints expose only expected public operations")
    void authEndpointSecurity() throws Exception {
        assertPermitAll(AuthResource.class, "login", com.evbooking.dto.LoginRequest.class);
        assertPermitAll(AuthResource.class, "register", com.evbooking.dto.RegisterRequest.class);
        assertPermitAll(AuthResource.class, "googleAuth", java.util.Map.class);
        assertPermitAll(AuthResource.class, "logout", String.class);
        assertNoPermitAll(AuthResource.class, "me", jakarta.ws.rs.core.SecurityContext.class);
        assertNoPermitAll(AuthResource.class, "changePassword",
            com.evbooking.dto.ChangePasswordRequest.class,
            jakarta.ws.rs.core.SecurityContext.class);
    }

    @Test
    @DisplayName("station, connector and slot management is admin-only while browse is driver/admin")
    void catalogResourceSecurity() throws Exception {
        assertRoles(ChargingStationResource.class, "list", new String[] {"DRIVER", "ADMIN"});
        assertRoles(ChargingStationResource.class, "getById", new String[] {"DRIVER", "ADMIN"}, Long.class);
        assertRoles(ChargingStationResource.class, "create", new String[] {"ADMIN"}, com.evbooking.dto.StationDTO.class);
        assertRoles(ChargingStationResource.class, "update", new String[] {"ADMIN"}, Long.class, com.evbooking.dto.StationDTO.class);
        assertRoles(ChargingStationResource.class, "delete", new String[] {"ADMIN"}, Long.class);

        assertRoles(ConnectorResource.class, "list", new String[] {"DRIVER", "ADMIN"}, Long.class);
        assertRoles(ConnectorResource.class, "create", new String[] {"ADMIN"}, Long.class, com.evbooking.dto.ConnectorRequest.class);
        assertRoles(ConnectorResource.class, "update", new String[] {"ADMIN"}, Long.class, Long.class, com.evbooking.dto.ConnectorRequest.class);
        assertRoles(ConnectorResource.class, "delete", new String[] {"ADMIN"}, Long.class, Long.class);

        assertRoles(AvailableSlotResource.class, "list", new String[] {"DRIVER", "ADMIN"}, Long.class, String.class);
        assertRoles(AvailableSlotResource.class, "create", new String[] {"ADMIN"}, Long.class, com.evbooking.dto.SlotRequest.class);
        assertRoles(AvailableSlotResource.class, "delete", new String[] {"ADMIN"}, Long.class, Long.class);
    }

    @Test
    @DisplayName("booking resource requires authenticated driver/admin users")
    void bookingResourceSecurity() {
        RolesAllowed roles = BookingResource.class.getAnnotation(RolesAllowed.class);
        assertNotNull(roles);
        assertArrayEquals(new String[] {"DRIVER", "ADMIN"}, roles.value());
    }

    private static void assertRoles(Class<?> type, String methodName, String[] roles, Class<?>... params)
            throws Exception {
        Method method = type.getMethod(methodName, params);
        RolesAllowed annotation = method.getAnnotation(RolesAllowed.class);
        assertNotNull(annotation, type.getSimpleName() + "." + methodName + " must declare @RolesAllowed");
        assertArrayEquals(roles, annotation.value());
    }

    private static void assertPermitAll(Class<?> type, String methodName, Class<?>... params) throws Exception {
        Method method = type.getMethod(methodName, params);
        assertNotNull(method.getAnnotation(PermitAll.class));
    }

    private static void assertNoPermitAll(Class<?> type, String methodName, Class<?>... params) throws Exception {
        Method method = type.getMethod(methodName, params);
        org.junit.jupiter.api.Assertions.assertNull(method.getAnnotation(PermitAll.class));
    }
}
