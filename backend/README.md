# Mawa3id Backend

Spring Boot REST API for the Mawa3id doctor appointment app.

## Stack

- Java 21, Spring Boot 3.3
- Spring Web, Spring Security (stateless JWT), Spring Data JPA, Bean Validation
- PostgreSQL + Flyway (runtime); H2 in-memory (tests)
- springdoc-openapi (Swagger UI)

## Architecture

Feature-package layout (`com.mawa3id.<feature>`):

- `user` — `User` auth entity + `Role` (PATIENT/DOCTOR)
- `security` — `JwtService`, `JwtAuthenticationFilter`, `AppUserDetails(Service)`
- `auth` — registration/login/`me`, issues JWTs
- `specialty` — specialty catalog (seeded via Flyway `V1__init.sql`)
- `doctor` — doctor profile, browse/filter/search, self-service update
- `patient` — patient profile, auto-generated `patientCode` (e.g. `MW-7K3PQ9`)
- `schedule` / `appointment` — weekly availability, computed slots, booking lifecycle
- `donation` — Stripe-backed card donations behind a `PaymentGateway` interface, plus a
  Patreon link; the Stripe adapter is stubbed in tests
- `common` — `ApiError` + `GlobalExceptionHandler` (consistent error shape)
- `config` — security, CORS, OpenAPI, stable `Page` serialization

Conventions: constructor injection, DTOs at the controller boundary (entities are
never serialized directly), `@Valid` request DTOs, a single global exception
handler.

## Running

```bash
# PostgreSQL must be running with a `mawa3id` database.
DB_URL=jdbc:postgresql://localhost:5432/mawa3id \
DB_USERNAME=mawa3id DB_PASSWORD=mawa3id \
mvn spring-boot:run
```

### Configuration (environment variables)

No secrets are committed to the repository — passwords and keys come from the
environment.

