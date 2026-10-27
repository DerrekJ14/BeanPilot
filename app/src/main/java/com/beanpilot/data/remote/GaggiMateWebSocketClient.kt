package com.beanpilot.data.remote

import com.beanpilot.domain.model.TelemetrySnapshot
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Singleton
class GaggiMateWebSocketClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _telemetry = MutableStateFlow(TelemetrySnapshot())

    val telemetry: StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()

    fun connect(host: String) {
        val sanitizedHost = host.trim().removePrefix("ws://").removePrefix("wss://")
        if (sanitizedHost.isEmpty()) return

        scope.launch {
            try {
                httpClient.webSocketSession("ws://$sanitizedHost") {
                    for (frame in incoming) {
                        val text = (frame as? Frame.Text)?.readText() ?: continue
                        val snapshot = json.decodeFromString<TelemetrySnapshot>(text)
                        _telemetry.value = snapshot.copy(timestamp = System.currentTimeMillis())
                    }
                }
            } catch (_: Exception) {
                _telemetry.value = _telemetry.value.copy(machineState = "Disconnected")
            }
        }
    }
}
