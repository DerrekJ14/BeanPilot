package com.beanpilot.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beanpilot.data.remote.MachineConnectionStatus
import com.beanpilot.data.remote.GaggiMateShotSample
import com.beanpilot.domain.recommendation.BrewCoach
import com.beanpilot.domain.recommendation.BrewRecommendation
import com.beanpilot.domain.model.ShotRecord
import com.beanpilot.domain.model.CoffeeBean
import com.beanpilot.domain.model.TelemetrySnapshot
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var address by rememberSaveable { mutableStateOf(uiState.machineAddress) }
    var selectedShotId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCoffeeLibrary by rememberSaveable { mutableStateOf(false) }
    var showShotHistory by rememberSaveable { mutableStateOf(false) }
    var showProfileManager by rememberSaveable { mutableStateOf(false) }
    var showDashboardSettings by rememberSaveable { mutableStateOf(false) }
    var widgetEditMode by rememberSaveable { mutableStateOf(false) }
    var selectedCoffeeBeanId by rememberSaveable { mutableStateOf<Long?>(null) }
    var coffeeSelectionInitialized by rememberSaveable { mutableStateOf(false) }
    var connectionExpanded by rememberSaveable {
        mutableStateOf(uiState.connection.status != MachineConnectionStatus.Connected)
    }
    val liveTelemetry = remember { mutableStateListOf<TelemetrySnapshot>() }
    val selectedCoffee = uiState.coffeeBeans.firstOrNull { it.id == selectedCoffeeBeanId }
    val recommendation = remember(uiState.recentShots, selectedCoffee) {
        BrewCoach.recommend(uiState.recentShots, selectedCoffee)
    }

    LaunchedEffect(uiState.telemetry.timestamp) {
        val snapshot = uiState.telemetry
        if (snapshot.timestamp != null && liveTelemetry.lastOrNull()?.timestamp != snapshot.timestamp) {
            liveTelemetry.add(snapshot)
            if (liveTelemetry.size > MAX_LIVE_TELEMETRY_SAMPLES) {
                liveTelemetry.removeAt(0)
            }
        }
    }

    LaunchedEffect(showProfileManager) {
        if (showProfileManager) viewModel.loadProfiles()
    }
    LaunchedEffect(uiState.coffeeBeans, uiState.recentShots) {
        if (!coffeeSelectionInitialized && uiState.coffeeBeans.isNotEmpty()) {
            selectedCoffeeBeanId = uiState.recentShots
                .firstNotNullOfOrNull { shot -> shot.coffeeBeanId?.takeIf { id ->
                    uiState.coffeeBeans.any { it.id == id }
                } }
                ?: uiState.coffeeBeans.first().id
            coffeeSelectionInitialized = true
        }
    }
    LaunchedEffect(uiState.connection.status) {
        if (uiState.connection.status == MachineConnectionStatus.Connected) {
            connectionExpanded = false
        } else {
            connectionExpanded = true
        }
    }

    val profileEditorOpen = showProfileManager && uiState.profileUiState.editingProfile != null
    val canNavigateBack = profileEditorOpen ||
        showDashboardSettings ||
        selectedShotId != null ||
        showProfileManager ||
        showCoffeeLibrary ||
        showShotHistory ||
        widgetEditMode ||
        (uiState.connection.status == MachineConnectionStatus.Connected && connectionExpanded)
    BackHandler(enabled = canNavigateBack) {
        when {
            profileEditorOpen -> viewModel.closeProfileEditor()
            showDashboardSettings -> showDashboardSettings = false
            selectedShotId != null -> selectedShotId = null
            showProfileManager -> showProfileManager = false
            showCoffeeLibrary -> showCoffeeLibrary = false
            showShotHistory -> showShotHistory = false
            widgetEditMode -> widgetEditMode = false
            uiState.connection.status == MachineConnectionStatus.Connected && connectionExpanded ->
                connectionExpanded = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            showCoffeeLibrary -> "Coffee Library"
                            selectedShotId != null -> "Shot Review"
                            showShotHistory -> "Shot History"
                            showProfileManager -> "GaggiMate Profiles"
                            showDashboardSettings -> "Saved Dashboards"
                            else -> "BEANPILOT"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        },
        bottomBar = {
            if (
                uiState.connection.status == MachineConnectionStatus.Connected &&
                selectedShotId == null &&
                !showDashboardSettings &&
                !profileEditorOpen
            ) {
                NavigationBar {
                    NavigationBarItem(
                        selected = !showCoffeeLibrary && !showShotHistory && !showProfileManager,
                        onClick = {
                            showCoffeeLibrary = false
                            showShotHistory = false
                            showProfileManager = false
                        },
                        icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                        label = { Text("Station") },
                        alwaysShowLabel = false,
                    )
                    NavigationBarItem(
                        selected = showCoffeeLibrary,
                        onClick = {
                            showCoffeeLibrary = true
                            showShotHistory = false
                            showProfileManager = false
                        },
                        icon = { Icon(Icons.Filled.Coffee, contentDescription = null) },
                        label = { Text("Beans") },
                        alwaysShowLabel = false,
                    )
                    NavigationBarItem(
                        selected = showShotHistory,
                        onClick = {
                            showCoffeeLibrary = false
                            showShotHistory = true
                            showProfileManager = false
                        },
                        icon = { Icon(Icons.Filled.History, contentDescription = null) },
                        label = { Text("Shots") },
                        alwaysShowLabel = false,
                    )
                    NavigationBarItem(
                        selected = showProfileManager,
                        onClick = {
                            showCoffeeLibrary = false
                            showShotHistory = false
                            showProfileManager = true
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Profiles") },
                        alwaysShowLabel = false,
                    )
                }
            }
        },
    ) { paddingValues ->
        val selectedShot = uiState.recentShots.firstOrNull { it.id == selectedShotId }
        if (showCoffeeLibrary) {
            CoffeeLibraryScreen(
                coffees = uiState.coffeeBeans,
                shots = uiState.recentShots,
                selectedCoffeeId = selectedCoffeeBeanId,
                saveMessage = uiState.coffeeSaveMessage,
                onSave = viewModel::saveCoffeeBean,
                onDelete = viewModel::deleteCoffeeBean,
                onUseForCoach = { coffee ->
                    selectedCoffeeBeanId = coffee.id
                    coffeeSelectionInitialized = true
                    showCoffeeLibrary = false
                },
                modifier = Modifier.padding(paddingValues),
            )
        } else if (showDashboardSettings) {
            SavedDashboardLayoutsScreen(
                layouts = uiState.profileUiState.savedWidgetLayouts,
                selectedLayoutId = uiState.profileUiState.selectedWidgetLayoutId,
                message = uiState.profileUiState.widgetLayoutMessage,
                onSelect = viewModel::selectWidgetLayout,
                onSaveAs = viewModel::saveWidgetLayoutAs,
                onDelete = viewModel::deleteWidgetLayout,
                onBack = { showDashboardSettings = false },
                modifier = Modifier.padding(paddingValues),
            )
        } else if (selectedShot != null) {
            ShotReviewScreen(
                shot = selectedShot,
                coffees = uiState.coffeeBeans,
                detailState = uiState.shotDetail,
                saveMessage = uiState.reviewSaveMessage,
                onBack = { selectedShotId = null },
                onSave = viewModel::saveShotReview,
                modifier = Modifier.padding(paddingValues),
            )
        } else if (showShotHistory) {
            ShotHistoryScreen(
                shots = uiState.recentShots,
                onSelect = { shot ->
                    selectedShotId = shot.id
                    viewModel.loadShotDetails(shot)
                },
                modifier = Modifier.padding(paddingValues),
            )
        } else if (showProfileManager) {
            val editor = uiState.profileUiState.editingProfile
            if (editor == null) {
                ProfileManagerScreen(
                    state = uiState.profileUiState,
                    onReload = viewModel::loadProfiles,
                    onSelect = viewModel::selectProfile,
                    onEdit = viewModel::loadProfileForEditing,
                    onImport = viewModel::importProfile,
                    onBack = { showProfileManager = false },
                    modifier = Modifier.padding(paddingValues),
                )
            } else {
                ProfileEditorScreen(
                    profile = editor,
                    busy = uiState.profileUiState.loading,
                    message = uiState.profileUiState.message,
                    onSave = viewModel::saveProfile,
                    onCancel = viewModel::closeProfileEditor,
                    modifier = Modifier.padding(paddingValues),
                )
            }
    } else {
        if (uiState.connection.status != MachineConnectionStatus.Connected) {
            ConnectionLoginScreen(
                address = address,
                onAddressChange = { address = it },
                onConnect = { viewModel.connectToMachine(address) },
                onDisconnect = viewModel::disconnectFromMachine,
                uiState = uiState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        } else {
            val activeLayout = uiState.profileUiState.savedWidgetLayouts
                .firstOrNull { it.id == uiState.profileUiState.selectedWidgetLayoutId }
                ?: SavedDashboardLayout.default()
            val widgets = activeLayout.widgets.filterNot { it in activeLayout.hiddenWidgets }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ConnectionStatusBar(
                        uiState = uiState,
                        expanded = connectionExpanded,
                        onExpand = { connectionExpanded = !connectionExpanded },
                        onAddressChange = { address = it },
                        address = address,
                        onConnect = { viewModel.connectToMachine(address) },
                        onDisconnect = viewModel::disconnectFromMachine,
                        onSyncHistory = viewModel::syncShotHistory,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { widgetEditMode = !widgetEditMode },
                                modifier = Modifier.semantics {
                                    contentDescription = if (widgetEditMode) "Done editing widgets" else "Edit widgets"
                                },
                            ) {
                                Icon(
                                    if (widgetEditMode) Icons.Filled.Done else Icons.Filled.Edit,
                                    contentDescription = null,
                                )
                            }
                            IconButton(
                                onClick = { showDashboardSettings = true },
                                modifier = Modifier.semantics { contentDescription = "Saved layouts" },
                            ) {
                                Icon(Icons.Filled.Dashboard, contentDescription = null)
                            }
                        }
                    }
                }
                if (widgetEditMode) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        WidgetEditorPanel(
                            layout = activeLayout,
                            onMove = viewModel::moveWidget,
                            onVisibilityChange = viewModel::setWidgetVisible,
                            onSizeChange = viewModel::setWidgetSize,
                        )
                    }
                }
                if (widgets.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("No widgets are visible. Use Edit to add widgets to this layout.")
                    }
                }
                items(
                    items = widgets,
                    key = { it.name },
                    span = { widget ->
                        val fullWidth = activeLayout.sizes[widget] != DashboardWidgetSize.Compact
                        GridItemSpan(if (fullWidth) maxLineSpan else 1)
                    },
                ) { widget ->
                    when (widget) {
                        DashboardWidget.MachineControls -> Unit
                        DashboardWidget.MachineStatus -> MachineStatusCard(
                            uiState,
                            compact = activeLayout.sizes[widget] == DashboardWidgetSize.Compact,
                        )
                        DashboardWidget.LiveTelemetry -> LiveTelemetryChart(
                            points = liveTelemetry,
                            compact = activeLayout.sizes[widget] == DashboardWidgetSize.Compact,
                        )
                        DashboardWidget.BrewCoach -> BrewCoachCard(
                            recommendation,
                            coffees = uiState.coffeeBeans,
                            selectedCoffee = selectedCoffee,
                            onCoffeeSelected = { coffee ->
                                selectedCoffeeBeanId = coffee?.id
                                coffeeSelectionInitialized = true
                            },
                            compact = activeLayout.sizes[widget] == DashboardWidgetSize.Compact,
                        )
                        DashboardWidget.ShotHistory -> ShotHistoryWidget(
                            shots = uiState.recentShots,
                            compact = activeLayout.sizes[widget] == DashboardWidgetSize.Compact,
                            onSelect = { shot ->
                                selectedShotId = shot.id
                                viewModel.loadShotDetails(shot)
                            },
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun MachineStatusCard(uiState: DashboardUiState, compact: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("MACHINE", style = MaterialTheme.typography.labelLarge)
                val connected = uiState.connection.status == MachineConnectionStatus.Connected
                Text(
                    text = when (uiState.connection.status) {
                        MachineConnectionStatus.Disconnected -> "OFFLINE"
                        MachineConnectionStatus.Connecting -> "CONNECTING"
                        MachineConnectionStatus.Connected -> "ONLINE"
                        MachineConnectionStatus.Reconnecting -> "RECONNECTING"
                    },
                    color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Thermostat, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Text(
                    text = uiState.telemetry.machineState ?: "Waiting for machine telemetry",
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                "PROFILE  ${uiState.telemetry.activeProfile ?: "—"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun BrewCoachCard(
    recommendation: BrewRecommendation,
    coffees: List<CoffeeBean>,
    selectedCoffee: CoffeeBean?,
    onCoffeeSelected: (CoffeeBean?) -> Unit,
    compact: Boolean = false,
) {
    var coffeeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("BREW COACH", style = MaterialTheme.typography.labelLarge)
                Text(
                    recommendation.confidence,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Box {
                TextButton(onClick = { coffeeMenuExpanded = true }) {
                    Text(selectedCoffee?.let { "${it.beanName} · ${it.roaster}" } ?: "All coffees")
                }
                DropdownMenu(
                    expanded = coffeeMenuExpanded,
                    onDismissRequest = { coffeeMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("All coffees") },
                        onClick = {
                            onCoffeeSelected(null)
                            coffeeMenuExpanded = false
                        },
                    )
                    coffees.forEach { coffee ->
                        DropdownMenuItem(
                            text = { Text("${coffee.beanName} · ${coffee.roaster}") },
                            onClick = {
                                onCoffeeSelected(coffee)
                                coffeeMenuExpanded = false
                            },
                        )
                    }
                }
            }
            Text(
                recommendation.title,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            )
            Text(
                recommendation.detail,
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            )
            if (!compact) {
                Text(
                    recommendation.basis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ConnectionLoginScreen(
    address: String,
    onAddressChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    uiState: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("CONNECT MACHINE", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("BeanPilot", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Connect to your GaggiMate machine by entering its host or IP address.",
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ConnectionCard(
            address = address,
            onAddressChange = onAddressChange,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
            onSyncHistory = {},
            uiState = uiState,
        )
        Text(
            "Your machine address is stored on this device.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConnectionStatusBar(
    uiState: DashboardUiState,
    expanded: Boolean,
    onExpand: () -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSyncHistory: () -> Unit,
) {
    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("MACHINE CONNECTION", style = MaterialTheme.typography.labelLarge)
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.semantics { contentDescription = "Minimize connection settings" },
                    ) {
                        Icon(Icons.Filled.ExpandLess, contentDescription = null)
                    }
                }
            }
            ConnectionCard(
                address = address,
                onAddressChange = onAddressChange,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onSyncHistory = onSyncHistory,
                uiState = uiState,
            )
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("MACHINE ONLINE", style = MaterialTheme.typography.labelLarge)
                    Text(
                        uiState.connection.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSyncHistory,
                        modifier = Modifier.semantics { contentDescription = "Sync shot history" },
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null)
                    }
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.semantics { contentDescription = "Connection settings" },
                    ) {
                        Icon(Icons.Filled.ExpandMore, contentDescription = null)
                    }
                    IconButton(
                        onClick = onDisconnect,
                        modifier = Modifier.semantics { contentDescription = "Disconnect machine" },
                    ) {
                        Icon(Icons.Filled.LinkOff, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetEditorPanel(
    layout: SavedDashboardLayout,
    onMove: (DashboardWidget, Int) -> Unit,
    onVisibilityChange: (DashboardWidget, Boolean) -> Unit,
    onSizeChange: (DashboardWidget, DashboardWidgetSize) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("CUSTOMIZE WIDGETS", style = MaterialTheme.typography.labelLarge)
            Text(
                "Use the arrows to reorder. Hide widgets you do not need; compact widgets use half-width cards.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            layout.widgets.forEachIndexed { index, widget ->
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(widget.title(), style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row {
                            IconButton(
                                onClick = { onMove(widget, -1) },
                                enabled = index > 0,
                                modifier = Modifier.semantics {
                                    contentDescription = "Move ${widget.title()} up"
                                },
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
                            }
                            IconButton(
                                onClick = { onMove(widget, 1) },
                                enabled = index < layout.widgets.lastIndex,
                                modifier = Modifier.semantics {
                                    contentDescription = "Move ${widget.title()} down"
                                },
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                            }
                            IconButton(
                                onClick = { onVisibilityChange(widget, widget in layout.hiddenWidgets) },
                                modifier = Modifier.semantics {
                                    contentDescription = if (widget in layout.hiddenWidgets) {
                                        "Show ${widget.title()}"
                                    } else {
                                        "Hide ${widget.title()}"
                                    }
                                },
                            ) {
                                Icon(
                                    if (widget in layout.hiddenWidgets) {
                                        Icons.Filled.Visibility
                                    } else {
                                        Icons.Filled.VisibilityOff
                                    },
                                    contentDescription = null,
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val next = if (layout.sizes[widget] == DashboardWidgetSize.Compact) {
                                    DashboardWidgetSize.Full
                                } else {
                                    DashboardWidgetSize.Compact
                                }
                                onSizeChange(widget, next)
                            },
                            modifier = Modifier.semantics {
                                contentDescription = if (layout.sizes[widget] == DashboardWidgetSize.Compact) {
                                    "Make ${widget.title()} full width"
                                } else {
                                    "Make ${widget.title()} half width"
                                }
                            },
                        ) {
                            Icon(
                                if (layout.sizes[widget] == DashboardWidgetSize.Compact) {
                                    Icons.Filled.UnfoldMore
                                } else {
                                    Icons.Filled.UnfoldLess
                                },
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShotHistoryWidget(
    shots: List<ShotRecord>,
    compact: Boolean,
    onSelect: (ShotRecord) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(if (compact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        ) {
            Text(
                "SHOT HISTORY",
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
            )
            if (shots.isEmpty()) {
                Text(
                    "Sync machine history to see extractions here.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                shots.take(if (compact) 2 else 4).forEach { shot ->
                    HorizontalDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .then(
                                Modifier.clickable { onSelect(shot) },
                            ),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            "${shot.doseOutGr?.let { "${it.oneDecimal()} g" } ?: "Yield —"} · " +
                                "${(shot.brewTimeMs / 1_000.0).oneDecimal()} s",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (!compact) {
                            Text(
                                "${shot.profileUsed.ifBlank { "Unknown profile" }} · " +
                                    (shot.rating?.let { "Rating $it/5" } ?: "Not rated"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun DashboardWidget.title(): String =
    when (this) {
        DashboardWidget.MachineStatus -> "Machine status"
        DashboardWidget.MachineControls -> "Legacy widget"
        DashboardWidget.LiveTelemetry -> "Live telemetry"
        DashboardWidget.BrewCoach -> "Brew Coach"
        DashboardWidget.ShotHistory -> "Shot history"
    }

@Composable
private fun ConnectionCard(
    address: String,
    onAddressChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSyncHistory: () -> Unit,
    uiState: DashboardUiState,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "GaggiMate connection",
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = address,
                onValueChange = onAddressChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Host or IP address") },
                placeholder = { Text("gaggimate.local") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                supportingText = { Text("Example: 192.168.1.50 or gaggimate.local") },
            )
            Text(
                text = when (uiState.connection.status) {
                    MachineConnectionStatus.Disconnected -> "Disconnected"
                    MachineConnectionStatus.Connecting -> "Connecting…"
                    MachineConnectionStatus.Connected -> "Connected"
                    MachineConnectionStatus.Reconnecting -> "Connection lost; retrying…"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            uiState.addressError?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
            uiState.connection.error?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
            uiState.historySyncMessage?.let { message ->
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onConnect,
                    enabled = uiState.connection.status != MachineConnectionStatus.Connecting,
                ) {
                    Text(
                        if (uiState.connection.status == MachineConnectionStatus.Connecting) {
                            "Connecting…"
                        } else {
                            "Connect"
                        },
                    )
                }
                if (
                    uiState.connection.status == MachineConnectionStatus.Connected ||
                    uiState.connection.status == MachineConnectionStatus.Reconnecting
                ) {
                    TextButton(onClick = onDisconnect) {
                        Text("Disconnect")
                    }
                }
                TextButton(
                    onClick = onSyncHistory,
                    enabled = uiState.connection.status == MachineConnectionStatus.Connected,
                ) {
                    Text("Sync shots")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 10.dp else 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun ShotSummaryCard(shot: ShotRecord, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(shot.timestamp)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val doseIn = shot.doseInGr?.let { "${it.oneDecimal()}g in / " } ?: ""
            val doseOut = shot.doseOutGr?.let { "${it.oneDecimal()}g out" } ?: "Output weight unavailable"
            Text(
                text = "$doseIn$doseOut",
                style = MaterialTheme.typography.titleMedium,
            )
            shot.coffeeName?.let { coffee ->
                Text(text = coffee, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "Time: ${shot.brewTimeMs / 1_000.0}s · Profile: ${shot.profileUsed.ifBlank { "Unknown" }}",
                style = MaterialTheme.typography.bodyMedium,
            )
            shot.rating?.let { rating ->
                Text(
                    text = "Rating: $rating/5",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (shot.incomplete) {
                Text(
                    text = "Incomplete shot",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onClick) {
                Text("Review shot")
            }
        }
    }
}

@Composable
private fun ShotReviewScreen(
    shot: ShotRecord,
    coffees: List<CoffeeBean>,
    detailState: ShotDetailUiState,
    saveMessage: String?,
    onBack: () -> Unit,
    onSave: (ShotRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    var coffeeName by rememberSaveable(shot.id) { mutableStateOf(shot.coffeeName.orEmpty()) }
    var doseIn by rememberSaveable(shot.id) { mutableStateOf(shot.doseInGr?.toString().orEmpty()) }
    var doseOut by rememberSaveable(shot.id) { mutableStateOf(shot.doseOutGr?.toString().orEmpty()) }
    var grinderSetting by rememberSaveable(shot.id) {
        mutableStateOf(shot.grinderSetting?.toString().orEmpty())
    }
    var coffeeBeanId by rememberSaveable(shot.id) { mutableStateOf(shot.coffeeBeanId) }
    var coffeeMenuExpanded by rememberSaveable(shot.id) { mutableStateOf(false) }
    var rating by rememberSaveable(shot.id) { mutableStateOf(shot.rating) }
    var balanceTaste by rememberSaveable(shot.id) {
        mutableStateOf(shot.balanceTaste.toTasteBalanceScore())
    }
    var bodyRating by rememberSaveable(shot.id) { mutableStateOf(shot.bodyRating) }
    val shownMetrics = remember(shot.id) {
        mutableStateListOf("pressure", "temperature", "flow", "measuredWeight", "estimatedWeight")
    }
    var individualMetric by rememberSaveable(shot.id) { mutableStateOf<String?>(null) }
    var notes by rememberSaveable(shot.id) { mutableStateOf(shot.notes.orEmpty()) }
    var validationError by rememberSaveable(shot.id) { mutableStateOf<String?>(null) }
    var reviewEdited by rememberSaveable(shot.id) { mutableStateOf(false) }
    val numberFormat = "%.1f".format(Locale.getDefault(), shot.brewTimeMs / 1_000.0)

    LaunchedEffect(shot.id, detailState.notes, detailState.notesLoaded) {
        if (!reviewEdited) {
            detailState.notes?.let { remoteNotes ->
                coffeeName = remoteNotes.beanType ?: coffeeName
                doseIn = remoteNotes.doseIn ?: doseIn
                doseOut = remoteNotes.doseOut ?: doseOut
                grinderSetting = remoteNotes.grindSetting ?: grinderSetting
                rating = remoteNotes.rating?.takeIf { it in 1..5 } ?: rating
                balanceTaste = remoteNotes.balanceTaste.toTasteBalanceScore() ?: balanceTaste
                notes = remoteNotes.notes ?: notes
            }
        }
    }
    LaunchedEffect(shot.id, shot.coffeeBeanId) {
        if (shot.coffeeBeanId != null) coffeeBeanId = shot.coffeeBeanId
    }
    val selectedCoffee = coffees.firstOrNull { it.id == coffeeBeanId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back to dashboard") }
        Text("Shot review", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "${DateFormat.getDateTimeInstance().format(Date(shot.timestamp))} · " +
                "$numberFormat s · ${shot.profileUsed.ifBlank { "Unknown profile" }}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (shot.incomplete) {
            Text("This shot was marked incomplete.", color = MaterialTheme.colorScheme.error)
        }

        Text("Rating", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { value ->
                FilterChip(
                    selected = rating == value,
                    onClick = {
                        reviewEdited = true
                        rating = value
                    },
                    label = { Text("$value") },
                )
            }
            FilterChip(
                selected = rating == null,
                onClick = {
                    reviewEdited = true
                    rating = null
                },
                label = { Text("—") },
            )
        }
        Box {
            TextButton(onClick = { coffeeMenuExpanded = true }) {
                Text(selectedCoffee?.let { "${it.beanName} · ${it.roaster}" } ?: "Link a coffee")
            }
            DropdownMenu(
                expanded = coffeeMenuExpanded,
                onDismissRequest = { coffeeMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("No linked coffee") },
                    onClick = {
                        reviewEdited = true
                        coffeeBeanId = null
                        coffeeName = ""
                        coffeeMenuExpanded = false
                    },
                )
                coffees.forEach { coffee ->
                    DropdownMenuItem(
                        text = { Text("${coffee.beanName} · ${coffee.roaster}") },
                        onClick = {
                            reviewEdited = true
                            coffeeBeanId = coffee.id
                            coffeeName = coffee.beanName
                            coffeeMenuExpanded = false
                        },
                    )
                }
            }
        }
        if (selectedCoffee == null) {
            OutlinedTextField(
                value = coffeeName,
                onValueChange = {
                    reviewEdited = true
                    coffeeName = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Coffee name (not linked)") },
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReviewNumberField(
                value = doseIn,
                onValueChange = {
                    reviewEdited = true
                    doseIn = it
                },
                label = "Dose in (g)",
                modifier = Modifier.weight(1f),
            )
            ReviewNumberField(
                value = doseOut,
                onValueChange = {
                    reviewEdited = true
                    doseOut = it
                },
                label = "Dose out (g)",
                modifier = Modifier.weight(1f),
            )
            ReviewNumberField(
                value = grinderSetting,
                onValueChange = {
                    reviewEdited = true
                    grinderSetting = it
                },
                label = "Grinder",
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = {
                reviewEdited = true
                notes = it.take(MAX_GAGGIMATE_NOTE_LENGTH)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Taste and shot notes (${notes.length}/$MAX_GAGGIMATE_NOTE_LENGTH)") },
            minLines = 3,
        )
        Text("Taste balance", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("BITTER", style = MaterialTheme.typography.labelSmall)
            Text(
                balanceTaste?.toString() ?: "—",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("SOUR", style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = (balanceTaste ?: 5).toFloat(),
            onValueChange = {
                reviewEdited = true
                balanceTaste = it.roundToInt()
            },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.semantics {
                contentDescription = "Taste balance from bitter to sour, ${balanceTaste ?: "not rated"} of 10"
            },
        )
        TextButton(
            onClick = {
                reviewEdited = true
                balanceTaste = null
            },
            enabled = balanceTaste != null,
        ) { Text("Clear taste balance") }
        Text("Body", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("LIGHT", style = MaterialTheme.typography.labelSmall)
            Text(
                bodyRating?.toString() ?: "—",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text("FULL", style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = (bodyRating ?: 5).toFloat(),
            onValueChange = {
                reviewEdited = true
                bodyRating = it.roundToInt()
            },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.semantics {
                contentDescription = "Body rating from light to full, ${bodyRating ?: "not rated"} of 10"
            },
        )
        TextButton(
            onClick = {
                reviewEdited = true
                bodyRating = null
            },
            enabled = bodyRating != null,
        ) { Text("Clear body rating") }
        Button(
            onClick = {
                val parsedDoseIn = doseIn.toOptionalDouble()
                val parsedDoseOut = doseOut.toOptionalDouble()
                val parsedGrinderSetting = grinderSetting.toOptionalDouble()
                if (
                    (doseIn.isNotBlank() && parsedDoseIn == null) ||
                    (doseOut.isNotBlank() && parsedDoseOut == null) ||
                    (grinderSetting.isNotBlank() && parsedGrinderSetting == null)
                ) {
                    validationError = "Enter valid numbers for dose and grinder setting."
                } else {
                    validationError = null
                    val ratio = if (parsedDoseIn != null && parsedDoseOut != null && parsedDoseIn > 0) {
                        parsedDoseOut / parsedDoseIn
                    } else {
                        null
                    }
                    onSave(
                        shot.copy(
                            coffeeName = selectedCoffee?.beanName ?: coffeeName.trim(),
                            coffeeBeanId = selectedCoffee?.id,
                            doseInGr = parsedDoseIn,
                            doseOutGr = parsedDoseOut,
                            grinderSetting = parsedGrinderSetting,
                            rating = rating,
                            balanceTaste = balanceTaste?.toString(),
                            bodyRating = bodyRating,
                            ratio = ratio,
                            notes = notes.trim(),
                        ),
                    )
                }
            },
        ) {
            Text("Save review")
        }
        validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        saveMessage?.let {
            Text(
                text = it,
                color = if (it.startsWith("Could not")) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }

        Text("Shot measurements", style = MaterialTheme.typography.titleLarge)
        when {
            detailState.loading -> Text("Loading detailed shot data from GaggiMate…")
            detailState.sourceId != shot.sourceId -> Text("Waiting for detailed shot data…")
            detailState.error != null -> Text(
                text = detailState.error,
                color = MaterialTheme.colorScheme.error,
            )
            detailState.samples.isEmpty() -> Text("No measurements are available for this shot.")
            else -> {
                val finalSample = detailState.samples.lastOrNull()
                val finalMeasuredWeight = detailState.samples
                    .lastOrNull { (it.measuredWeightGr ?: 0.0) > 0.0 }?.measuredWeightGr
                val finalEstimatedWeight = detailState.samples
                    .lastOrNull { it.estimatedWeightGr != null }?.estimatedWeightGr
                val estimatedPeak = detailState.samples.maxOfOrNull { it.estimatedWeightGr ?: 0.0 }
                val indexOutput = shot.doseOutGr
                Text(
                    "History output: ${indexOutput?.let { "${it.oneDecimal()} g" } ?: "not recorded"}\n" +
                        "Final measured scale reading: " +
                        (finalMeasuredWeight?.let { "${it.oneDecimal()} g" } ?: "no positive reading") +
                        "\nFinal estimated weight: " +
                        (finalEstimatedWeight?.let { "${it.oneDecimal()} g" } ?: "not recorded") +
                        (estimatedPeak?.let { " (estimated peak ${it.oneDecimal()} g)" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (
                    indexOutput != null &&
                    finalSample?.measuredWeightGr?.let {
                        it > 0.0 && abs(indexOutput - it) > maxOf(2.0, indexOutput * 0.05)
                    } == true
                ) {
                    Text(
                        "The final measured reading differs from the history output. Check tare and compare " +
                            "with GaggiMate Shot Analyzer before using it as beverage yield.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (
                    indexOutput != null &&
                    finalEstimatedWeight != null &&
                    abs(indexOutput - finalEstimatedWeight) > maxOf(2.0, indexOutput * 0.05)
                ) {
                    Text(
                        "The final estimate differs from the history output. The estimate is shown separately " +
                            "and is not treated as measured beverage yield.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                detailState.notesError?.let {
                    Text(
                        "GaggiMate notes could not be loaded: $it. Remote sync is paused to protect existing notes.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (detailState.incomplete) {
                    Text(
                        "The machine log is incomplete; showing the samples that were recorded.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                ShotMeasurementsChart(
                    samples = detailState.samples,
                    shownMetrics = shownMetrics,
                    individualMetric = individualMetric,
                    onIndividualMetricChange = { individualMetric = it },
                )
            }
        }
    }
}

@Composable
private fun ReviewNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun CoffeeLibraryScreen(
    coffees: List<CoffeeBean>,
    shots: List<ShotRecord>,
    selectedCoffeeId: Long?,
    saveMessage: String?,
    onSave: (CoffeeBean) -> Unit,
    onDelete: (Long) -> Unit,
    onUseForCoach: (CoffeeBean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddCoffee by rememberSaveable { mutableStateOf(false) }
    var beanName by rememberSaveable { mutableStateOf("") }
    var roaster by rememberSaveable { mutableStateOf("") }
    var origin by rememberSaveable { mutableStateOf("") }
    var process by rememberSaveable { mutableStateOf("") }
    var roastDate by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingDelete = coffees.firstOrNull { it.id == pendingDeleteId }

    pendingDelete?.let { bean ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Remove ${bean.beanName}?") },
            text = { Text("Linked shots will be unlinked, but their saved coffee name will remain.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(bean.id)
                        pendingDeleteId = null
                    },
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "BEAN LIBRARY",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("Your coffees", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Choose a coffee to focus shot comparisons and Brew Coach recommendations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (coffees.isEmpty()) {
            Text("No coffees added yet. Add a bean to start building its shot history.")
        } else {
            coffees.forEach { coffee ->
                val beanShots = shots.filter { shot ->
                    shot.coffeeBeanId == coffee.id ||
                        (shot.coffeeBeanId == null &&
                            shot.coffeeName?.trim()?.equals(coffee.beanName.trim(), ignoreCase = true) == true)
                }
                val ratings = beanShots.mapNotNull { it.rating }
                val averageRating = ratings.takeIf { it.isNotEmpty() }?.average()
                val latestShot = beanShots.maxByOrNull { it.timestamp }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(coffee.beanName, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    coffee.roaster,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            FilterChip(
                                selected = selectedCoffeeId == coffee.id,
                                onClick = { onUseForCoach(coffee) },
                                label = { Text(if (selectedCoffeeId == coffee.id) "ACTIVE" else "FOCUS") },
                            )
                        }
                        HorizontalDivider()
                        val details = listOfNotNull(
                            coffee.origin?.takeIf(String::isNotBlank),
                            coffee.process?.takeIf(String::isNotBlank),
                            coffee.roastDate?.takeIf(String::isNotBlank),
                        )
                        if (details.isNotEmpty()) {
                            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "${beanShots.size} recent shots · " +
                                    (averageRating?.let { "Avg ${it.oneDecimal()}/5" } ?: "Not rated"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            latestShot?.let {
                                Text(
                                    "LAST ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(it.timestamp))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        coffee.notes?.takeIf(String::isNotBlank)?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { pendingDeleteId = coffee.id }) {
                            Text("Remove from library")
                        }
                    }
                }
            }
        }
        Button(
            onClick = {
                showAddCoffee = !showAddCoffee
                validationError = null
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (showAddCoffee) "Cancel adding coffee" else "Add coffee")
        }
        if (showAddCoffee) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("New coffee", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = beanName,
                        onValueChange = { beanName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Bean name *") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = roaster,
                        onValueChange = { roaster = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Roaster *") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Origin") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = process,
                        onValueChange = { process = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Process") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = roastDate,
                        onValueChange = { roastDate = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Roast date") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Coffee notes") },
                        minLines = 2,
                    )
                    Button(
                        onClick = {
                            if (beanName.isBlank() || roaster.isBlank()) {
                                validationError = "Bean name and roaster are required."
                            } else {
                                validationError = null
                                onSave(
                                    CoffeeBean(
                                        beanName = beanName,
                                        roaster = roaster,
                                        origin = origin,
                                        process = process,
                                        roastDate = roastDate,
                                        notes = notes,
                                    ),
                                )
                                beanName = ""
                                roaster = ""
                                origin = ""
                                process = ""
                                roastDate = ""
                                notes = ""
                                showAddCoffee = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save coffee")
                    }
                }
            }
        }
        validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        saveMessage?.let {
            Text(
                it,
                color = if (it == "Coffee saved.") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}

@Composable
private fun ShotHistoryScreen(
    shots: List<ShotRecord>,
    onSelect: (ShotRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("EXTRACTION LOG", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Shot history", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${shots.size} recent shots · Select an extraction to review its data and tasting notes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (shots.isEmpty()) {
            Text("Sync machine history to see extractions here.")
        } else {
            shots.forEach { shot ->
                ShotSummaryCard(shot = shot) { onSelect(shot) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShotMeasurementsChart(
    samples: List<GaggiMateShotSample>,
    shownMetrics: MutableList<String>,
    individualMetric: String?,
    onIndividualMetricChange: (String?) -> Unit,
) {
    val metrics = listOf(
        ShotMetric(
            id = "pressure",
            label = "Pressure",
            shortLabel = "PRESS",
            unit = "bar",
            values = samples.mapNotNull { sample -> sample.pressureBar?.let { sample.timeMs to it } },
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Filled.Speed,
        ),
        ShotMetric(
            id = "temperature",
            label = "Temperature",
            shortLabel = "TEMP",
            unit = "°C",
            values = samples.mapNotNull { sample -> sample.temperatureC?.let { sample.timeMs to it } },
            color = MaterialTheme.colorScheme.tertiary,
            icon = Icons.Filled.Thermostat,
        ),
        ShotMetric(
            id = "flow",
            label = "Flow",
            shortLabel = "FLOW",
            unit = "ml/s",
            values = samples.mapNotNull { sample -> sample.flowMlPerSec?.let { sample.timeMs to it } },
            color = MaterialTheme.colorScheme.secondary,
            icon = Icons.Filled.WaterDrop,
        ),
        ShotMetric(
            id = "measuredWeight",
            label = "Measured weight",
            shortLabel = "MEAS",
            unit = "g",
            values = samples.mapNotNull { sample -> sample.measuredWeightGr?.let { sample.timeMs to it } },
            color = MaterialTheme.colorScheme.error,
            icon = Icons.Filled.Scale,
        ),
        ShotMetric(
            id = "estimatedWeight",
            label = "Estimated weight",
            shortLabel = "EST",
            unit = "g",
            values = samples.mapNotNull { sample -> sample.estimatedWeightGr?.let { sample.timeMs to it } },
            color = MaterialTheme.colorScheme.inversePrimary,
            icon = Icons.Filled.Scale,
        ),
    )
    val selectedIndividual = metrics.firstOrNull { it.id == individualMetric }
    if (selectedIndividual != null) {
        var menuExpanded by rememberSaveable(individualMetric) { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${selectedIndividual.label} graph", style = MaterialTheme.typography.titleMedium)
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Choose graph view"
                        },
                    ) {
                        Icon(Icons.Filled.ExpandMore, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Combined graph") },
                            onClick = {
                                onIndividualMetricChange(null)
                                menuExpanded = false
                            },
                        )
                        metrics.forEach { metric ->
                            DropdownMenuItem(
                                text = { Text("${metric.label} only") },
                                onClick = {
                                    onIndividualMetricChange(metric.id)
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            MetricTrendChart(
                title = selectedIndividual.label,
                unit = selectedIndividual.unit,
                points = selectedIndividual.values,
                color = selectedIndividual.color,
            )
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Combined measurements", style = MaterialTheme.typography.titleMedium)
                    Box {
                        var menuExpanded by rememberSaveable { mutableStateOf(false) }
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.semantics {
                                contentDescription = "Split measurements into individual graphs"
                            },
                        ) {
                            Icon(Icons.Filled.ExpandMore, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Combined graph") },
                                onClick = {
                                    onIndividualMetricChange(null)
                                    menuExpanded = false
                                },
                            )
                            metrics.forEach { metric ->
                                DropdownMenuItem(
                                    text = { Text("${metric.label} only") },
                                    onClick = {
                                        onIndividualMetricChange(metric.id)
                                        menuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                Text(
                    "Each line is normalized to its own range. Values and units are shown in the controls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    metrics.forEach { metric ->
                        FilterChip(
                            selected = metric.id in shownMetrics,
                            onClick = {
                                if (metric.id in shownMetrics) shownMetrics.remove(metric.id)
                                else shownMetrics.add(metric.id)
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "${metric.label} ${if (metric.id in shownMetrics) "visible" else "hidden"}"
                            },
                            label = { Text(metric.shortLabel) },
                            leadingIcon = {
                                Icon(metric.icon, contentDescription = null, tint = metric.color)
                            },
                        )
                    }
                }
                val visibleMetrics = metrics.filter { it.id in shownMetrics }
                Text(
                    visibleMetrics.joinToString(" · ") { metric ->
                        val range = metric.values.takeIf { it.isNotEmpty() }?.let { values ->
                            "${values.minOf { it.second }.oneDecimal()}–${values.maxOf { it.second }.oneDecimal()} ${metric.unit}"
                        } ?: "— ${metric.unit}"
                        "${metric.label}: $range"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (visibleMetrics.isEmpty()) {
                    Text("Turn on a measurement to show its trend.")
                } else {
                    Canvas(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                        val allTimes = visibleMetrics.flatMap { metric -> metric.values.map { it.first } }
                        if (allTimes.isEmpty()) return@Canvas
                        val firstTime = allTimes.minOrNull()!!.toFloat()
                        val timeSpan = (allTimes.maxOrNull()!! - allTimes.minOrNull()!!).toFloat()
                            .coerceAtLeast(1f)
                        visibleMetrics.forEach { metric ->
                            if (metric.values.size >= 2) {
                                val values = metric.values.map { it.second }
                                val minValue = values.minOrNull()!!.toFloat()
                                val valueSpan = (values.maxOrNull()!! - minValue).toFloat().coerceAtLeast(0.01f)
                                val path = Path()
                                metric.values.forEachIndexed { index, point ->
                                    val x = ((point.first.toFloat() - firstTime) / timeSpan) * size.width
                                    val y = size.height -
                                        ((point.second.toFloat() - minValue) / valueSpan) * size.height
                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path, metric.color, style = Stroke(width = 2.5.dp.toPx()))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${"%.1f".format(Locale.getDefault(), samples.first().timeMs / 1_000.0)} s",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            "${"%.1f".format(Locale.getDefault(), samples.last().timeMs / 1_000.0)} s",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

private data class ShotMetric(
    val id: String,
    val label: String,
    val shortLabel: String,
    val unit: String,
    val values: List<Pair<Long, Double>>,
    val color: androidx.compose.ui.graphics.Color,
    val icon: ImageVector,
)

@Composable
private fun MetricTrendChart(
    title: String,
    unit: String,
    points: List<Pair<Long, Double>>,
    color: androidx.compose.ui.graphics.Color,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (points.isEmpty()) {
                Text("Not recorded for this shot.", style = MaterialTheme.typography.bodySmall)
            } else {
                val range = points.minOf { it.second }..points.maxOf { it.second }
                Text(
                    text = "${"%.1f".format(Locale.getDefault(), range.start)}–" +
                        "${"%.1f".format(Locale.getDefault(), range.endInclusive)} $unit",
                    style = MaterialTheme.typography.bodySmall,
                )
                Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    if (points.size < 2) return@Canvas
                    val minTime = points.first().first.toFloat()
                    val timeSpan = (points.last().first - points.first().first).toFloat().coerceAtLeast(1f)
                    val minValue = points.minOf { it.second }.toFloat()
                    val valueSpan = (points.maxOf { it.second } - minValue).toFloat().coerceAtLeast(0.01f)
                    val line = Path()
                    points.forEachIndexed { index, point ->
                        val x = ((point.first.toFloat() - minTime) / timeSpan) * size.width
                        val y = size.height -
                            ((point.second.toFloat() - minValue) / valueSpan) * size.height
                        if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
                    }
                    drawLine(
                        color = color.copy(alpha = 0.18f),
                        start = androidx.compose.ui.geometry.Offset(0f, size.height - 1f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height - 1f),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawPath(line, color = color, style = Stroke(width = 2.dp.toPx()))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${"%.1f".format(Locale.getDefault(), points.first().first / 1_000.0)} s",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        "${"%.1f".format(Locale.getDefault(), points.last().first / 1_000.0)} s",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private fun String.toOptionalDouble(): Double? =
    trim().takeIf { it.isNotEmpty() }?.replace(',', '.')?.toDoubleOrNull()

private fun String?.toTasteBalanceScore(): Int? =
    when (this?.trim()?.lowercase(Locale.ROOT)) {
        "bitter" -> 2
        "balanced" -> 5
        "sour" -> 9
        else -> this?.toIntOrNull()?.takeIf { it in 1..10 }
    }

private const val MAX_GAGGIMATE_NOTE_LENGTH = 200

private fun Double.oneDecimal(): String =
    String.format(Locale.getDefault(), "%.1f", this)

private const val MAX_LIVE_TELEMETRY_SAMPLES = 120
