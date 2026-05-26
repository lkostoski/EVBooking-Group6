# EV Booking — Playwright E2E Tests

End-to-end tests for the EV Booking frontend using [Playwright](https://playwright.dev/).

## Test suites

| File | What it covers |
|------|---------------|
| `auth.spec.js` | Login, logout, registration, redirect guards |
| `stations.spec.js` | Station list, search, detail panel, navbar admin link |
| `booking.spec.js` | Full booking flow, filters, modify, cancel, driver isolation |
| `admin.spec.js` | Admin CRUD for stations/connectors/slots, all-bookings tab |
| `rbac.spec.js` | Role enforcement via UI and direct API calls |
| `profile.spec.js` | Profile display, password change |

## Prerequisites

- Node.js 18+
- The local server must be **running and seeded** before running tests:
  ```
  # From the project root:
  mvn package -DskipTests && java -jar target/dependency/webapp-runner.jar --port 8083 target/ev-booking.war
  # Then seed the DB:
  psql ev_booking < schema.sql
  psql ev_booking < seed.sql
  ```
- Seed data creates three accounts used by the tests:
  - `admin` / `admin123` (ADMIN)
  - `kostas` / `driver123` (DRIVER)
  - `elena` / `driver123` (DRIVER)

## Install

```bash
cd e2e
npm install
npx playwright install chromium
```

## Run locally (against local server)

```bash
cd e2e
BASE_URL=http://localhost:8083 npm test
```

## Run against Heroku

```bash
cd e2e
BASE_URL=https://ev-booking-group6-a96b906b9ee0.herokuapp.com npm test
```

## Run headed (to watch the browser)

```bash
cd e2e
BASE_URL=http://localhost:8083 npm run test:headed
```

## View HTML report

```bash
cd e2e
npm run test:report
```

## Notes

- Tests are isolated: booking and admin tests clean up created data in `afterEach` hooks.
- Retries are set to 1 — a flaky network or slow startup will not cause false failures.
- The `BASE_URL` environment variable defaults to `http://localhost:8083` if omitted.
