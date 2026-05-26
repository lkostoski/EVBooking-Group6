# Testing

## Default test suite

Run the fast unit and contract tests:

```bash
mvn test
```

This covers service rules, RBAC annotations, auth-filter behavior, frontend workflow
contracts, deployment/config contracts, logging format, and error mapping.

## PostgreSQL integration tests

The real database/concurrency tests are opt-in because they reset the target
database by applying `schema.sql`.

Create a disposable database, then run:

```bash
createdb ev_booking_test

RUN_DB_INTEGRATION_TESTS=true \
TEST_JDBC_URL=jdbc:postgresql://localhost:5432/ev_booking_test \
TEST_DB_USER="$(whoami)" \
TEST_DB_PASSWORD="" \
mvn test
```

These tests validate the schema with Hibernate, persist server-side sessions,
exercise `BookingDAO` against PostgreSQL, and prove concurrent attempts to book
the same connector/time result in exactly one successful active booking.

Do not point `TEST_JDBC_URL` at a database that contains data you want to keep.
