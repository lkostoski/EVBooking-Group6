package com.evbooking.resource;

import java.util.Map;

import com.evbooking.dto.AuthResponse;
import com.evbooking.dto.ChangePasswordRequest;
import com.evbooking.dto.LoginRequest;
import com.evbooking.dto.MessageResponse;
import com.evbooking.dto.RegisterRequest;
import com.evbooking.exception.UnauthorizedException;
import com.evbooking.model.UserSession;
import com.evbooking.repository.UserSessionDAO;
import com.evbooking.service.AuthService;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final int SESSION_TTL_SECONDS = 7 * 24 * 60 * 60;

    private final AuthService service = new AuthService();
    private final UserSessionDAO sessionDAO = new UserSessionDAO();

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest request) {
        AuthResponse auth = service.login(request);
        return Response.ok(auth)
                .cookie(buildSessionCookies(auth))
                .build();
    }

    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid RegisterRequest request) {
        AuthResponse auth = service.register(request);
        return Response.status(Response.Status.CREATED)
                .entity(new MessageResponse("User registered successfully"))
                .cookie(buildSessionCookies(auth))
                .build();
    }

    @GET
    @Path("/me")
    public AuthResponse me(@Context SecurityContext sec) {
        if (sec.getUserPrincipal() == null) {
            throw new UnauthorizedException("Not logged in");
        }
        return new AuthResponse(
            sec.getUserPrincipal().getName(),
            sec.isUserInRole("ADMIN") ? "ADMIN" : "DRIVER"
        );
    }

    @PUT
    @Path("/password")
    public MessageResponse changePassword(@Valid ChangePasswordRequest request,
                                           @Context SecurityContext sec) {
        if (sec.getUserPrincipal() == null) {
            throw new UnauthorizedException("Not logged in");
        }
        service.changePassword(sec.getUserPrincipal().getName(), request);
        return new MessageResponse("Password updated successfully");
    }

    @POST
    @Path("/google")
    @PermitAll
    public Response googleAuth(Map<String, String> body) {
        String idToken = body == null ? null : body.get("idToken");
        AuthResponse auth = service.loginWithGoogleIdToken(idToken);
        return Response.ok(auth)
                .cookie(buildSessionCookies(auth))
                .build();
    }

    @POST
    @Path("/logout")
    @PermitAll
    public Response logout(@CookieParam("sessionId") String sessionId) {
        sessionDAO.delete(sessionId);
        return Response.ok(new MessageResponse("Logged out"))
                .cookie(
                    clearCookie("sessionId"),
                    clearCookie("session"),
                    clearCookie("username"),
                    clearCookie("role")
                )
                .build();
    }

    private NewCookie[] buildSessionCookies(AuthResponse auth) {
        boolean secure = "true".equalsIgnoreCase(System.getenv("COOKIE_SECURE"));
        UserSession session = sessionDAO.create(
            service.findUser(auth.getUsername()),
            SESSION_TTL_SECONDS
        );

        NewCookie sessionCookie = new NewCookie.Builder("sessionId")
                .value(session.getSessionId())
                .path("/")
                .maxAge(SESSION_TTL_SECONDS)
                .httpOnly(true)
                .secure(secure)
                .sameSite(NewCookie.SameSite.LAX)
                .build();

        return new NewCookie[] {
            sessionCookie,
            clearCookie("session"),
            clearCookie("username"),
            clearCookie("role")
        };
    }

    private NewCookie clearCookie(String name) {
        return new NewCookie.Builder(name)
                .value("")
                .path("/")
                .maxAge(0)
                .build();
    }
}
