package com.evbooking.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

import com.evbooking.dto.AuthResponse;
import com.evbooking.dto.ChangePasswordRequest;
import com.evbooking.dto.LoginRequest;
import com.evbooking.dto.RegisterRequest;
import com.evbooking.exception.BadRequestException;
import com.evbooking.exception.UnauthorizedException;
import com.evbooking.model.Role;
import com.evbooking.model.User;
import com.evbooking.repository.UserDAO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthService {

    /**
     * bcrypt work factor. 12 rounds is the OWASP-recommended minimum as of
     * 2024; each +1 doubles the hash time. Local benchmarks: ~250 ms per
     * hash on commodity hardware — acceptable for login latency, infeasible
     * for offline brute force.
     */
    private static final int BCRYPT_ROUNDS = 12;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public AuthResponse login(LoginRequest req) {
        User user = userDAO.findByUsername(req.getUsername());
        if (user == null || !BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return new AuthResponse(user.getUsername(), user.getRole().name());
    }

    public User findUser(String username) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            throw new UnauthorizedException("Authenticated user not found");
        }
        return user;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userDAO.existsByUsername(req.getUsername())) {
            throw new BadRequestException("Username already taken");
        }
        String hashed = BCrypt.hashpw(req.getPassword(), BCrypt.gensalt(BCRYPT_ROUNDS));
        User user = new User(req.getUsername(), hashed, Role.DRIVER);
        userDAO.save(user);
        return new AuthResponse(user.getUsername(), user.getRole().name());
    }

    public void changePassword(String username, ChangePasswordRequest req) {
        User user = userDAO.findByUsername(username);
        if (user == null || !BCrypt.checkpw(req.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPassword(BCrypt.hashpw(req.getNewPassword(), BCrypt.gensalt(BCRYPT_ROUNDS)));
        userDAO.update(user);
    }

    public AuthResponse loginWithGoogleIdToken(String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            throw new BadRequestException("Missing idToken");
        }
        try {
            HttpClient http = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new UnauthorizedException("Invalid Google token");
            }
            Map<String, String> tokenData = JSON.readValue(
                    resp.body(), new TypeReference<Map<String, String>>() {});
            String googleSub = tokenData.get("sub");
            String email     = tokenData.get("email");
            if (googleSub == null || email == null) {
                throw new UnauthorizedException("Invalid Google token");
            }

            User user = userDAO.findByGoogleId(googleSub);
            if (user == null) {
                String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
                if (base.length() > 45) base = base.substring(0, 45);
                String username = base;
                int suffix = 1;
                while (userDAO.existsByUsername(username)) {
                    username = base + suffix++;
                }
                user = new User(username,
                        BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt(BCRYPT_ROUNDS)),
                        Role.DRIVER);
                user.setGoogleId(googleSub);
                userDAO.save(user);
            }
            return new AuthResponse(user.getUsername(), user.getRole().name());
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Google authentication failed", e);
        }
    }
}
