package com.evbooking.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.evbooking.model.Role;
import com.evbooking.model.User;
import com.evbooking.model.UserSession;
import com.evbooking.repository.UserSessionDAO;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

class AuthFilterTest {

    @Test
    @DisplayName("forged username/role cookies do not authenticate a request")
    void forgedPlainCookiesAreRejected() throws Exception {
        RecordingRequest request = new RecordingRequest(
            "GET",
            "bookings",
            Map.of(
                "username", new Cookie("username", "nikos"),
                "role", new Cookie("role", "ADMIN")
            )
        );

        new AuthFilter(new FakeSessionDAO(null)).filter(request.context());

        assertNotNull(request.aborted.get());
        assertEquals(401, request.aborted.get().getStatus());
        assertNull(request.securityContext.get());
    }

    @Test
    @DisplayName("valid session uses server-side role even when role cookie is forged")
    void validSessionDerivesRoleFromServerSession() throws Exception {
        User admin = new User("admin", "hash", Role.ADMIN);
        UserSession session = new UserSession("server-token", admin, java.time.LocalDateTime.now().plusHours(1));
        RecordingRequest request = new RecordingRequest(
            "GET",
            "bookings",
            Map.of(
                "sessionId", new Cookie("sessionId", "server-token"),
                "role", new Cookie("role", "DRIVER")
            )
        );

        new AuthFilter(new FakeSessionDAO(session)).filter(request.context());

        assertNull(request.aborted.get());
        SecurityContext sec = request.securityContext.get();
        assertNotNull(sec);
        assertEquals("admin", sec.getUserPrincipal().getName());
        assertTrue(sec.isUserInRole("ADMIN"));
    }

    private static final class FakeSessionDAO extends UserSessionDAO {
        private final UserSession session;

        FakeSessionDAO(UserSession session) {
            this.session = session;
        }

        @Override
        public UserSession findValid(String sessionId) {
            return session != null && session.getSessionId().equals(sessionId) ? session : null;
        }
    }

    private static final class RecordingRequest {
        private final String method;
        private final String path;
        private final Map<String, Cookie> cookies;
        private final AtomicReference<Response> aborted = new AtomicReference<>();
        private final AtomicReference<SecurityContext> securityContext = new AtomicReference<>();

        RecordingRequest(String method, String path, Map<String, Cookie> cookies) {
            this.method = method;
            this.path = path;
            this.cookies = cookies;
        }

        ContainerRequestContext context() {
            return (ContainerRequestContext) Proxy.newProxyInstance(
                ContainerRequestContext.class.getClassLoader(),
                new Class<?>[] { ContainerRequestContext.class },
                (proxy, methodRef, args) -> {
                    switch (methodRef.getName()) {
                        case "getMethod":
                            return method;
                        case "getCookies":
                            return cookies;
                        case "getUriInfo":
                            return uriInfo();
                        case "abortWith":
                            aborted.set((Response) args[0]);
                            return null;
                        case "setSecurityContext":
                            securityContext.set((SecurityContext) args[0]);
                            return null;
                        default:
                            throw new UnsupportedOperationException(methodRef.getName());
                    }
                });
        }

        private UriInfo uriInfo() {
            return (UriInfo) Proxy.newProxyInstance(
                UriInfo.class.getClassLoader(),
                new Class<?>[] { UriInfo.class },
                (proxy, methodRef, args) -> {
                    switch (methodRef.getName()) {
                        case "getPath":
                            return path;
                        case "getRequestUri":
                            return URI.create("https://example.test/api/" + path);
                        default:
                            throw new UnsupportedOperationException(methodRef.getName());
                    }
                });
        }
    }
}
