package com.evbooking.security;

import java.security.Principal;

import jakarta.ws.rs.core.SecurityContext;

public class AuthenticatedSecurityContext implements SecurityContext {

    private final String username;
    private final String role;
    private final boolean secure;

    public AuthenticatedSecurityContext(String username, String role, boolean secure) {
        this.username = username;
        this.role = role;
        this.secure = secure;
    }

    @Override
    public Principal getUserPrincipal() {
        return () -> username;
    }

    @Override
    public boolean isUserInRole(String requiredRole) {
        return role != null && role.equals(requiredRole);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return "Cookie";
    }

    public String getRole() {
        return role;
    }
}
