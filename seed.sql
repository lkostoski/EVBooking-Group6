-- ============================================================
-- EV Booking — Sample data for Thessaloniki, Greece
-- Run after schema.sql:  psql ev_booking < seed.sql
-- Test accounts:
--   admin  / admin123   (ADMIN)
--   kostas / driver123  (DRIVER)
--   elena  / driver123  (DRIVER)
-- ============================================================

-- Users
INSERT INTO users (username, password, role) VALUES
  ('admin',  '$2a$10$tucnTtPT5M.yq8QhnOEmse.ENqqgbEID8/r6OmhzWrCu/DIiMGgza', 'ADMIN'),
  ('kostas', '$2a$10$I03g8rFty6teryoWZP1TfeqcW/NFVgTAEmtyfQyMdFX8v7zPxYTbi', 'DRIVER'),
  ('elena',  '$2a$10$I03g8rFty6teryoWZP1TfeqcW/NFVgTAEmtyfQyMdFX8v7zPxYTbi', 'DRIVER')
ON CONFLICT (username) DO NOTHING;

-- Charging stations — 8 locations across Thessaloniki
INSERT INTO charging_stations (name, address, latitude, longitude) VALUES
  ('Aristotelous Square EV Hub',    'Plateia Aristotelous, Thessaloniki 54624',            40.6337,  22.9390),
  ('Nea Paralia Fast Charge',       'Leoforos Nikis 12, Thessaloniki 54622',               40.6262,  22.9526),
  ('Makedonia Airport Charger',     'Thessaloniki International Airport, Thessaloniki 57001', 40.5196, 22.9709),
  ('Makedonia Palace EV Point',     'Leoforos Megalou Alexandrou 2, Thessaloniki 54640',   40.6225,  22.9481),
  ('Kalamaria Charge Station',      'Leoforos Stratou 45, Kalamaria 55133',                40.5835,  22.9749),
  ('Panorama EV Hub',               'Panorama, Thessaloniki 55236',                        40.5891,  22.9906),
  ('Thermi Fast Charger',           'Thermi, Thessaloniki 57001',                          40.5636,  23.0119),
  ('Tsimiski Street Charger',       'Tsimiski 26, Thessaloniki 54624',                     40.6344,  22.9457);

-- Connectors (2–3 per station)
-- Station 1 (Aristotelous): connector_id 1, 2, 3
-- Station 2 (Nea Paralia):  connector_id 4, 5
-- Station 3 (Airport):      connector_id 6, 7, 8
-- Station 4 (Makedonia):    connector_id 9, 10
-- Station 5 (Kalamaria):    connector_id 11, 12
-- Station 6 (Panorama):     connector_id 13, 14
-- Station 7 (Thermi):       connector_id 15, 16, 17
-- Station 8 (Tsimiski):     connector_id 18, 19
INSERT INTO connectors (connector_type, station_id) VALUES
  ('Type 2',  1), ('CCS',     1), ('CHAdeMO', 1),
  ('Type 2',  2), ('CCS',     2),
  ('Type 2',  3), ('CCS',     3), ('CHAdeMO', 3),
  ('Type 2',  4), ('CCS',     4),
  ('Type 2',  5), ('CCS',     5),
  ('Type 2',  6), ('CHAdeMO', 6),
  ('Type 2',  7), ('CCS',     7), ('CHAdeMO', 7),
  ('Type 2',  8), ('CCS',     8);

-- Availability windows: next 14 days, 00:00–23:59:59 per connector (24/7).
-- Admins can narrow these windows for maintenance; users book any sub-range within them.
DO $$
DECLARE
  d   DATE;
  cid INTEGER;
BEGIN
  FOR day_offset IN 0..13 LOOP
    d := CURRENT_DATE + day_offset;
    FOR cid IN 1..19 LOOP
      INSERT INTO available_slots (connector_id, date, start_time, end_time) VALUES
        (cid, d, '00:00', '23:59:59');
    END LOOP;
  END LOOP;
END $$;

-- Sample bookings
-- Past bookings (show as Completed in the UI):
--   kostas: Aristotelous/Type2, 3 days ago, ACTIVE
--   elena:  Nea Paralia/CCS, 1 day ago, ACTIVE
--   kostas: Airport/CCS, 2 days ago, CANCELLED
-- Upcoming bookings:
--   elena:  Nea Paralia/Type2, tomorrow, ACTIVE
--   kostas: Aristotelous/CCS, in 2 days, ACTIVE
--   kostas: Makedonia Palace/Type2, in 3 days, ACTIVE
--   elena:  Thermi/CCS, in 5 days, ACTIVE
--   kostas: Kalamaria/CCS, in 7 days, CANCELLED
INSERT INTO bookings (driver_username, station_id, connector_id, date, start_time, end_time, status) VALUES
  ('kostas', 1, 1,  CURRENT_DATE - 3, '09:00', '09:30', 'ACTIVE'),
  ('elena',  2, 5,  CURRENT_DATE - 1, '14:00', '14:40', 'ACTIVE'),
  ('kostas', 3, 7,  CURRENT_DATE - 2, '11:00', '11:40', 'CANCELLED'),
  ('elena',  2, 4,  CURRENT_DATE + 1, '08:00', '08:30', 'ACTIVE'),
  ('kostas', 1, 2,  CURRENT_DATE + 2, '10:00', '10:40', 'ACTIVE'),
  ('kostas', 4, 9,  CURRENT_DATE + 3, '13:00', '13:30', 'ACTIVE'),
  ('elena',  7, 16, CURRENT_DATE + 5, '15:00', '15:40', 'ACTIVE'),
  ('kostas', 5, 12, CURRENT_DATE + 7, '09:00', '09:20', 'CANCELLED');
