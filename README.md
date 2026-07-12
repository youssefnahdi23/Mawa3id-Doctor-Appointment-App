# Mawa3id — Doctor Appointment App

Mawa3id is a mobile app that connects **patients** and **doctors** for booking
medical appointments (RDV). It is a **non-profit** project; users can optionally
support development via donations (bank card / Patreon).

- **Mobile:** Flutter (Android & iOS) — _future iteration_
- **Backend:** Java 21, Spring Boot, PostgreSQL — _this repository's `backend/`_

## Roadmap

| Iteration | Scope | Status |
|-----------|-------|--------|
| 1 | Backend foundation: JWT auth (doctors & patients), doctor profiles, specialties, browse/filter | ✅ Done |
| 2 | Appointments (RDV): weekly availability, computed slots, booking, manual/auto acceptance | ✅ Done |
| 3 | Donations (card via Stripe + Patreon link) | ⏳ Planned |
| 4 | Flutter mobile app | ⏳ Planned |

Doctors publish weekly availability windows and a per-doctor slot duration; the server
computes bookable slots. Booking honours the doctor's `acceptanceMode` (`MANUAL` →
`PENDING`, `AUTO` → `ACCEPTED`). Times are clinic-local (single-region; no timezone
conversion yet).

## Backend

Spring Boot REST API. See [`backend/README.md`](backend/README.md) for full details.

### Quick start

Requires JDK 21, Maven, and PostgreSQL.

```bash
# 1. Create the database
createdb mawa3id           # or: CREATE DATABASE mawa3id;

# 2. Run — supply the DB password (and, for non-local use, a JWT secret) via env vars
cd backend
DB_PASSWORD=yourpassword mvn spring-boot:run
```

No secrets are committed. Configure connection and secrets via environment
variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
`JWT_EXPIRATION`, `SERVER_PORT`. If `JWT_SECRET` is unset the app generates an
**ephemeral** signing key at startup (fine locally; tokens are invalidated on
restart) — always set `JWT_SECRET` in staging/production.

Interactive API docs: `http://localhost:8080/swagger-ui.html`

### Run the tests

Tests use an in-memory H2 database, so **no PostgreSQL is required**:

```bash
cd backend
mvn test
```

### Key endpoints (iteration 1)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/register/patient` | public | Register a patient, returns JWT |
| `POST` | `/api/auth/register/doctor` | public | Register a doctor, returns JWT |
| `POST` | `/api/auth/login` | public | Log in, returns JWT |
| `GET`  | `/api/auth/me` | JWT | Current user identity |
| `GET`  | `/api/specialties` | public | List specialties |
| `GET`  | `/api/doctors?specialtyId=&q=&page=&size=` | public | Browse / filter doctors |
| `GET`  | `/api/doctors/{id}` | public | Doctor detail |
| `PUT`  | `/api/doctors/me` | `DOCTOR` | Update own profile (incl. `acceptanceMode`, `slotDurationMinutes`) |
| `GET`  | `/api/patients/me`, `PUT /api/patients/me` | `PATIENT` | View / update own profile |

### Appointments (iteration 2)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET`  | `/api/doctors/{id}/availability` | public | Doctor's weekly availability |
| `PUT`  | `/api/doctors/me/availability` | `DOCTOR` | Replace own weekly availability |
| `GET`  | `/api/doctors/{id}/slots?from=&to=` | public | Computed free slots (range ≤ 31 days) |
| `POST` | `/api/appointments` | `PATIENT` | Book a slot (`PENDING` or `ACCEPTED` per `acceptanceMode`) |
| `GET`  | `/api/appointments/me?status=` | `PATIENT` | Patient's appointments |
| `GET`  | `/api/appointments?status=` | `DOCTOR` | Doctor's appointment queue |
| `PUT`  | `/api/appointments/{id}/accept`, `/reject` | `DOCTOR` | Accept / reject a pending appointment |
| `PUT`  | `/api/appointments/{id}/cancel` | `PATIENT` or `DOCTOR` | Cancel own appointment |

Send the token as `Authorization: Bearer <token>`.
