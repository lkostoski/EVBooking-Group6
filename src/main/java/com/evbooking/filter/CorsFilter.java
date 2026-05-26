package com.evbooking.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * CORS response filter. When credentials are sent (cookies), the spec forbids
 * "Access-Control-Allow-Origin: *". We therefore echo the request Origin
 * header so the browser will accept the cookies on the response. A
 * comma-separated CORS_ALLOWED_ORIGINS env var may restrict which origins are
 * allowed in production; if unset the request origin is reflected (suitable
 * for development).
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    private static final String ALLOWED = System.getenv("CORS_ALLOWED_ORIGINS");

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {

        String origin = requestContext.getHeaderString("Origin");
        String allowed = resolveAllowedOrigin(origin);

        if (allowed != null) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", allowed);
            responseContext.getHeaders().add("Vary", "Origin");
            responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        }
        responseContext.getHeaders().add(
            "Access-Control-Allow-Headers",
            "origin, content-type, accept, authorization");
        responseContext.getHeaders().add(
            "Access-Control-Allow-Methods",
            "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }

    private String resolveAllowedOrigin(String origin) {
        if (origin == null || origin.isEmpty()) return null;
        if (ALLOWED == null || ALLOWED.isBlank()) {
            // Dev mode: echo whichever origin is asking. Safe because the AuthFilter
            // still requires a valid session cookie.
            return origin;
        }
        for (String candidate : ALLOWED.split(",")) {
            if (candidate.trim().equals(origin)) return origin;
        }
        return null;
    }
}