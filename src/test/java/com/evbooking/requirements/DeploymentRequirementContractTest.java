package com.evbooking.requirements;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeploymentRequirementContractTest {

    @Test
    @DisplayName("PaaS launch command is committed")
    void procfileExistsForPaasDeployment() throws IOException {
        String procfile = Files.readString(Path.of("Procfile"));

        assertTrue(procfile.contains("web:"));
        assertTrue(procfile.contains("webapp-runner.jar"));
        assertTrue(procfile.contains("--port $PORT"));
    }

    @Test
    @DisplayName("database configuration is externalised for cloud-hosted PostgreSQL")
    void databaseConfigurationIsExternalised() throws IOException {
        String hibernate = Files.readString(Path.of("src/main/java/com/evbooking/util/HibernateUtil.java"));
        String persistence = Files.readString(Path.of("src/main/resources/META-INF/persistence.xml"));

        assertTrue(hibernate.contains("DATABASE_URL"));
        assertTrue(hibernate.contains("JDBC_URL"));
        assertTrue(hibernate.contains("DB_USER"));
        assertTrue(hibernate.contains("sslmode=require"));
        assertTrue(persistence.contains("hibernate.hbm2ddl.auto"));
        assertTrue(persistence.contains("validate"));
    }

    @Test
    @DisplayName("schema matches required concepts including server-side sessions and booking indexes")
    void schemaContainsRequiredTablesAndIndexes() throws IOException {
        String schema = Files.readString(Path.of("schema.sql"));

        assertTrue(schema.contains("CREATE TABLE users"));
        assertTrue(schema.contains("CREATE TABLE charging_stations"));
        assertTrue(schema.contains("CREATE TABLE connectors"));
        assertTrue(schema.contains("CREATE TABLE available_slots"));
        assertTrue(schema.contains("CREATE TABLE bookings"));
        assertTrue(schema.contains("CREATE TABLE user_sessions"));
        assertTrue(schema.contains("idx_bookings_connector_date_status"));
        assertTrue(schema.contains("idx_bookings_user_date_status"));
    }

    @Test
    @DisplayName("multi-instance deployment uses shared DB sessions and bounded connection pooling")
    void multiInstanceAssumptionsAreConfigured() throws IOException {
        String persistence = Files.readString(Path.of("src/main/resources/META-INF/persistence.xml"));
        String authResource = Files.readString(Path.of("src/main/java/com/evbooking/resource/AuthResource.java"));
        String authFilter = Files.readString(Path.of("src/main/java/com/evbooking/filter/AuthFilter.java"));

        assertTrue(persistence.contains("hibernate.hikari.maximumPoolSize"));
        assertTrue(authResource.contains("UserSessionDAO"));
        assertTrue(authFilter.contains("UserSessionDAO"));
    }
}
