package com.evbooking.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthenticatedSecurityContextTest {

    @Test
    @DisplayName("security context exposes server-resolved user and role")
    void exposesAuthenticatedPrincipalAndRole() {
        AuthenticatedSecurityContext context =
            new AuthenticatedSecurityContext("alice", "ADMIN", true);

        assertEquals("alice", context.getUserPrincipal().getName());
        assertTrue(context.isUserInRole("ADMIN"));
        assertFalse(context.isUserInRole("DRIVER"));
        assertTrue(context.isSecure());
        assertEquals("Cookie", context.getAuthenticationScheme());
    }
}
