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
- Per-shot `.slog` download and decoding for pressure, temperature, flow, and weight trends
- Local shot reviews for coffee, dose, grinder setting, rating, and tasting notes
- A local coffee library linked to shot reviews
- GaggiMate profile browsing, selection, editing, and JSON import
- Remote process start and hold-to-flush controls
- A connection-first screen that collapses to a machine status bar after connecting
- Customizable widget dashboards with reorder, visibility, and compact/full-width controls
- Up to three named dashboard layouts saved locally

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

BeanPilot uses the [GaggiMate WebSocket API](https://github.com/jniebuhr/gaggimate/blob/master/docs/websocket-api.yaml) and [shot history API](https://github.com/jniebuhr/gaggimate/blob/master/docs/shot-notes-api.md). Machine addresses are saved locally and reconnect automatically the next time the app opens. When disconnected, BeanPilot opens on its machine connection screen; after connecting, the connection form collapses into a compact status bar that can be expanded from the dashboard.

The documented status events provide temperature, pressure, flow, machine state, and profile. They do not currently define a live scale-weight field, so the dashboard displays a dash until a supported field is available. Imported shot index entries include duration, output volume when recorded, rating, and profile; dose-in, grinder setting, and notes are not provided by the index and remain unset until entered in a shot review.

Selecting **Review shot** fetches the individual `/api/history/<six-digit-id>.slog` log and plots measured scale weight separately from estimated weight. It compares the final measured and estimated readings with the shot-index output and flags material discrepancies instead of treating estimates as measured beverage yield. Coffee, dose, grinder setting, rating, taste balance, and notes are saved locally and synced to GaggiMate through its WebSocket notes API. Existing remote note fields are fetched and merged before save to avoid erasing fields BeanPilot does not edit. Local values remain intact during later history imports.

Add coffees in **Coffee library** and link each to a shot review; coffee metadata and shot links are stored locally. The GaggiMate `beanType` field is updated with the linked bean name on review sync.

Open **Profiles** to list profiles stored on GaggiMate, select one, load it into the multi-phase editor, or import a profile JSON file from the device. The editor validates pressure/flow pump targets, phase timings, transitions, valve state, temperature, and stop conditions before saving to the machine. Profile JSON follows the [GaggiMate profile schema](https://github.com/jniebuhr/gaggimate/blob/master/schema/profile.json).

**Start process** sends GaggiMate's documented current-mode activation command. If the machine requests warning confirmation, BeanPilot displays the warnings and requires an explicit confirmation. The API does not document a separate remote brew-stop command, so stop a running brew using the machine's normal stop control. **Press and hold to flush** sends the documented flush start/stop requests; stop-on-release behavior depends on the flush duration configured on GaggiMate. Separate Start flush and Stop flush buttons are available as well.

Use **Edit** on the connected dashboard to reorder widgets, show or hide them, and switch the machine status or Brew Coach card between half-width and full-width. **Layouts** lets you save the current configuration under a custom name, switch between layouts, or remove a saved layout. The app allows up to three custom layouts in addition to the default Brew Station layout. Widget layouts are stored on the device.

The connection, shot-index import, individual shot review, notes sync, profile-list retrieval, and profile loading have been verified against the physical GaggiMate device. Brew and flush actuation were not triggered during testing. Shot-log parsing, notes merging, and profile validation have unit coverage.
