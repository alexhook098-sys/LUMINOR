# ASTRA + SIRIUS Browser/LoreMotion integration

This staging tree combines the existing ASTRA Browser Agent with a small SIRIUS
Browser Bridge. The Android app remains the distributed SIRIUS control plane;
Chromium/CDP stays in the proven Termux/Node environment.

## Flow

Telegram -> ASTRA -> Browser Agent -> Chromium/CDP -> LoreMotion
-> manual Turnstile when required -> MP4 -> ASTRA -> Telegram

SIRIUS provides the Android-side bridge/health endpoint. It does **not** bypass
Cloudflare/Turnstile and does not execute arbitrary JavaScript from remote tasks.

## Included

- existing Browser Agent v5 building blocks
- LoreMotion adapter for prompt + 9:16 + generation/result detection
- Android `SiriusBrowserBridge`
- no secrets or API tokens

## Runtime configuration

Browser Agent CDP can be selected with environment variables:

- `ASTRA_BROWSER_HOST` (default `127.0.0.1`)
- `ASTRA_BROWSER_PORT` (default `9222`)
- `ASTRA_TURNSTILE_TIMEOUT_MS` (default `600000`, 10 minutes)
- `ASTRA_LORE_URL` (default `https://loremotion.com/generate/`)

For manual Turnstile, Chromium must be a user-visible X11 session, not a
headless-only session. ASTRA does not bypass or solve Turnstile automatically.