| Variable | Default | Notes |
|----------|---------|-------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/mawa3id` | JDBC URL |
| `DB_USERNAME` | `mawa3id` | DB user |
| `DB_PASSWORD` | _(empty)_ | **set this** — DB password |
| `JWT_SECRET` | _(empty)_ | Base64 key ≥ 256-bit. If unset, an **ephemeral** key is generated at startup (tokens don't survive restarts). **Set in production.** |
| `JWT_EXPIRATION` | `86400000` | token lifetime in ms (24h) |
| `JWT_FAIL_ON_MISSING_SECRET` | `false` | when `true`, the app **refuses to boot** without a `JWT_SECRET` instead of generating an ephemeral key. **Set `true` in staging/production.** |
| `RATELIMIT_ENABLED` | `true` | per-IP throttle on `/api/auth/login` and `/api/auth/register/**` |
| `RATELIMIT_AUTH_CAPACITY` | `10` | max auth requests per IP per window |
| `RATELIMIT_AUTH_WINDOW_SECONDS` | `60` | rate-limit window length |
| `DONATIONS_ENABLED` | `true` | when `false`, `POST /api/donations` returns `503` (Patreon link still served) |
| `DONATION_CURRENCY` | `usd` | default ISO-4217 currency when a request omits one |
| `DONATION_MIN_AMOUNT_MINOR` | `100` | minimum accepted amount in minor units (e.g. cents) |
| `PATREON_URL` | _(empty)_ | Patreon link returned by `/api/donations/config` |
| `STRIPE_SECRET_KEY` | _(empty)_ | Stripe API secret key — **set to enable card donations** |
| `STRIPE_WEBHOOK_SECRET` | _(empty)_ | Stripe webhook signing secret, verifies `POST /api/donations/webhook` |
| `STRIPE_SUCCESS_URL` | `.../donation/success` | redirect after a successful checkout |
| `STRIPE_CANCEL_URL` | `.../donation/cancel` | redirect after a cancelled checkout |
| `NOTIFICATIONS_REMINDER_ENABLED` | `true` | enable the scheduled appointment-reminder job |
| `NOTIFICATIONS_REMINDER_WINDOW_HOURS` | `24` | how far ahead an `ACCEPTED` appointment is reminded about |
| `NOTIFICATIONS_REMINDER_CRON` | `0 0 * * * *` | reminder job schedule (Spring cron; default hourly) |
| `SERVER_PORT` | `8080` | HTTP port |

### Operational endpoints

- `GET /actuator/health` — liveness/readiness (public, no auth). Use for container probes.

### Hardening notes

- **Auth rate limiting** returns `429 Too Many Requests` once an IP exceeds the
  window. State is in-memory (per instance) — sufficient for a single-instance
  deployment; use a shared store if scaling horizontally.
- **Consistent errors:** malformed JSON, bad path/param types, unsupported methods,
  and unknown routes return structured `4xx` `ApiError` bodies (not `500`).

## Testing

```bash
mvn test          # run the test suite (H2 in-memory)
mvn verify        # tests + JaCoCo coverage report and gate
```

Tests run entirely on in-memory H2 (Flyway disabled, Hibernate `create-drop`),
so no external database is needed. Coverage: patient register → login → `me`,
JWT enforcement (401 anonymous / 403 wrong role), doctor browse + specialty
filter + name search, doctor profile update including `acceptanceMode`, plus
duplicate-email (409) and validation (400) cases.

## Data model

```
users(id, email UNIQUE, password_hash, role, created_at)
specialties(id, name UNIQUE, description)
patients(user_id PK→users, full_name, date_of_birth, patient_code UNIQUE)
doctors(user_id PK→users, name, specialty_id→specialties, cabinet_address,
        working_hours, phone, bio, acceptance_mode, rating_count, rating_average)
donations(id, user_id→users NULLABLE, amount_minor, currency, status, provider,
          provider_session_id UNIQUE, provider_payment_ref, donor_name, message,
          created_at, updated_at)
medical_records(id, appointment_id→appointments UNIQUE, diagnosis, notes,
          prescription, follow_up_date, created_at, updated_at)
reviews(id, appointment_id→appointments UNIQUE, rating, comment,
          created_at, updated_at)
notifications(id, user_id→users, type, message, appointment_id→appointments NULLABLE,
          read_at NULLABLE, created_at)
```

### Donation lifecycle

`POST /api/donations` persists a `PENDING` donation (anonymous, or attributed to the
caller when a JWT is present), opens a Stripe Checkout session, and returns its
`checkoutUrl`. Stripe later calls `POST /api/donations/webhook`; the signed event is
verified and the matching donation transitions to `SUCCEEDED` / `EXPIRED` / `FAILED`.
Webhook handling is idempotent — unknown sessions and already-finalised donations are
acknowledged with no change.

### Medical records (iteration 4)

A doctor closes out a visit and attaches a **medical record** (diagnosis, notes,
prescription, optional follow-up date) via `POST /api/appointments/{id}/record`. Records
belong to completed visits: attaching one to an `ACCEPTED` appointment auto-completes it,
and `PUT /api/appointments/{id}/complete` closes a visit without notes. At most one record
exists per appointment (a duplicate is `409`). The linked appointment's doctor and patient
may read it (`GET /api/appointments/{id}/record`); the patient's full history is at
`GET /api/records/me`.

### Reviews & ratings (iteration 5)

After a `COMPLETED` visit, the patient submits a 1–5 star **review** (optional comment)
via `POST /api/appointments/{id}/review` — one per completed appointment (a duplicate is
`409`). Each write recomputes the doctor's denormalised `rating_count` / `rating_average`,
which surface on `GET /api/doctors` and `GET /api/doctors/{id}`. Reviews are publicly
listed per doctor at `GET /api/doctors/{id}/reviews` (paginated, newest first); the patient
reads their own at `GET /api/reviews/me`.

### Notifications & reminders (iteration 6)

Each appointment lifecycle transition emits an in-app **notification** to the affected user,
persisted in the same transaction as the state change (booking → doctor; accept/reject/complete
→ patient; cancel → the other party). A `@Scheduled` job (`NotificationScheduler`) calls
`NotificationService.dispatchDueReminders(...)` to remind both parties of `ACCEPTED`
appointments starting within `mawa3id.notifications.reminder.window-hours`; the scan is
idempotent per appointment. Users read their feed at `GET /api/notifications`
(paginated, `unread` filter), see `GET /api/notifications/unread-count`, and mark items read
with `PUT /api/notifications/{id}/read` / `PUT /api/notifications/read-all`. The reminder job is
disabled in the test profile so the suite drives `dispatchDueReminders` deterministically.

## Example flow

```bash
# Register a patient (returns a JWT)
curl -X POST localhost:8080/api/auth/register/patient \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"password123","fullName":"Alice","dateOfBirth":"1990-05-20"}'

# Browse cardiologists
curl 'localhost:8080/api/doctors?specialtyId=2'

# Authenticated request
curl localhost:8080/api/auth/me -H "Authorization: Bearer <TOKEN>"
```
