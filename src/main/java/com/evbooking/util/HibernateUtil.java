package com.evbooking.util;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class HibernateUtil {

    private static final EntityManagerFactory emf = buildEntityManagerFactory();

    private static EntityManagerFactory buildEntityManagerFactory() {
        try {
            return Persistence.createEntityManagerFactory("evbooking-pu", resolveProperties());
        } catch (Exception e) {
            System.err.println("EntityManagerFactory creation failed: " + e.getMessage());
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Resolves JDBC connection settings from the environment.
     * Precedence:
     *   1. DATABASE_URL (Heroku style, e.g. postgres://user:pass@host:port/db) - parsed
     *   2. JDBC_URL / DB_USER / DB_PASSWORD - direct override
     *   3. Local development defaults (localhost, no password)
     * No credentials are stored in the build artefact.
     */
    private static Map<String, String> resolveProperties() {
        Map<String, String> props = new HashMap<>();

        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            return parseHerokuDatabaseUrl(databaseUrl);
        }

        String jdbcUrl  = envOrDefault("JDBC_URL",
                                       "jdbc:postgresql://localhost:5432/ev_booking");
        String dbUser   = envOrDefault("DB_USER",     System.getProperty("user.name"));
        String dbPass   = envOrDefault("DB_PASSWORD", "");

        props.put("jakarta.persistence.jdbc.url",      jdbcUrl);
        props.put("jakarta.persistence.jdbc.user",     dbUser);
        props.put("jakarta.persistence.jdbc.password", dbPass);
        return props;
    }

    private static Map<String, String> parseHerokuDatabaseUrl(String databaseUrl) {
        try {
            URI uri = new URI(databaseUrl);
            String[] userInfo = uri.getUserInfo().split(":");
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                    + ":" + uri.getPort()
                    + uri.getPath()
                    + "?sslmode=require";
            Map<String, String> props = new HashMap<>();
            props.put("jakarta.persistence.jdbc.url",      jdbcUrl);
            props.put("jakarta.persistence.jdbc.user",     userInfo[0]);
            props.put("jakarta.persistence.jdbc.password", userInfo[1]);
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse DATABASE_URL", e);
        }
    }

    private static String envOrDefault(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    public static void shutdown() {
        if (emf != null && emf.isOpen()) emf.close();
    }
}
