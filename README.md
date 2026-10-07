# ASTRA-SIRIUS-LoreMotion

New project assembled from the working ASTRA Browser Agent and the SIRIUS distributed Android core.

## Current architecture

Telegram -> ASTRA backend -> Browser Agent -> Chromium/CDP -> LoreMotion -> manual Turnstile if required -> MP4 -> Telegram

Android SIRIUS remains the distributed node/control layer. Browser automation stays in the proven Termux/Node environment rather than moving Chromium into the Android APK.

## Included now

- SIRIUS Android Gradle project reconstructed from `alexhook098-sys/sirius-distributed-core`
- SIRIUS node/router/client/task dispatcher/services
- SIRIUS Browser Bridge class
- Browser Worker / Inspector / LLM Planner / Runner / Agent v4-v5 building blocks
- LoreMotion adapter with 9:16 selection and human-in-the-loop Turnstile
- ASTRA Associative Interest Engine v0.2
- no API keys or bot tokens

## Verification status

- Python Interest Engine test: PASS
- JavaScript syntax checks: PASS
- Android Gradle build: pending because the current execution environment does not have Gradle/Android SDK installed
- Telegram -> LoreMotion end-to-end: pending integration test on the Android/Termux runtime

## Security

Remote planner actions do not expose arbitrary `EVALUATE`; page evaluation remains an internal Browser Worker primitive. Turnstile is not bypassed.
