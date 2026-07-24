# Release signing & the `release-build` CI

The [`release-build.yml`](../.github/workflows/release-build.yml) workflow builds
the **signed** Android App Bundle (`.aab`) and iOS App Store IPA (`.ipa`) on GitHub
Actions (Android on `ubuntu-latest`, iOS on `macos-latest`). It runs on manual
**Run workflow** (`workflow_dispatch`) and on pushing a `v*` tag.

**It degrades cleanly.** Every signing step is guarded on the presence of its
secrets. With **no** secrets configured the workflow still runs and produces:

- an **unsigned/debug-signed** Android AAB + APK, and
- an iOS **`--no-codesign`** compile plus **iOS Simulator screenshots** (uploaded
  as a build artifact — the way to see the iOS app from a Windows machine).

Add the secrets below to upgrade those to fully **signed release** artifacts. No
code change is needed — the build picks the secrets up automatically.

---

## Android secrets (repo → Settings → Secrets and variables → Actions)

| Secret | What it is |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 of your upload keystore (`upload-keystore.jks`). |
| `ANDROID_KEY_ALIAS` | Key alias inside the keystore (e.g. `upload`). |
| `ANDROID_KEY_PASSWORD` | Password for that key. |
| `ANDROID_STORE_PASSWORD` | Password for the keystore. |
| `ANDROID_GOOGLE_SERVICES_JSON` | *(optional)* Contents of `google-services.json` so release builds have live FCM push. Omit to build without push. |

### Generate the upload keystore (once)

```bash
keytool -genkey -v -keystore upload-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Keep the `.jks` file **and** the passwords backed up securely — losing them
complicates future Play Store updates. Then base64-encode it for the secret:

```bash
# macOS / Linux
base64 -i upload-keystore.jks | tr -d '\n' > keystore.b64
# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Set-Content keystore.b64
```

Paste the contents of `keystore.b64` into `ANDROID_KEYSTORE_BASE64`.

CI writes these into `android/key.properties` (with `storeFile=upload-keystore.jks`)
and the existing Gradle logic in `android/app/build.gradle.kts` signs the release
build. See also [`android/key.properties.example`](android/key.properties.example).

---

## iOS secrets — App Store Connect API key (automatic signing)

No certificates or provisioning profiles to manage by hand: the API key lets Xcode
create and download the distribution certificate + profile during the build
(`xcodebuild -allowProvisioningUpdates`).

| Secret | What it is |
| --- | --- |
| `IOS_ASC_KEY_P8` | Contents of the App Store Connect API key `.p8` file. |
| `APP_STORE_CONNECT_KEY_ID` | The key's Key ID (from App Store Connect). |
| `APP_STORE_CONNECT_ISSUER_ID` | Your API Issuer ID. |
| `APPLE_TEAM_ID` | Your 10-character Apple Developer Team ID. |
| `IOS_GOOGLE_SERVICE_INFO_PLIST` | *(optional)* Contents of `GoogleService-Info.plist` for live FCM push. |

### Create the API key

App Store Connect → **Users and Access → Integrations → App Store Connect API** →
generate a team key with the **App Manager** role. Download the `.p8` (one time
only). Copy its full contents into `IOS_ASC_KEY_P8`, and note the **Key ID** and
**Issuer ID** shown on that page.

The Team ID is in the Apple Developer portal → **Membership**.

Prerequisite: register the app's bundle id `com.mawa3id.app` (App IDs) with the
**Push Notifications** capability enabled, and create the app record in App Store
Connect. CI supplies `APPLE_TEAM_ID` to `ios/ExportOptions.plist` and flips the
push entitlement to `production` on a build-time copy before archiving.

---

## What the CI produces (download from the run's **Artifacts**)

| Artifact | When |
| --- | --- |
| `android-release` (`.aab` + `.apk`) | always (signed once Android secrets exist) |
| `ios-release` (`.ipa`) | only when the iOS signing secrets exist |
| `ios-simulator-screenshots` | always — view the iOS app from Windows |

Uploading these to the Play Console / TestFlight, plus the store listing, privacy
and screenshots, is covered by the submission runbook:
[`docs/store/SUBMISSION.md`](../docs/store/SUBMISSION.md). Listing copy lives under
`android/fastlane/metadata` and `ios/fastlane/metadata`; the `store-release.yml`
workflow pushes it. Data-safety/privacy-label and content-rating answers are in
[`docs/store/data-safety.md`](../docs/store/data-safety.md) and
[`docs/store/content-rating.md`](../docs/store/content-rating.md).
