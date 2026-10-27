package com.beanpilot.domain.model

data class TelemetrySnapshot(
    val machineState: String? = null,
    val temperatureC: Double? = null,
    val pressureBar: Double? = null,
    val flowMlPerMin: Double? = null,
    val weightGr: Double? = null,
    val activeProfile: String? = null,
    val shotTimeSeconds: Double? = null,
    val timestamp: Long? = null,
)
