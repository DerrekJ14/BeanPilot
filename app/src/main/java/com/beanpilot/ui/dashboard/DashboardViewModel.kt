package com.beanpilot.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beanpilot.data.remote.GaggiMateWebSocketClient
import com.beanpilot.data.repository.ShotRepository
import com.beanpilot.domain.model.ShotRecord
import com.beanpilot.domain.model.TelemetrySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val shotRepository: ShotRepository,
    private val webSocketClient: GaggiMateWebSocketClient,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        webSocketClient.telemetry,
        shotRepository.recentShots(),
    ) { telemetry, shots ->
        DashboardUiState(
            telemetry = telemetry,
            recentShots = shots,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    fun connectToMachine(host: String) {
        webSocketClient.connect(host)
    }
}

data class DashboardUiState(
    val telemetry: TelemetrySnapshot = TelemetrySnapshot(),
    val recentShots: List<ShotRecord> = emptyList(),
)
