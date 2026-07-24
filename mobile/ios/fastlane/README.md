# App Store metadata (fastlane deliver)

Version-controlled App Store Connect listing for **com.mawa3id.app**, in English,
French and Arabic. Uploaded with `fastlane deploy` (see `Fastfile`).

```
metadata/
  copyright.txt
  primary_category.txt      (MEDICAL)
  <locale>/
    name.txt                (≤ 30 chars)
    subtitle.txt            (≤ 30 chars)
    description.txt
    keywords.txt            (≤ 100 chars, comma-separated)
    promotional_text.txt    (≤ 170 chars)
    release_notes.txt
    support_url.txt
    marketing_url.txt       (optional)
    privacy_url.txt
screenshots/<locale>/        generated PNGs (see below)
```

Locales: `en-US`, `fr-FR`, `ar`.

## URLs

`support_url.txt` / `privacy_url.txt` currently point at
`https://REPLACE_WITH_YOUR_DOMAIN/legal...` — swap in the real production domain
(the backend serves `/legal/privacy` and `/legal/terms`; see Phase 7) before the
first submission.

## Screenshots (provide before submitting)

`screenshots/<locale>/` is **generated** by the screenshot harness in CI
(`flutter drive` → `integration_test/screenshots_test.dart`) and consumed by the
upload lane in the same run, so the PNGs are gitignored. App Store Connect requires
6.7" iPhone screenshots (and 12.9" iPad if the app is offered on iPad).

## Auth

`Appfile` reads `APPLE_ID` / `APPLE_TEAM_ID`; the lane authenticates with an App
Store Connect API key via `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8`. See
`docs/store/SUBMISSION.md`.
