package com.beanpilot.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beanpilot.data.local.MachinePreferences
import com.beanpilot.data.remote.GaggiMateEndpoint
import com.beanpilot.data.remote.GaggiMateHistoryClient
import com.beanpilot.data.remote.GaggiMateWebSocketClient
import com.beanpilot.data.remote.GaggiMateShotSample
import com.beanpilot.data.remote.GaggiMateShotNotes
import com.beanpilot.data.remote.mergeReview
import com.beanpilot.data.remote.MachineConnectionState
import com.beanpilot.data.remote.MachineConnectionStatus
import com.beanpilot.data.repository.ShotRepository
import com.beanpilot.data.repository.CoffeeBeanRepository
import com.beanpilot.domain.model.CoffeeBean
import com.beanpilot.domain.model.ShotRecord
import com.beanpilot.domain.model.TelemetrySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val shotRepository: ShotRepository,
    private val coffeeBeanRepository: CoffeeBeanRepository,
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
        coffeeBeanRepository.coffeeBeans(),
    ) { telemetry, connection, shots, coffeeBeans ->
        DashboardUiState(
            telemetry = telemetry,
            connection = connection,
            recentShots = shots,
            coffeeBeans = coffeeBeans,
        )
    }

    private val dashboardState = combine(
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
    }

    private val shotDetail = MutableStateFlow(ShotDetailUiState())
    private val reviewSaveMessage = MutableStateFlow<String?>(null)
    private val coffeeSaveMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        dashboardState,
        shotDetail,
        reviewSaveMessage,
        coffeeSaveMessage,
    ) { state, detail, saveMessage, coffeeMessage ->
        state.copy(
            shotDetail = detail,
            reviewSaveMessage = saveMessage,
            coffeeSaveMessage = coffeeMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(machineAddress = machineAddress.value),
    )

    private var autoSyncedAddress: String? = null
    private var shotDetailJob: kotlinx.coroutines.Job? = null

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

    fun loadShotDetails(shot: ShotRecord) {
        val sourceId = shot.sourceId
        if (sourceId == null) {
            shotDetail.value = ShotDetailUiState(
                sourceId = null,
                error = "Detailed shot data is available only for GaggiMate imports.",
            )
            return
        }

        shotDetailJob?.cancel()
        val address = machineAddress.value
        reviewSaveMessage.value = null
        shotDetail.value = ShotDetailUiState(sourceId = sourceId, loading = true)
        shotDetailJob = viewModelScope.launch {
            try {
                val detailsRequest = async { historyClient.fetchShotDetails(address, sourceId) }
                val notesRequest = async {
                    try {
                        historyClient.fetchShotNotes(address, sourceId) to null
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Log.e(TAG, "Could not load GaggiMate shot notes", exception)
                        null to (exception.message ?: "Could not load notes from GaggiMate.")
                    }
                }
                val details = detailsRequest.await()
                val (notes, notesError) = notesRequest.await()
                notes?.let { saveLocalNotes(shot, it) }
                shotDetail.value = ShotDetailUiState(
                    sourceId = sourceId,
                    samples = details.samples,
                    loading = false,
                    incomplete = details.incomplete,
                    notes = notes,
                    notesLoaded = notesError == null,
                    notesError = notesError,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Could not load GaggiMate shot details", exception)
                shotDetail.value = ShotDetailUiState(
                    sourceId = sourceId,
                    error = exception.message ?: "Could not load shot details from GaggiMate.",
                )
            }
        }
    }

    fun saveShotReview(shot: ShotRecord) {
        viewModelScope.launch {
            reviewSaveMessage.value = null
            try {
                shotRepository.updateShotReview(shot)
                if (shot.sourceId == null) {
                    reviewSaveMessage.value = "Review saved on this device."
                    return@launch
                }
                reviewSaveMessage.value = "Saved on this device; syncing notes to GaggiMate…"
                try {
                    val existingNotes = historyClient.fetchShotNotes(machineAddress.value, shot.sourceId)
                    val mergedNotes = (existingNotes ?: GaggiMateShotNotes()).mergeReview(
                        sourceId = shot.sourceId,
                        rating = shot.rating,
                        coffeeName = shot.coffeeName,
                        doseInGr = shot.doseInGr,
                        doseOutGr = shot.doseOutGr,
                        ratio = shot.ratio,
                        grinderSetting = shot.grinderSetting,
                        balanceTaste = shot.balanceTaste,
                        notes = shot.notes,
                    )
                    historyClient.saveShotNotes(machineAddress.value, shot.sourceId, mergedNotes)
                    reviewSaveMessage.value = "Review saved on this device and synced to GaggiMate."
                    syncShotHistory(machineAddress.value)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.e(TAG, "Could not sync shot review to GaggiMate", exception)
                    reviewSaveMessage.value =
                        "Review saved locally, but GaggiMate sync failed: " +
                            (exception.message ?: "unknown error")
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Could not save shot review", exception)
                reviewSaveMessage.value =
                    exception.message ?: "Could not save this shot review."
            }
        }
    }

    fun saveCoffeeBean(bean: CoffeeBean) {
        coffeeSaveMessage.value = null
        viewModelScope.launch {
            try {
                require(bean.beanName.isNotBlank()) { "Coffee name is required." }
                require(bean.roaster.isNotBlank()) { "Roaster is required." }
                coffeeBeanRepository.save(bean)
                coffeeSaveMessage.value = "Coffee saved."
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Could not save coffee", exception)
                coffeeSaveMessage.value = exception.message ?: "Could not save coffee."
            }
        }
    }

    fun deleteCoffeeBean(coffeeBeanId: Long) {
        viewModelScope.launch {
            try {
                shotRepository.unlinkCoffeeBean(coffeeBeanId)
                coffeeBeanRepository.delete(coffeeBeanId)
                coffeeSaveMessage.value = "Coffee removed; linked shots were unlinked."
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Could not remove coffee", exception)
                coffeeSaveMessage.value = exception.message ?: "Could not remove coffee."
            }
        }
    }

    private suspend fun saveLocalNotes(shot: ShotRecord, notes: GaggiMateShotNotes) {
        val bean = notes.beanType?.let { coffeeBeanRepository.findByName(it) }
        shotRepository.updateShotReview(
            shot.copy(
                coffeeName = notes.beanType ?: shot.coffeeName,
                coffeeBeanId = bean?.id ?: shot.coffeeBeanId,
                doseInGr = notes.doseIn?.toDoubleOrNull() ?: shot.doseInGr,
                doseOutGr = notes.doseOut?.toDoubleOrNull() ?: shot.doseOutGr,
                ratio = notes.ratio?.toDoubleOrNull() ?: shot.ratio,
                grinderSetting = notes.grindSetting?.toDoubleOrNull() ?: shot.grinderSetting,
                rating = notes.rating?.takeIf { it in 1..5 } ?: shot.rating,
                balanceTaste = notes.balanceTaste ?: shot.balanceTaste,
                notes = notes.notes,
            ),
        )
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
    val coffeeBeans: List<CoffeeBean> = emptyList(),
    val machineAddress: String = "",
    val historySyncMessage: String? = null,
    val addressError: String? = null,
    val shotDetail: ShotDetailUiState = ShotDetailUiState(),
    val reviewSaveMessage: String? = null,
    val coffeeSaveMessage: String? = null,
)

data class ShotDetailUiState(
    val sourceId: String? = null,
    val samples: List<GaggiMateShotSample> = emptyList(),
    val loading: Boolean = false,
    val incomplete: Boolean = false,
    val error: String? = null,
    val notes: GaggiMateShotNotes? = null,
    val notesLoaded: Boolean = false,
    val notesError: String? = null,
)
