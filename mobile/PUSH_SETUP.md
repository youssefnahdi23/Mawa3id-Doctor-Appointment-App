# Push notifications (Firebase Cloud Messaging)

The app registers a device token with the backend on login and receives push
notifications for appointment events. **Push degrades cleanly**: without the
Firebase config files below, the app still builds and runs — only system pushes
are inactive (the in-app notifications feed keeps working).

## To enable push (per environment)

1. Create a Firebase project and register the Android app (`com.mawa3id.app`)
   and the iOS app (bundle id from the Runner target).
2. **Android** — download `google-services.json` into `mobile/android/app/`.
   The Gradle build auto-applies the `google-services` plugin only when this
   file is present (see `android/app/build.gradle.kts`).
3. **iOS** — download `GoogleService-Info.plist` into `mobile/ios/Runner/` and
   add it to the Runner target in Xcode. Upload an **APNs auth key** to Firebase
   (Project settings → Cloud Messaging). `Runner/Runner.entitlements` is already
   wired into the Runner target's `CODE_SIGN_ENTITLEMENTS` build setting; it is
   `aps-environment = development` for local/debug builds, and the release CI
   flips it to `production` for signed App Store archives (see
   [RELEASE_SIGNING.md](RELEASE_SIGNING.md)).
4. **Backend** — set `FCM_CREDENTIALS` (service-account JSON) so the server can
   send. See `.env.example`.

Both native config files are gitignored — never commit them.
