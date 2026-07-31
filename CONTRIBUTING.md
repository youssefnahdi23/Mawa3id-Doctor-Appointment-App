# Contributing to Mawa3id

Mawa3id is a **community-built, non-profit** doctor-appointment app for Tunisia.
It's built and maintained in the open, and contributions of every size are
welcome — whether you write code, translate a string, file a good bug report, or
just tell us what's confusing.

This guide gets you from a clone to a merged pull request.

## Ways to contribute

- **Code** — pick up an [open issue](https://github.com/youssefnahdi23/Mawa3id-Doctor-Appointment-App/issues),
  especially ones labelled `good first issue`, or propose something new.
- **Feedback** — the app has a built-in **Community → Send feedback** screen
  (bug / suggestion / praise). It goes straight to the maintainers.
- **Translations** — the app ships in **English, French, and Arabic (RTL)**. New
  strings must be added to all three (see [Localization](#localization)).
- **Docs** — improvements to this file, the READMEs, or in-code comments.

## Project layout

| Path        | What it is                                                        |
|-------------|-------------------------------------------------------------------|
| `backend/`  | Java 21 + Spring Boot REST API, PostgreSQL, Flyway migrations     |
| `mobile/`   | Flutter app (Android & iOS), Riverpod + go_router, en/fr/ar       |
| `docs/`     | Store-submission and process docs                                 |

## Development setup

### Backend (Java 21 + Maven)

Tests run against in-memory H2, so **you don't need PostgreSQL just to build and test**:

```bash
cd backend
mvn verify          # compiles, runs all tests, and enforces the coverage gate
```

To run the API locally, see [`README.md`](README.md) (Docker or local JDK). Flyway
migrations live in `backend/src/main/resources/db/migration` and are **append-only**:
add the next `V<n>__description.sql`, never edit an applied migration.

### Mobile (Flutter)

```bash
cd mobile
flutter pub get
dart run build_runner build          # regenerate JSON models (*.g.dart)
flutter gen-l10n                     # regenerate localization delegates
flutter analyze                      # must be clean
flutter test --exclude-tags integration
```

Generated files (`*.g.dart`, `lib/l10n/gen/`) are **git-ignored** and produced by
the commands above — don't commit them.

## Localization

Strings live in `mobile/lib/l10n/app_en.arb`, `app_fr.arb`, and `app_ar.arb`. All
three must stay at **key parity** — add your new key to every file, then run
`flutter gen-l10n`. Arabic is a real RTL locale; prefer `EdgeInsetsDirectional`
and `start`/`end` over `left`/`right` in new UI.

## Coding conventions

- Match the style of the surrounding code — naming, comment density, and idioms.
- Keep changes focused; unrelated cleanups belong in their own PR.
- Add or update tests for behaviour you change. Backend keeps a JaCoCo coverage
  gate; mobile favours widget/repository tests with `mocktail`.

## Pull requests

1. Branch off `main`.
2. Keep the PR scoped to one logical change; a short, reviewable diff merges faster.
3. Make sure CI is green: `mvn verify` (backend) and `flutter analyze` +
   `flutter test` (mobile) both pass locally before you push.
4. Write a clear description: what changed and why.

## Reporting bugs & requesting features

Open a [GitHub issue](https://github.com/youssefnahdi23/Mawa3id-Doctor-Appointment-App/issues)
with steps to reproduce (for bugs) or the problem you're trying to solve (for
features). In-app, **Community → Send feedback** is the quickest path.

## Code of conduct

Be respectful and constructive. This is a volunteer, healthcare-adjacent project
serving real patients and doctors — assume good faith and keep discussions kind.

Thank you for helping build Mawa3id. 💚
