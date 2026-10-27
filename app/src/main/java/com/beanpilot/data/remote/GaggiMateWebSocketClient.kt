package com.beanpilot.data.remote

import android.util.Log
import com.beanpilot.domain.model.TelemetrySnapshot
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Singleton
class GaggiMateWebSocketClient @Inject constructor(
    private val httpClient: HttpClient,
    json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = GaggiMateStatusParser(json)
    private val _telemetry = MutableStateFlow(TelemetrySnapshot())
    private val _connection = MutableStateFlow(MachineConnectionState())
    private var connectionJob: Job? = null

    val telemetry: StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()
    val connection: StateFlow<MachineConnectionState> = _connection.asStateFlow()

    @Synchronized
    fun connect(address: String) {
        val endpoint = GaggiMateEndpoint.parse(address)
        connectionJob?.cancel()
        connectionJob = scope.launch {
            var retryDelayMs = INITIAL_RETRY_DELAY_MS
            while (isActive) {
                _connection.value = MachineConnectionState(
                    status = MachineConnectionStatus.Connecting,
                    address = address.trim(),
                )
                try {
                    val session = httpClient.webSocketSession {
                        url(endpoint.webSocketUrl)
                    }
                    retryDelayMs = INITIAL_RETRY_DELAY_MS
                    _connection.value = MachineConnectionState(
                        status = MachineConnectionStatus.Connected,
                        address = address.trim(),
                    )

                    for (frame in session.incoming) {
                        val text = (frame as? Frame.Text)?.readText() ?: continue
                        try {
                            val updated = parser.apply(text, _telemetry.value)
                            if (updated != null) {
                                _telemetry.value = updated
                                _connection.value = _connection.value.copy(error = null)
                            }
                        } catch (exception: Exception) {
                            Log.e(TAG, "Could not parse GaggiMate status event", exception)
                            _connection.value = _connection.value.copy(
                                error = "Received an invalid status message from GaggiMate.",
                            )
                        }
                    }
                    session.close()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.e(TAG, "GaggiMate WebSocket connection failed", exception)
                    _connection.value = MachineConnectionState(
                        status = MachineConnectionStatus.Reconnecting,
                        address = address.trim(),
                        error = exception.message ?: "Connection failed.",
                    )
                }

                if (isActive) {
                    _connection.value = _connection.value.copy(
                        status = MachineConnectionStatus.Reconnecting,
                    )
                    delay(retryDelayMs)
                    retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                }
            }
        }
    }

    @Synchronized
    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        _connection.value = MachineConnectionState()
    }

    private companion object {
        const val TAG = "GaggiMateWebSocket"
        const val INITIAL_RETRY_DELAY_MS = 1_000L
        const val MAX_RETRY_DELAY_MS = 30_000L
    }
}

enum class MachineConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
}

data class MachineConnectionState(
    val status: MachineConnectionStatus = MachineConnectionStatus.Disconnected,
    val address: String = "",
    val error: String? = null,
)
