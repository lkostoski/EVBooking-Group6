# EV Booking — Group 6

A cloud-ready EV charging station booking system built with Java/Jersey, PostgreSQL, and Bootstrap 5.

---

## Quick start

Pick your operating system below. The build is platform-agnostic (Java + Maven), but
installing PostgreSQL and launching the server use different commands on each platform.

- [macOS / Linux](#-macos--linux)
- [Windows](#-windows)

---

## macOS / Linux

### 1. Prerequisites

- **Java 17+** (the project targets Java 11, but 17 works)
- **Maven 3.6+**
- **PostgreSQL 14+** (17 recommended)

```bash
# macOS via Homebrew
brew install openjdk@17 maven postgresql@17
brew services start postgresql@17
```

```bash
# Linux (Debian/Ubuntu)
sudo apt update
sudo apt install openjdk-17-jdk maven postgresql
sudo systemctl start postgresql
```

### 2. Clone and configure

```bash
git clone <repo-url>
cd cloud_group6
cp run.sh.example run.sh
chmod +x run.sh
```

Open `run.sh` in a text editor and fill in the two API keys (get them from the group chat):

- `MAPS_KEY` — Google Maps API key
- `CLIENT_ID` — Google OAuth Client ID

`run.sh` is git-ignored so your keys won't be pushed.

### 3. Create the database and load sample data

```bash
createdb ev_booking
psql ev_booking < schema.sql
psql ev_booking < seed.sql
```

> `schema.sql` creates the tables; `seed.sql` populates 6 Greek charging stations and the
> three test accounts listed [below](#test-accounts).

### 4. Run

```bash
./run.sh
```

Then open **<http://localhost:8083>**.

---

## Windows

### 1. Prerequisites

Install all of the following — restart your terminal after each install so `PATH` updates.

- **Java 17+** — <https://adoptium.net> (pick "Temurin 17 LTS", run the `.msi`,
  tick *"Set JAVA_HOME"* and *"Add to PATH"* in the installer options)
- **Maven 3.6+** — <https://maven.apache.org/download.cgi> → "Binary zip", extract to
  e.g. `C:\Maven`, then add `C:\Maven\bin` to your `PATH`
- **PostgreSQL 14+** — <https://www.postgresql.org/download/windows/> (use the EDB
  installer; **remember the `postgres` superuser password you set** — you'll need it
  in `run.bat`). Also tick *"pgAdmin 4"* and *"Command Line Tools"* in the components
  step
- **Git** — <https://git-scm.com/download/win>

Verify everything in a **new** PowerShell or Command Prompt window:

```powershell
java -version
mvn -version
psql --version
```

> If `psql` is not recognised, add PostgreSQL's `bin` folder to PATH
> (default: `C:\Program Files\PostgreSQL\17\bin`). Open *"Edit the system environment
> variables"* → *Environment Variables* → edit `Path` → add that folder.

### 2. Clone and configure

```powershell
git clone <repo-url>
cd cloud_group6
copy run.bat.example run.bat
```

Open `run.bat` in Notepad (or VS Code) and fill in:

- `MAPS_KEY` — Google Maps API key (from the group chat)
- `CLIENT_ID` — Google OAuth Client ID (from the group chat)
- `DB_PASSWORD` — the password you set when installing PostgreSQL

`run.bat` is git-ignored so your keys won't be pushed.

### 3. Create the database and load sample data

Open a Command Prompt and run:

```cmd
psql -U postgres -c "CREATE DATABASE ev_booking;"
psql -U postgres -d ev_booking -f schema.sql
psql -U postgres -d ev_booking -f seed.sql
```

> Each command prompts for the `postgres` password you set during install. If `psql`
> isn't on PATH, run these from the PostgreSQL `bin` folder, or open *"SQL Shell
> (psql)"* from the Start menu and paste `\i schema.sql` / `\i seed.sql` after
> connecting to the `ev_booking` database.

### 4. Run

```cmd
run.bat
```

Then open **<http://localhost:8083>**.

---

## Test accounts

The seed data ships with three users:

| Username | Password   | Role   |
|----------|-----------|--------|
| `admin`  | `admin123`  | ADMIN  |
| `nikos`  | `driver123` | DRIVER |
| `maria`  | `driver123` | DRIVER |

- **DRIVER** can browse stations, view their own bookings, and create / modify / cancel
  bookings for themselves only.
- **ADMIN** can additionally manage stations, connectors, available slots, and any
  booking in the system.

---

## Project layout

```
src/main/java/com/evbooking/
  config/        Jersey + Jackson configuration
  dto/           Wire-format request/response objects
  exception/     Domain exceptions + central ExceptionMapper
  filter/        Auth, CORS, request-logging filters
  model/         JPA entities (User, ChargingStation, Connector, ...)
  repository/    DAOs (per-entity persistence)
  resource/      JAX-RS REST endpoints
  security/      HMAC session token + SecurityContext
  service/       Business logic (orchestrates DAOs + validation)
  util/          HibernateUtil (per-env JDBC resolution)

src/main/webapp/
  WEB-INF/web.xml      Servlet mapping (/api/*)
  *.html               UI pages
  js/                  Vanilla ES modules (api, auth, map)
  css/                 Stylesheet

src/test/java/         JUnit 5 tests (auth token, exception mapping, validation)

schema.sql             CREATE TABLE statements
seed.sql               Sample data + test users
run.sh / run.bat       Local launchers (gitignored — copy from .example)
```

---

## Tech stack

- **Backend** — Java 11, Jersey 3 (JAX-RS), Hibernate 6 / JPA, PostgreSQL, HikariCP
- **Frontend** — Vanilla JS (ES modules), Google Maps JavaScript API, custom CSS
- **Auth** — Database-backed session tokens (32-byte `SecureRandom`, `HttpOnly`
  cookie) + BCrypt password hashing, optional Google Sign-In (OAuth 2.0 ID
  token verification)
- **Tests** — JUnit 5 (54 tests across 14 classes cover validation rules,
  exception mapping, security annotations, filters, and booking-rule contracts)
- **Deployment** — Heroku-style PaaS (webapp-runner / embedded Tomcat 10)

---

## Troubleshooting

**`psql: command not found`** — PostgreSQL's `bin` folder isn't on PATH. On macOS with
Homebrew, run `brew link postgresql@17 --force`. On Windows, add
`C:\Program Files\PostgreSQL\17\bin` to PATH as described above.

**`Address already in use` on port 8083** — Another process is listening. The launcher
scripts try to free the port automatically; if that fails, find the process manually:

- macOS / Linux: `lsof -ti :8083 | xargs kill`
- Windows: `netstat -ano | findstr :8083` then `taskkill /F /PID <pid>`

**White page in the browser after a code update** — Your browser is caching the old
JavaScript. Hard-refresh with **Cmd + Shift + R** (macOS) or **Ctrl + Shift + R**
(Windows / Linux), or open the page in an incognito window.

**`EntityManagerFactory creation failed`** — The app couldn't connect to PostgreSQL.
Check that the database `ev_booking` exists, the schema has been loaded, and the
`DB_USER` / `DB_PASSWORD` in the launcher script match your local PostgreSQL install.
