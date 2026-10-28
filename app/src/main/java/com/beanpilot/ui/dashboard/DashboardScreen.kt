package com.beanpilot.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beanpilot.data.remote.MachineConnectionStatus
import com.beanpilot.domain.model.ShotRecord
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var address by rememberSaveable { mutableStateOf(uiState.machineAddress) }
    var selectedShotId by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BeanPilot Dashboard") },
            )
        },
    ) { paddingValues ->
        val selectedShot = uiState.recentShots.firstOrNull { it.id == selectedShotId }
        if (selectedShot != null) {
            ShotReviewScreen(
                shot = selectedShot,
                detailState = uiState.shotDetail,
                saveMessage = uiState.reviewSaveMessage,
                onBack = { selectedShotId = null },
                onSave = viewModel::saveShotReview,
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            item {
                ConnectionCard(
                    address = address,
                    onAddressChange = { address = it },
                    onConnect = { viewModel.connectToMachine(address) },
                    onDisconnect = viewModel::disconnectFromMachine,
                    onSyncHistory = viewModel::syncShotHistory,
                    uiState = uiState,
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Machine status",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Thermostat,
                                contentDescription = null,
                            )
                            Text(
                                text = uiState.telemetry.machineState ?: "Waiting for telemetry",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Text(
                            text = "Profile: ${uiState.telemetry.activeProfile ?: "—"}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricCard(
                        title = "Temperature",
                        value = uiState.telemetry.temperatureC?.let { "${it.oneDecimal()}°C" } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        title = "Pressure",
                        value = uiState.telemetry.pressureBar?.let { "${it.oneDecimal()} bar" } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricCard(
                        title = "Flow",
                        value = uiState.telemetry.flowMlPerMin?.let { "${it.oneDecimal()} ml/min" } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        title = "Scale weight",
                        value = uiState.telemetry.weightGr?.let { "${it.oneDecimal()} g" } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Text(
                    text = "Recent shots",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            if (uiState.recentShots.isEmpty()) {
                item {
                    Text(
                        text = "Connect to GaggiMate to import shot history.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(uiState.recentShots, key = { it.id }) { shot ->
                    ShotSummaryCard(
                        shot = shot,
                        onClick = {
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
                Button(onClick = onConnect) {
                    Text("Connect")
                }
                TextButton(onClick = onDisconnect) {
                    Text("Disconnect")
                }
                TextButton(onClick = onSyncHistory) {
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
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
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
    var rating by rememberSaveable(shot.id) { mutableStateOf(shot.rating) }
    var balanceTaste by rememberSaveable(shot.id) { mutableStateOf(shot.balanceTaste) }
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
                balanceTaste = remoteNotes.balanceTaste ?: balanceTaste
                notes = remoteNotes.notes ?: notes
            }
        }
    }

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
        OutlinedTextField(
            value = coffeeName,
            onValueChange = {
                reviewEdited = true
                coffeeName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Coffee") },
            singleLine = true,
        )
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("bitter", "balanced", "sour").forEach { taste ->
                FilterChip(
                    selected = balanceTaste == taste,
                    onClick = {
                        reviewEdited = true
                        balanceTaste = if (balanceTaste == taste) null else taste
                    },
                    label = { Text(taste.replaceFirstChar(Char::uppercase)) },
                )
            }
        }
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
                            coffeeName = coffeeName.trim().ifEmpty { null },
                            doseInGr = parsedDoseIn,
                            doseOutGr = parsedDoseOut,
                            grinderSetting = parsedGrinderSetting,
                            rating = rating,
                            balanceTaste = balanceTaste,
                            ratio = ratio,
                            notes = notes.trim().ifEmpty { null },
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
                MetricTrendChart(
                    title = "Pressure",
                    unit = "bar",
                    points = detailState.samples.mapNotNull { it.pressureBar?.let { value -> it.timeMs to value } },
                    color = MaterialTheme.colorScheme.primary,
                )
                MetricTrendChart(
                    title = "Temperature",
                    unit = "°C",
                    points = detailState.samples.mapNotNull {
                        it.temperatureC?.let { value -> it.timeMs to value }
                    },
                    color = MaterialTheme.colorScheme.tertiary,
                )
                MetricTrendChart(
                    title = "Flow",
                    unit = "ml/s",
                    points = detailState.samples.mapNotNull {
                        it.flowMlPerSec?.let { value -> it.timeMs to value }
                    },
                    color = MaterialTheme.colorScheme.secondary,
                )
                MetricTrendChart(
                    title = "Measured scale weight",
                    unit = "g",
                    points = detailState.samples.mapNotNull {
                        it.measuredWeightGr?.let { value -> it.timeMs to value }
                    },
                    color = MaterialTheme.colorScheme.error,
                )
                MetricTrendChart(
                    title = "Estimated weight",
                    unit = "g",
                    points = detailState.samples.mapNotNull {
                        it.estimatedWeightGr?.let { value -> it.timeMs to value }
                    },
                    color = MaterialTheme.colorScheme.error,
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

private const val MAX_GAGGIMATE_NOTE_LENGTH = 200

private fun Double.oneDecimal(): String =
    String.format(Locale.getDefault(), "%.1f", this)
