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
| `SERVER_PORT` | `8080` | HTTP port |

## Testing

```bash
mvn test
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
        working_hours, phone, bio, acceptance_mode)
```

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
