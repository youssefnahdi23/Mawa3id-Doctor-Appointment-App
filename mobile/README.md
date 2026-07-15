# Mawa3id — Flutter mobile app

Patient & doctor client for the Mawa3id backend (`../backend`). Covers
authentication for both roles, doctor browsing with search/filters and ratings,
slot picking & booking, the doctor agenda with a minimal weekly-availability
editor, in-app notifications, and the **post-visit flow** — doctors attach a
medical record (which completes the visit), patients read their record history
and leave star reviews — in **English, French and Arabic (RTL)**.

## Quick start

```bash
cd mobile
flutter pub get
flutter gen-l10n
dart run build_runner build --delete-conflicting-outputs
flutter run --dart-define=API_BASE_URL=http://localhost:8080
```

`API_BASE_URL` defaults to `http://10.0.2.2:8080` on the **Android emulator**
(the emulator's alias for the host machine) and `http://localhost:8080`
everywhere else, so with a locally running backend (`docker compose up` at the
repo root) you usually don't need the flag at all.

## Code generation

Generated files (`lib/l10n/gen/`, `**/*.g.dart`) are **not committed**; rerun
after touching ARB files or `@JsonSerializable` models:

```bash
flutter gen-l10n
dart run build_runner build --delete-conflicting-outputs
```

## Tests

```bash
flutter analyze
flutter test --exclude-tags integration   # unit + widget (default in CI)
```

Integration flows exercise a real backend:

```bash
cd .. && docker compose up -d --build && cd mobile
flutter test --tags integration --dart-define=API_BASE_URL=http://localhost:8080
```

Note: the backend rate-limits auth endpoints (10 requests/60 s/IP); rerunning
the integration suite several times within a minute can trip 429s.

## Structure

```
lib/
  app/         router (role-branched shells), app widget, splash
  core/        dio client + auth interceptor, error mapping, page wrapper,
               secure token storage, wall-clock time codec, l10n helpers
  features/    auth, doctors, appointments, availability, notifications,
               records, reviews
               (each: data/ models+repository, state/ controllers, ui/)
  l10n/        app_{en,fr,ar}.arb (+ generated gen/, gitignored)
```

**Time rule:** appointment/slot times from the API are clinic-local wall-clock
strings without a zone. `core/time/wall_clock.dart` is the only place that
parses/formats them — never `toUtc()`/`toLocal()` on those values. The single
exception is `NotificationResponse.createdAt`, a genuine UTC instant used for
relative timestamps.

## Out of scope (this iteration)

Donations, push notifications (in-app only), doctor profile editing.
