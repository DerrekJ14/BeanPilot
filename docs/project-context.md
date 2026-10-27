BeanPilot Project Context

I am building an Android application called BeanPilot for my espresso machine.

Hardware
Gaggia Classic Pro E24
GaggiMate Pro
Wi-Fi enabled
Bluetooth scale support enabled through GaggiMate
Goal

Create an Android application that:

Connects to GaggiMate Pro over Wi-Fi.
Reads machine telemetry in real time.
Imports shot history automatically.
Tracks coffees, grinders, and shot performance.
Uses AI/rules-based recommendations to improve espresso extraction.
Eventually learns from my own shot history and provides personalized recommendations.
Known GaggiMate Capabilities

GaggiMate Pro provides:

Web UI
WebSocket API
Shot history API
Brew profiles
Pressure profiling
Flow profiling
Temperature telemetry
Weight tracking
MQTT/Home Assistant integration

The preferred integration method is:

Plain Text
Android App
↓
WebSocket
↓
GaggiMate Pro

Not Bluetooth.

Technology Stack
Kotlin
Jetpack Compose
Material 3
Room Database
Hilt
Ktor WebSocket Client
Coroutines
Navigation Compose
Proposed App Architecture
Plain Text
ui/
domain/
data/
telemetry/
ai/
websocket/
MVP
Dashboard

Display:

Temperature
Pressure
Flow
Weight
Machine State
Active Profile
Shot History

Store:

Dose In
Dose Out
Brew Time
Ratio
Profile Used
Grinder Setting
User Rating
Notes
Coffee Library

Store:

Roaster
Bean Name
Origin
Process
Roast Date
AI Coach V1

Rules-based engine:

Examples:

Shot too fast → grind finer
Shot too slow → grind coarser
Target best extraction window based on prior highest-rated shots
Immediate Goal

Generate:

Android project structure
Gradle configuration
Room entities
Hilt setup
WebSocket client for GaggiMate
Telemetry models
Dashboard screen

Assume Clean Architecture and MVVM.