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
import com.beanpilot.data.remote.GaggiMateControlClient
import com.beanpilot.data.remote.GaggiMateProfile
import com.beanpilot.data.remote.GaggiMateProfileCodec
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val shotRepository: ShotRepository,
    private val coffeeBeanRepository: CoffeeBeanRepository,
    private val webSocketClient: GaggiMateWebSocketClient,
    private val historyClient: GaggiMateHistoryClient,
    private val controlClient: GaggiMateControlClient,
    private val machinePreferences: MachinePreferences,
    private val json: Json,
) : ViewModel() {
    private val machineAddress = MutableStateFlow(machinePreferences.getMachineAddress())
    private val historySyncMessage = MutableStateFlow<String?>(null)
    private val addressError = MutableStateFlow<String?>(null)
    val graphMeasurementColors = MutableStateFlow(machinePreferences.getGraphMeasurementColors())

    fun setGraphMeasurementColor(measurement: String, color: Int?) {
        machinePreferences.saveGraphMeasurementColor(measurement, color)
        graphMeasurementColors.value = machinePreferences.getGraphMeasurementColors()
    }

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
    private val profileState = MutableStateFlow(ProfileUiState())
    private val selectedLayout = MutableStateFlow(
        DashboardLayout.fromPreference(machinePreferences.getDashboardLayout()),
    )
    private val savedWidgetLayouts = MutableStateFlow(loadSavedWidgetLayouts())
    private val selectedWidgetLayoutId = MutableStateFlow(
        machinePreferences.getSelectedCustomDashboardLayout().takeIf { id ->
            savedWidgetLayouts.value.any { it.id == id }
        } ?: "default",
    )
    private val widgetLayoutMessage = MutableStateFlow<String?>(null)
    private val widgetLayoutState = combine(
        savedWidgetLayouts,
        selectedWidgetLayoutId,
        widgetLayoutMessage,
    ) { layouts, selectedId, message ->
        Triple(layouts, selectedId, message)
    }

    private val controlsAndLayout = combine(profileState, selectedLayout, widgetLayoutState) {
            profiles,
            layout,
            widgetLayouts,
        ->
        profiles.copy(
            layout = layout,
            savedWidgetLayouts = widgetLayouts.first,
            selectedWidgetLayoutId = widgetLayouts.second,
            widgetLayoutMessage = widgetLayouts.third,
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        dashboardState,
        shotDetail,
        reviewSaveMessage,
        coffeeSaveMessage,
        controlsAndLayout,
    ) { state, detail, saveMessage, coffeeMessage, controls ->
        state.copy(
            shotDetail = detail,
            reviewSaveMessage = saveMessage,
            coffeeSaveMessage = coffeeMessage,
            profileUiState = controls,
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
                    loadProfiles(connection.address)
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

    fun setDashboardLayout(layout: DashboardLayout) {
        machinePreferences.saveDashboardLayout(layout.preferenceValue)
        selectedLayout.value = layout
    }

    fun selectWidgetLayout(id: String) {
        if (savedWidgetLayouts.value.none { it.id == id }) {
            widgetLayoutMessage.value = "That saved layout is no longer available."
            return
        }
        selectedWidgetLayoutId.value = id
        machinePreferences.saveSelectedCustomDashboardLayout(id)
        widgetLayoutMessage.value = null
    }

    fun updateWidgetLayout(layout: SavedDashboardLayout) {
        val normalized = layout.normalized()
        val updated = savedWidgetLayouts.value.map { current ->
            if (current.id == normalized.id) normalized else current
        }
        if (updated.none { it.id == normalized.id }) return
        persistWidgetLayouts(updated)
    }

    fun saveWidgetLayoutAs(name: String): String? {
        val normalizedName = name.trim().take(SavedDashboardLayout.MAX_LAYOUT_NAME_LENGTH)
        if (normalizedName.isBlank()) return "Enter a name for this layout."
        val userLayouts = savedWidgetLayouts.value.filterNot { it.id == DEFAULT_WIDGET_LAYOUT_ID }
        if (userLayouts.size >= SavedDashboardLayout.MAX_SAVED_LAYOUTS) {
            return "You can save up to ${SavedDashboardLayout.MAX_SAVED_LAYOUTS} custom layouts."
        }
        if (savedWidgetLayouts.value.any { it.name.equals(normalizedName, ignoreCase = true) }) {
            return "A layout with that name already exists."
        }
        val current = currentWidgetLayout()
        val newLayout = current.copy(
            id = "custom-${System.currentTimeMillis()}",
            name = normalizedName,
        )
        persistWidgetLayouts(savedWidgetLayouts.value + newLayout)
        selectedWidgetLayoutId.value = newLayout.id
        machinePreferences.saveSelectedCustomDashboardLayout(newLayout.id)
        return null
    }

    fun deleteWidgetLayout(id: String) {
        if (id == DEFAULT_WIDGET_LAYOUT_ID) return
        val updated = savedWidgetLayouts.value.filterNot { it.id == id }
        if (updated.size == savedWidgetLayouts.value.size) return
        persistWidgetLayouts(updated)
        if (selectedWidgetLayoutId.value == id) {
            selectWidgetLayout(DEFAULT_WIDGET_LAYOUT_ID)
        }
    }

    fun moveWidget(widget: DashboardWidget, offset: Int) {
        val current = currentWidgetLayout()
        val fromIndex = current.widgets.indexOf(widget)
        val toIndex = (fromIndex + offset).coerceIn(0, current.widgets.lastIndex)
        if (fromIndex < 0 || fromIndex == toIndex) return
        val reordered = current.widgets.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        updateWidgetLayout(current.copy(widgets = reordered))
    }

    fun setWidgetVisible(widget: DashboardWidget, visible: Boolean) {
        val current = currentWidgetLayout()
        val hidden = current.hiddenWidgets.toMutableSet().apply {
            if (visible) remove(widget) else add(widget)
        }
        updateWidgetLayout(current.copy(hiddenWidgets = hidden))
    }

    fun setWidgetSize(widget: DashboardWidget, size: DashboardWidgetSize) {
        val current = currentWidgetLayout()
        updateWidgetLayout(current.copy(sizes = current.sizes + (widget to size)))
    }

    private fun currentWidgetLayout(): SavedDashboardLayout =
        savedWidgetLayouts.value.firstOrNull { it.id == selectedWidgetLayoutId.value }
            ?: savedWidgetLayouts.value.first()

    private fun persistWidgetLayouts(layouts: List<SavedDashboardLayout>) {
        val default = layouts.firstOrNull { it.id == DEFAULT_WIDGET_LAYOUT_ID }
            ?: SavedDashboardLayout.default()
        val custom = layouts
            .filterNot { it.id == DEFAULT_WIDGET_LAYOUT_ID }
            .take(SavedDashboardLayout.MAX_SAVED_LAYOUTS)
        val validLayouts = listOf(default.normalized()) + custom.map { it.normalized() }
        savedWidgetLayouts.value = validLayouts
        machinePreferences.saveSavedDashboardLayouts(json.encodeToString(validLayouts))
    }

    private fun loadSavedWidgetLayouts(): List<SavedDashboardLayout> {
        val stored = machinePreferences.getSavedDashboardLayouts()
            ?: return listOf(SavedDashboardLayout.default())
        return try {
            val decoded = json.decodeFromString<List<SavedDashboardLayout>>(stored)
            val default = decoded.firstOrNull { it.id == DEFAULT_WIDGET_LAYOUT_ID }
                ?: SavedDashboardLayout.default()
            val custom = decoded
                .filterNot { it.id == DEFAULT_WIDGET_LAYOUT_ID }
                .distinctBy { it.id }
                .take(SavedDashboardLayout.MAX_SAVED_LAYOUTS)
            (listOf(default.normalized()) + custom.map { it.normalized() })
                .also { machinePreferences.saveSavedDashboardLayouts(json.encodeToString(it)) }
        } catch (exception: SerializationException) {
            Log.e(TAG, "Could not read saved dashboard layouts", exception)
            listOf(SavedDashboardLayout.default())
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "Saved dashboard layouts contain invalid values", exception)
            listOf(SavedDashboardLayout.default())
        }
    }

    fun loadProfiles() {
        loadProfiles(machineAddress.value)
    }

    private fun loadProfiles(address: String) {
        if (address.isBlank()) return
        viewModelScope.launch {
            profileState.value = profileState.value.copy(loading = true, message = null)
            try {
                val profiles = controlClient.listProfiles(address)
                profileState.value = profileState.value.copy(
                    profiles = profiles,
                    loading = false,
                    message = "Loaded ${profiles.size} profiles.",
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Could not load GaggiMate profiles", exception)
                profileState.value = profileState.value.copy(
                    loading = false,
                    message = exception.message ?: "Could not load profiles from GaggiMate.",
                )
            }
        }
    }

    fun loadProfileForEditing(id: String) {
        val address = machineAddress.value
        viewModelScope.launch {
            profileState.value = profileState.value.copy(loading = true, message = null)
            try {
                val profile = controlClient.loadProfile(address, id)
                profileState.value = profileState.value.copy(
                    loading = false,
                    editingProfile = profile,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Could not load GaggiMate profile", exception)
                profileState.value = profileState.value.copy(
                    loading = false,
                    message = exception.message ?: "Could not load this profile.",
                )
            }
        }
    }

    fun importProfile(source: String) {
        try {
            val profile = GaggiMateProfileCodec.decode(json = json, source = source)
            profileState.value = profileState.value.copy(
                editingProfile = profile.copy(id = null, selected = null, favorite = false),
                message = null,
            )
        } catch (exception: SerializationException) {
            Log.e(TAG, "Could not import GaggiMate profile", exception)
            profileState.value = profileState.value.copy(
                message = exception.message ?: "Could not import this profile JSON.",
            )
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "Invalid GaggiMate profile import", exception)
            profileState.value = profileState.value.copy(
                message = exception.message ?: "This profile contains invalid values.",
            )
        }
    }

    fun closeProfileEditor() {
        profileState.value = profileState.value.copy(editingProfile = null)
    }

    fun saveProfile(profile: GaggiMateProfile) {
        val address = machineAddress.value
        viewModelScope.launch {
            profileState.value = profileState.value.copy(loading = true, message = null)
            try {
                controlClient.saveProfile(address, profile)
                val profiles = controlClient.listProfiles(address)
                profileState.value = profileState.value.copy(
                    profiles = profiles,
                    editingProfile = null,
                    loading = false,
                    message = "Saved ${profile.label}.",
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Could not save GaggiMate profile", exception)
                profileState.value = profileState.value.copy(
                    loading = false,
                    message = exception.message ?: "Could not save this profile.",
                )
            }
        }
    }

    fun selectProfile(profile: GaggiMateProfile) {
        val id = profile.id ?: return
        val address = machineAddress.value
        viewModelScope.launch {
            profileState.value = profileState.value.copy(loading = true, message = null)
            try {
                controlClient.selectProfile(address, id)
                val profiles = controlClient.listProfiles(address)
                profileState.value = profileState.value.copy(
                    profiles = profiles,
                    loading = false,
                    message = "Selected ${profile.label}.",
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Could not select GaggiMate profile", exception)
                profileState.value = profileState.value.copy(
                    loading = false,
                    message = exception.message ?: "Could not select this profile.",
                )
            }
        }
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
        const val DEFAULT_WIDGET_LAYOUT_ID = "default"
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
    val profileUiState: ProfileUiState = ProfileUiState(),
)

data class ProfileUiState(
    val profiles: List<GaggiMateProfile> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null,
    val editingProfile: GaggiMateProfile? = null,
    val layout: DashboardLayout = DashboardLayout.BrewStation,
    val savedWidgetLayouts: List<SavedDashboardLayout> = listOf(SavedDashboardLayout.default()),
    val selectedWidgetLayoutId: String = "default",
    val widgetLayoutMessage: String? = null,
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
