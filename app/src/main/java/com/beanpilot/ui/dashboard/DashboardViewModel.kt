package com.beanpilot.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beanpilot.data.local.MachinePreferences
import com.beanpilot.data.remote.GaggiMateEndpoint
import com.beanpilot.data.remote.GaggiMateHistoryClient
import com.beanpilot.data.remote.GaggiMateWebSocketClient
import com.beanpilot.data.remote.MachineConnectionState
import com.beanpilot.data.remote.MachineConnectionStatus
import com.beanpilot.data.repository.ShotRepository
import com.beanpilot.domain.model.ShotRecord
import com.beanpilot.domain.model.TelemetrySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val shotRepository: ShotRepository,
    private val webSocketClient: GaggiMateWebSocketClient,
    private val historyClient: GaggiMateHistoryClient,
    private val machinePreferences: MachinePreferences,
) : ViewModel() {
    private val machineAddress = MutableStateFlow(machinePreferences.getMachineAddress())
    private val historySyncMessage = MutableStateFlow<String?>(null)
    private val addressError = MutableStateFlow<String?>(null)

    private val machineState = combine(
        webSocketClient.telemetry,
        webSocketClient.connection,
        shotRepository.recentShots(),
    ) { telemetry, connection, shots ->
        DashboardUiState(
            telemetry = telemetry,
            connection = connection,
            recentShots = shots,
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        machineState,
        machineAddress,
        historySyncMessage,
        addressError,
    ) { state, address, syncMessage, error ->
        state.copy(
            machineAddress = address,
            historySyncMessage = syncMessage,
            addressError = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(machineAddress = machineAddress.value),
    )

    private var autoSyncedAddress: String? = null

    init {
        viewModelScope.launch {
            webSocketClient.connection.collect { connection ->
                if (
                    connection.status == MachineConnectionStatus.Connected &&
                    connection.address != autoSyncedAddress
                ) {
                    autoSyncedAddress = connection.address
                    syncShotHistory(connection.address)
                }
            }
        }
        if (machineAddress.value.isNotBlank()) {
            connectToMachine(machineAddress.value)
        }
    }

    fun connectToMachine(address: String) {
        try {
            GaggiMateEndpoint.parse(address)
            machinePreferences.saveMachineAddress(address.trim())
            machineAddress.value = address.trim()
            addressError.value = null
            webSocketClient.connect(address)
        } catch (exception: IllegalArgumentException) {
            addressError.value = exception.message ?: "Enter a valid GaggiMate address."
        }
    }

    fun disconnectFromMachine() {
        webSocketClient.disconnect()
    }

    fun syncShotHistory() {
        val address = machineAddress.value
        if (address.isBlank()) {
            addressError.value = "Enter the GaggiMate host before syncing shot history."
            return
        }
        syncShotHistory(address)
    }

    private fun syncShotHistory(address: String) {
        viewModelScope.launch {
            historySyncMessage.value = "Syncing shot history…"
            try {
                val shots = historyClient.fetchRecentShots(address)
                shotRepository.importMachineShots(shots)
                historySyncMessage.value = "Synced ${shots.size} shots."
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Could not sync GaggiMate shot history", exception)
                historySyncMessage.value =
                    exception.message ?: "Could not sync shot history from GaggiMate."
            }
        }
    }

    private companion object {
        const val TAG = "BeanPilotDashboard"
    }
}

data class DashboardUiState(
    val telemetry: TelemetrySnapshot = TelemetrySnapshot(),
    val connection: MachineConnectionState = MachineConnectionState(),
    val recentShots: List<ShotRecord> = emptyList(),
    val machineAddress: String = "",
    val historySyncMessage: String? = null,
    val addressError: String? = null,
)
