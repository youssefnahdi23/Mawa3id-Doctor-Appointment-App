# Content rating

Mawa3id is a medical appointment-booking utility. It has no violence, sexual,
gambling, or mature content. It does surface **medical/treatment information**
(diagnosis, prescription notes), which the rating questionnaires ask about.

## Google Play — IARC questionnaire

Category: choose **Utility / Productivity / Health** style (not Game).

Answer **No** to essentially everything:

- Violence / scary content — No
- Sexuality / nudity — No
- Profanity / crude humour — No
- Controlled substances (drugs, alcohol, tobacco) — No. *(A doctor's prescription
  is medical information, not promotion of drug use — answer No; if the tool asks
  specifically about references to medical/pharmaceutical information you may
  disclose it, but it does not raise the rating.)*
- Gambling (simulated or real) — No
- User-to-user interaction / user-generated content — **Yes** (patients leave
  reviews; patient↔doctor share appointment info). Declare it; provide moderation
  contact `youssefnahdi95@gmail.com`.
- Shares user location — No
- Digital purchases — **Yes** (optional donations via Stripe).

Expected result: **Everyone / PEGI 3 / rated for all ages**.

## Apple App Store — age rating

Answer the age-rating questions all **None**; the app has no objectionable
content. Expected rating: **4+**.

- Medical/Treatment Information: the questionnaire has a "Medical/Treatment
  Information" toggle — set it to **Yes** (the app presents health/medical
  information). This does not by itself raise the rating but must be declared.
- Unrestricted Web Access — No.
- Gambling / Contests — No.

### Category

- **Primary category: Medical** (set in `ios/fastlane/metadata/primary_category.txt`).
- Secondary (optional): Health & Fitness.

## Notes for review

Because the app handles **health data** and shows medical records, App Review may
scrutinise it under the Health & Fitness / Medical guidelines. In the review notes
(see `SUBMISSION.md`) state clearly that:

- Mawa3id is a scheduling/communication tool; it does **not** provide medical
  advice, diagnosis, or treatment (also stated in-app in the Terms).
- Medical records are authored by the treating doctor and visible only to that
  doctor and the patient.
- Provide a demo patient **and** doctor account so the reviewer can see the flow.
