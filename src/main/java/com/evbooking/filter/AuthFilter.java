package com.evbooking.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.evbooking.model.User;
import com.evbooking.model.UserSession;
import com.evbooking.repository.UserSessionDAO;
import com.evbooking.security.AuthenticatedSecurityContext;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    private final UserSessionDAO sessionDAO;

    public AuthFilter() {
        this(new UserSessionDAO());
    }

    AuthFilter(UserSessionDAO sessionDAO) {
        this.sessionDAO = sessionDAO;
    }

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "auth/login",
        "auth/register",
        "auth/google",
        "config"
    );

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        String path = requestContext.getUriInfo().getPath();

        // Exact-segment match: avoid substring false positives like /users/myconfig
        // accidentally bypassing auth because "config" is a substring.
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath) || path.startsWith(publicPath + "/")) return;
        }

        if (requestContext.getMethod().equals("OPTIONS")) return;

        Map<String, Cookie> cookies = requestContext.getCookies();
        Cookie sessionCookie = cookies.get("sessionId");

        UserSession session =
            sessionCookie == null ? null : sessionDAO.findValid(sessionCookie.getValue());

        if (session == null) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                    .entity(Map.of("message", "Please log in"))
                    .build()
            );
            return;
        }

        User user = session.getUser();
        boolean secure = requestContext.getUriInfo().getRequestUri().getScheme().equals("https");
        requestContext.setSecurityContext(
            new AuthenticatedSecurityContext(user.getUsername(), user.getRole().name(), secure)
        );
    }
}
