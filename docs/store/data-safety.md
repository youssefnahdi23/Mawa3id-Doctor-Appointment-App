# Data safety / privacy labels

Answers for the **Google Play Data Safety** form and the **Apple App Privacy**
("nutrition label") questionnaire, derived from what the backend actually stores.
Keep this in sync with `/legal/privacy` (served by the backend) whenever the data
model changes.

## Summary

- **Encrypted in transit:** yes — all traffic is HTTPS/TLS (Caddy).
- **Data sold:** no.
- **Data shared with third parties:** only processors needed to run the service —
  **Stripe** (donation payments), **Google Firebase Cloud Messaging** (push
  delivery), and the hosting provider. Not shared for advertising.
- **Account deletion:** users can request access/correction/deletion by email
  (`youssefnahdi95@gmail.com`); a deletion path is offered per store policy.
- **Card data:** entered on Stripe; it never reaches our servers.

## Data types (source of truth: backend entities)

| Data | Where it comes from | Collected | Shared | Purpose | Optional? |
|------|--------------------|-----------|--------|---------|-----------|
| Name | auth/profile (patient, doctor) | Yes | No | App functionality, account | Required |
| Email address | auth | Yes | No | Account, app functionality | Required |
| Phone number | profile | Yes | No | Account, app functionality | Optional |
| Date of birth | patient profile | Yes | No | App functionality | Required (patient) |
| Password | auth (stored bcrypt-hashed) | Yes | No | Authentication | Required |
| Health info — appointments | booking | Yes | No | App functionality | Required to book |
| Health info — medical records (diagnosis, notes, prescription) | doctor visit | Yes | No | App functionality | Created by doctor |
| Reviews & ratings | patient after visit | Yes | Yes (shown publicly on the doctor) | App functionality | Optional |
| Purchase/donation history (amount, currency, status) | donations | Yes | Yes (Stripe) | Support the non-profit | Optional |
| Payment card details | — | **No** (handled by Stripe) | — | — | — |
| Device push token (FCM) | push registration | Yes | Yes (Google FCM) | Send notifications | Optional |
| App activity / diagnostics (request + rate-limit logs) | server | Yes | No | Security, prevent abuse | Required |

## Google Play Data Safety — mapping

Declare these **collected data types**:

- **Personal info:** Name; Email address; Phone number; Other info (date of birth).
- **Financial info:** Purchase history (donation records). *Do not* declare
  payment/card info — it is processed by Stripe and not collected by the app.
- **Health and fitness → Health info:** appointments and medical records.
- **Messages / other:** none.
- **App activity:** none for analytics; only security logging (not user-facing
  analytics).
- **Device or other IDs:** the FCM push token (declare under "Device or other IDs"
  if prompted; used only for messaging, not tracking/ads).

For every type: **Collected = yes, Processed ephemerally = no, Shared** only where
the table says yes, **encrypted in transit = yes**, **user can request deletion =
yes**. Nothing is used for advertising or shared with data brokers.

## Apple App Privacy — mapping

Data linked to the user (used for **App Functionality** only, **not** tracking):

- **Contact Info:** Name, Email, Phone Number.
- **Health & Fitness:** Health (appointments, medical records). *(Also disclose in
  App Review notes that the app handles health data; see the runbook.)*
- **Sensitive Info:** health data as above.
- **User Content:** reviews.
- **Purchases:** donation history.
- **Identifiers:** Device ID (FCM token) — App Functionality, not tracking.
- **Diagnostics:** none (crash/analytics SDKs are not integrated).

Set **"Used to Track You" = No** for all — the app has no advertising or
cross-app tracking. Payment card data is **not** collected (Stripe handles it).
