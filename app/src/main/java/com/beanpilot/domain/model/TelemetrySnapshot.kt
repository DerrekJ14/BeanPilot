package com.beanpilot.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelemetrySnapshot(
    @SerialName("machineState") val machineState: String = "Idle",
    @SerialName("temperatureC") val temperatureC: Double = 0.0,
    @SerialName("pressureBar") val pressureBar: Double = 0.0,
    @SerialName("flowMlPerMin") val flowMlPerMin: Double = 0.0,
    @SerialName("weightGr") val weightGr: Double = 0.0,
    @SerialName("activeProfile") val activeProfile: String = "Classic",
    @SerialName("shotTimeSeconds") val shotTimeSeconds: Double = 0.0,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
)
