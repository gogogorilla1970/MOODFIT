# MOODFIT — AI Makeover Studio

MOODFIT is an Android concept app for identity-preserving AI makeovers.

## v0.1.0

The first build contains:

- two-image workflow: **YOU** + **LOOK**
- Android image selection
- visual transfer controls for outfit, hair, make-up, pose and background
- identity / look / creativity sliders
- optional prompt
- modular `MakeoverProvider` interface for later AI backends
- modern rose / mauve / plum visual system
- GitHub Actions workflow that creates a debug APK

## AI backend

The v0.1.0 UI intentionally ships without a hard-coded third-party API key. A real implementation can plug into `MakeoverProvider`, for example with a self-hosted ComfyUI workflow using FLUX + PuLID/InstantID/IP-Adapter or a specialized virtual try-on model.

## Build

GitHub Actions will build the APK automatically.

Local build with Android SDK installed:

```bash
gradle :app:assembleDebug
```

APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Package

`com.moodfit.app`
