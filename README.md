# MOODFIT — AI Makeover Studio

MOODFIT is an Android app for identity-preserving AI makeovers using a **YOU + LOOK** workflow.

## v0.2.0

Current development version:

- choose a **YOU** identity photo
- choose a **LOOK** reference photo
- transfer outfit, hair, make-up, pose and/or background
- add an optional free-text prompt
- choose between two AI providers:
  - **OpenAI GPT Image 2** via the official OpenAI Images Edit API
  - **fal.ai FLUX.2 Edit**
- display the generated result directly in the app
- API keys are entered at runtime and are not committed to the repository

### OpenAI mode

MOODFIT sends the LOOK image first and the YOU identity image second to `POST /v1/images/edits` with `gpt-image-2`. The app requests a portrait output at 1024×1536, medium quality, and saves the returned base64 image temporarily in the Android app cache for display.

### FLUX mode

The alternative provider uses fal.ai FLUX.2 Edit with the same two-reference concept.

## Security note

This is a personal/test build. Runtime key entry is safer than hard-coding a key in source, but a production Android app should normally call a private backend rather than expose a reusable provider API key on the device.

## Build

GitHub Actions builds the debug APK automatically.

Local build with Android SDK installed:

```bash
gradle :app:assembleDebug
```

APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Package

`com.moodfit.app`
