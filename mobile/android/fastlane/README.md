# Play Store metadata (fastlane supply)

Version-controlled Google Play listing for **com.mawa3id.app**, in English,
French and Arabic. Uploaded with `fastlane deploy` (see `Fastfile`).

```
metadata/android/<locale>/
  title.txt                (≤ 30 chars)
  short_description.txt     (≤ 80 chars)
  full_description.txt      (≤ 4000 chars)
  video.txt                 (optional YouTube URL)
  changelogs/default.txt    (release notes for the next versionCode)
  images/
    icon.png                512×512 32-bit PNG
    featureGraphic.png      1024×500 PNG (required)
    phoneScreenshots/       2–8 PNG/JPEG, min 320px, 16:9 or 9:16
```

Locales: `en-US`, `fr-FR`, `ar`.

## Images (provide before submitting)

- `icon.png` — export from `mobile/assets/icon/app_icon.png` at 512×512 (swap for
  the real logo once it lands; see `pubspec.yaml` `flutter_launcher_icons`).
- `featureGraphic.png` — 1024×500 banner. Not auto-generated; add it per locale
  (or reuse the en-US one) before the first store push.
- `phoneScreenshots/` — **generated** by the screenshot harness in CI
  (`flutter drive` → `integration_test/screenshots_test.dart`) and consumed by the
  upload lane in the same run, so the PNGs are gitignored, not committed.

## Auth

`Appfile` reads the service-account JSON path from `PLAY_JSON_KEY_FILE`. In CI it
is written from the `PLAY_SERVICE_ACCOUNT_JSON` secret. See `docs/store/SUBMISSION.md`.
