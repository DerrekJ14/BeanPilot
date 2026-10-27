# BeanPilot

BeanPilot is an Android app for monitoring and improving espresso extraction via a Wi‑Fi-connected GaggiMate Pro device.

## Current status

This repository now includes the initial project skeleton for the requested Android architecture:

- Gradle configuration for Android + Kotlin + Hilt + Jetpack Compose
- Clean Architecture package layout
- Room database entities and DAOs
- Ktor WebSocket client for GaggiMate
- Domain models for telemetry and espresso shots
- Dashboard screen with live telemetry metrics and recent-shot summary

## Required setup

1. Install Android Studio or the Android SDK.
2. Copy `local.properties.example` to `local.properties`.
3. Update `sdk.dir` to your local Android SDK installation path.
4. Run:

```bash
./gradlew assembleDebug
```

## Notes

The project is designed around the GaggiMate Pro WebSocket API and uses Jetpack Compose + Material 3 for the UI. The current implementation is a functional starter structure suitable for continuing development on the full espresso dashboard, coffee library, and AI coaching features.
