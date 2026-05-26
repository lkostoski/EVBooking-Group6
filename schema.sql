-- ============================================================
-- EV Booking — Schema (run before seed.sql)
-- Run with: psql ev_booking < schema.sql
--
-- Hibernate runs in hbm2ddl.auto=validate, so the application
-- requires these tables to exist with this exact shape.
-- ============================================================

DROP TABLE IF EXISTS bookings         CASCADE;
DROP TABLE IF EXISTS available_slots  CASCADE;
DROP TABLE IF EXISTS connectors       CASCADE;
DROP TABLE IF EXISTS charging_stations CASCADE;
DROP TABLE IF EXISTS user_sessions    CASCADE;
DROP TABLE IF EXISTS users            CASCADE;

CREATE TABLE users (
    username   VARCHAR(50)  PRIMARY KEY,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(10)  NOT NULL CHECK (role IN ('DRIVER', 'ADMIN')),
    google_id  VARCHAR(100) UNIQUE
);

CREATE TABLE charging_stations (
    station_id BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    address    VARCHAR(255) NOT NULL,
    latitude   DOUBLE PRECISION NOT NULL,
    longitude  DOUBLE PRECISION NOT NULL
);

CREATE TABLE connectors (
    connector_id   BIGSERIAL   PRIMARY KEY,
    connector_type VARCHAR(50) NOT NULL,
    station_id     BIGINT      NOT NULL
        REFERENCES charging_stations(station_id) ON DELETE CASCADE
);

CREATE TABLE user_sessions (
    session_id VARCHAR(128) PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL
        REFERENCES users(username) ON DELETE CASCADE,
    expires_at TIMESTAMP    NOT NULL
);

CREATE TABLE available_slots (
    slot_id      BIGSERIAL PRIMARY KEY,
    connector_id BIGINT    NOT NULL
        REFERENCES connectors(connector_id) ON DELETE CASCADE,
    date         DATE      NOT NULL,
    start_time   TIME      NOT NULL,
    end_time     TIME      NOT NULL,
    CHECK (start_time < end_time)
);

CREATE TABLE bookings (
    booking_id      BIGSERIAL PRIMARY KEY,
    driver_username VARCHAR(50) NOT NULL
        REFERENCES users(username) ON DELETE CASCADE,
    station_id      BIGINT      NOT NULL
        REFERENCES charging_stations(station_id) ON DELETE CASCADE,
    connector_id    BIGINT      NOT NULL
        REFERENCES connectors(connector_id) ON DELETE CASCADE,
    date            DATE        NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CHECK (start_time < end_time)
);

-- Indexes that the overlap-detection queries depend on for performance
CREATE INDEX idx_bookings_connector_date_status
    ON bookings(connector_id, date, status);

CREATE INDEX idx_bookings_user_date_status
    ON bookings(driver_username, date, status);

CREATE INDEX idx_slots_connector_date
    ON available_slots(connector_id, date);

CREATE INDEX idx_user_sessions_expires_at
    ON user_sessions(expires_at);
