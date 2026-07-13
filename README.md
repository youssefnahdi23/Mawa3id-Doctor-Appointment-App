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
| 3 | Donations (card via Stripe + Patreon link) | ✅ Done |
| 4 | Medical records: doctors complete visits and attach visit notes; patients read their history | ✅ Done |
| 5 | Reviews & ratings: patients rate doctors after a completed visit; aggregate rating on browse/detail | ✅ Done |
| 6 | Notifications & reminders: in-app feed for lifecycle events + scheduled upcoming-appointment reminders | ✅ Done |
| 7 | Hardening: pagination on all list endpoints + explicit configurable clinic timezone | ✅ Done |
| 8 | Flutter mobile app | ⏳ Planned |

Doctors publish weekly availability windows and a per-doctor slot duration; the server
computes bookable slots. Booking honours the doctor's `acceptanceMode` (`MANUAL` →
`PENDING`, `AUTO` → `ACCEPTED`). Times are clinic-local: a single configurable clinic zone
(`CLINIC_ZONE`, default `UTC`) is used for every "now" comparison, so behaviour no longer
depends on the server's default time zone.

List endpoints are paginated and return a Spring `Page` (`{ "content": [...], "page": {
"size", "number", "totalElements", "totalPages" } }`) with `page` (0-based) and `size`
(max 100) query params.

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

Donations (iteration 3) add: `DONATIONS_ENABLED`, `DONATION_CURRENCY`,
`DONATION_MIN_AMOUNT_MINOR`, `PATREON_URL`, and the Stripe settings
`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_SUCCESS_URL`,
`STRIPE_CANCEL_URL`. Card donations are disabled cleanly (503) if the feature is
turned off; the Patreon link is still served from `PATREON_URL`.

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
| `GET`  | `/api/appointments/me?status=&page=&size=` | `PATIENT` | Patient's appointments (paginated) |
| `GET`  | `/api/appointments?status=&page=&size=` | `DOCTOR` | Doctor's appointment queue (paginated) |
| `PUT`  | `/api/appointments/{id}/accept`, `/reject` | `DOCTOR` | Accept / reject a pending appointment |
| `PUT`  | `/api/appointments/{id}/cancel` | `PATIENT` or `DOCTOR` | Cancel own appointment |

### Donations (iteration 3)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/donations` | public | Start a donation; returns a Stripe Checkout `checkoutUrl`. Anonymous, or attributed if a token is sent |
| `POST` | `/api/donations/webhook` | public | Stripe webhook (verified by signature); finalises the donation |
| `GET`  | `/api/donations/me` | JWT | Caller's donation history |
| `GET`  | `/api/donations/config` | public | Card availability, currency, minimum amount, and Patreon link |

Amounts are in the currency's **minor units** (e.g. cents). Send the token as
`Authorization: Bearer <token>`.

### Medical records (iteration 4)

Doctors close out a visit and attach a **medical record** (diagnosis, notes, prescription,
optional follow-up date). A record belongs to a completed visit — attaching one to an
`ACCEPTED` appointment auto-completes it. The patient reads the record for their own
appointment and browses their full visit history.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `PUT`  | `/api/appointments/{id}/complete` | `DOCTOR` | Mark an `ACCEPTED` appointment `COMPLETED` |
| `POST` | `/api/appointments/{id}/record` | `DOCTOR` | Attach a visit record (appointment must be `ACCEPTED`/`COMPLETED`; auto-completes if accepted); `409` if one already exists |
| `PUT`  | `/api/appointments/{id}/record` | `DOCTOR` | Update the record |
| `GET`  | `/api/appointments/{id}/record` | `DOCTOR` or `PATIENT` | Read the record (owner of that appointment only) |
| `GET`  | `/api/records/me?page=&size=` | `PATIENT` | Caller's visit history, newest first (paginated) |

### Reviews & ratings (iteration 5)

After a **completed** visit, the patient leaves a **1–5 star rating** with an optional
comment (one review per completed appointment). Each doctor's **average rating and review
count** are then surfaced on `GET /api/doctors` (browse) and `GET /api/doctors/{id}`
(detail), and the reviews themselves are publicly listable per doctor.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/appointments/{id}/review` | `PATIENT` | Leave a review (appointment must be `COMPLETED`); `409` if one already exists |
| `PUT`  | `/api/appointments/{id}/review` | `PATIENT` | Update own rating/comment |
| `GET`  | `/api/appointments/{id}/review` | `DOCTOR` or `PATIENT` | Read the review (owner of that appointment only) |
| `GET`  | `/api/doctors/{id}/reviews?page=&size=` | public | A doctor's reviews, newest first |
| `GET`  | `/api/reviews/me?page=&size=` | `PATIENT` | Caller's own reviews (paginated) |

### Notifications & reminders (iteration 6)

Appointment lifecycle transitions generate **in-app notifications** for the affected party
(booking → doctor; accept/reject/complete → patient; cancel → the other party). A scheduled
job also emits **reminders** to both parties for `ACCEPTED` appointments starting within a
configurable window (default 24h; idempotent per appointment). The API is read + mark-read only.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET`  | `/api/notifications?unread=&page=&size=` | JWT | Caller's notifications, newest first; optional `unread=true` |
| `GET`  | `/api/notifications/unread-count` | JWT | `{ "count": N }` of unread notifications |
| `PUT`  | `/api/notifications/{id}/read` | JWT | Mark one read (owner only) |
| `PUT`  | `/api/notifications/read-all` | JWT | Mark all caller's notifications read |

Reminder settings: `NOTIFICATIONS_REMINDER_ENABLED` (default `true`),
`NOTIFICATIONS_REMINDER_WINDOW_HOURS` (default `24`), `NOTIFICATIONS_REMINDER_CRON`
(default hourly).
