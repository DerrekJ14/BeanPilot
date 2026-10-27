# BeanPilot

BeanPilot is an Android app for monitoring and improving espresso extraction via a Wi‑Fi-connected GaggiMate Pro device.

## Current status

This repository now includes the initial project skeleton for the requested Android architecture:

- Gradle configuration for Android + Kotlin + Hilt + Jetpack Compose
- Clean Architecture package layout
- Room database entities, DAOs, and a non-destructive schema migration
- Ktor WebSocket connection to the documented GaggiMate `/ws` endpoint
- Partial `evt:status` telemetry parsing with reconnect handling
- Automatic import and manual sync of the latest GaggiMate shot index
- Dashboard controls for configuring, connecting, and disconnecting from the machine

## Required setup

1. Install Android Studio or the Android SDK.
2. Copy `local.properties.example` to `local.properties`.
3. Update `sdk.dir` to your local Android SDK installation path.
4. Open BeanPilot on the same network as GaggiMate and enter its hostname or IP address (for example, `gaggimate.local` or `192.168.1.50`).
5. Run:

```bash
./gradlew assembleDebug
```

## Notes

BeanPilot uses the [GaggiMate WebSocket API](https://github.com/jniebuhr/gaggimate/blob/master/docs/websocket-api.yaml) and [shot history API](https://github.com/jniebuhr/gaggimate/blob/master/docs/shot-notes-api.md). Machine addresses are saved locally and reconnect automatically the next time the app opens.

The documented status events provide temperature, pressure, flow, machine state, and profile. They do not currently define a live scale-weight field, so the dashboard displays a dash until a supported field is available. Imported shot index entries include duration, output volume when recorded, rating, and profile; dose-in, grinder setting, and notes are not provided by the index and remain unset.

The connection and import flow has unit coverage, but still needs to be exercised against the user's physical GaggiMate device to confirm network reachability and firmware compatibility.
