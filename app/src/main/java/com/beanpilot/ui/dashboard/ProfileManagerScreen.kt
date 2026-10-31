package com.beanpilot.ui.dashboard

import android.net.Uri
import java.io.IOException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.beanpilot.data.remote.GaggiMateProfile
import com.beanpilot.data.remote.GaggiMateProfileCodec
import com.beanpilot.data.remote.GaggiMateProfilePhase
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Composable
fun ProfileManagerScreen(
    state: ProfileUiState,
    onReload: () -> Unit,
    onSelect: (GaggiMateProfile) -> Unit,
    onEdit: (String) -> Unit,
    onImport: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var importError by rememberSaveable { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val source = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: throw IllegalStateException("Could not read the selected profile file.")
                importError = null
                onImport(source)
            } catch (exception: IOException) {
                importError = exception.message ?: "Could not read this profile file."
            } catch (exception: SecurityException) {
                importError = "BeanPilot could not access the selected profile file."
            } catch (exception: IllegalStateException) {
                importError = exception.message ?: "Could not read this profile file."
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("GaggiMate Profiles", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text("Back") }
                TextButton(onClick = onReload, enabled = !state.loading) { Text("Refresh") }
            }
            Text(
                "Choose a profile to select for brewing, or load it into the editor. Import accepts a GaggiMate profile JSON file.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Import profile JSON")
            }
            state.loading.let { loading ->
                if (loading) Text("Communicating with GaggiMate…")
            }
            state.message?.let { message ->
                Text(
                    message,
                    color = if (message.startsWith("Could not") || message.contains("rejected")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            importError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.profiles.isEmpty()) {
                Text("No profiles loaded yet.")
            }
            state.profiles.forEach { profile ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(profile.label, style = MaterialTheme.typography.titleMedium)
                        Text("${profile.type} · ${profile.phases.size} phases")
                        ProfileCurveChart(
                            phases = profile.phases.map(ProfilePhaseDraft::from),
                            compact = true,
                        )
                        if (profile.description.isNotBlank()) Text(profile.description)
                        if (profile.selected == true) {
                            Text("Currently selected", color = MaterialTheme.colorScheme.primary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { onSelect(profile) },
                                enabled = !state.loading && profile.id != null,
                            ) { Text("Select") }
                            TextButton(
                                onClick = { profile.id?.let(onEdit) },
                                enabled = !state.loading && profile.id != null,
                            ) { Text("Load & edit") }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileEditorScreen(
    profile: GaggiMateProfile,
    busy: Boolean,
    message: String?,
    onSave: (GaggiMateProfile) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var label by remember(profile) { mutableStateOf(profile.label) }
    var type by remember(profile) { mutableStateOf(profile.type) }
    var description by remember(profile) { mutableStateOf(profile.description) }
    var temperature by remember(profile) { mutableStateOf(profile.temperature?.toString().orEmpty()) }
    var dose by remember(profile) { mutableStateOf(profile.dose?.toString().orEmpty()) }
    var favorite by remember(profile) { mutableStateOf(profile.favorite == true) }
    var utility by remember(profile) { mutableStateOf(profile.utility == true) }
    var phases by remember(profile) { mutableStateOf(profile.phases.map(ProfilePhaseDraft::from)) }
    var validationError by remember(profile) { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            Text(
                if (profile.id == null) "New Profile" else "Edit Profile",
                style = MaterialTheme.typography.headlineSmall,
            )
            TextButton(onClick = onCancel) { Text("Back to profiles") }
            Text(
                "GaggiMate runs these phases in order. Review pressure, flow, targets, and timing carefully before saving.",
                style = MaterialTheme.typography.bodyMedium,
            )
            ProfileCurveChart(
                phases = phases,
                compact = false,
                onPhaseChange = { index, phase ->
                    phases = phases.toMutableList().also { it[index] = phase }
                },
            )
            OutlinedTextField(label, { label = it }, Modifier.fillMaxWidth(), label = { Text("Profile name") })
            ProfileDropdown("Profile type", type, listOf("standard", "pro"), { type = it })
            OutlinedTextField(
                description,
                { description = it },
                Modifier.fillMaxWidth(),
                label = { Text("Description") },
                minLines = 2,
            )
            ProfileNumberField("Boiler temperature (°C)", temperature) { temperature = it }
            ProfileNumberField("Dose (g, optional)", dose) { dose = it }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Checkbox(checked = favorite, onCheckedChange = { favorite = it })
                Text("Favorite on GaggiMate")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Checkbox(checked = utility, onCheckedChange = { utility = it })
                Text("Utility profile (maintenance/flush routine)")
            }
            Text("Phases", style = MaterialTheme.typography.titleLarge)
            phases.forEachIndexed { index, phase ->
                ProfilePhaseEditor(
                    phase = phase,
                    index = index,
                    canRemove = phases.size > 1,
                    onChange = { updated -> phases = phases.toMutableList().also { it[index] = updated } },
                    onRemove = { phases = phases.toMutableList().also { it.removeAt(index) } },
                    onMoveUp = {
                        phases = phases.toMutableList().also {
                            val moved = it.removeAt(index)
                            it.add(index - 1, moved)
                        }
                    },
                    onMoveDown = {
                        phases = phases.toMutableList().also {
                            val moved = it.removeAt(index)
                            it.add(index + 1, moved)
                        }
                    },
                    canMoveUp = index > 0,
                    canMoveDown = index < phases.lastIndex,
                )
            }
            TextButton(
                onClick = { phases = phases + ProfilePhaseDraft.default(phases.size + 1) },
            ) { Text("Add phase") }
            validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    try {
                        val edited = GaggiMateProfile(
                            id = profile.id,
                            label = label.trim(),
                            type = type,
                            description = description.trim(),
                            temperature = temperature.toOptionalNumber("Profile temperature"),
                            dose = dose.toOptionalNumber("Dose"),
                            favorite = favorite,
                            selected = profile.selected,
                            utility = utility,
                            phases = phases.map(ProfilePhaseDraft::toProfilePhase),
                        )
                        GaggiMateProfileCodec.validate(edited)
                        validationError = null
                        onSave(edited)
                    } catch (exception: IllegalArgumentException) {
                        validationError = exception.message ?: "Profile contains invalid values."
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Saving…" else "Save to GaggiMate") }
            Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ProfilePhaseEditor(
    phase: ProfilePhaseDraft,
    index: Int,
    canRemove: Boolean,
    onChange: (ProfilePhaseDraft) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Phase ${index + 1}", style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(onClick = onMoveUp, enabled = canMoveUp) { Text("Up") }
                    TextButton(onClick = onMoveDown, enabled = canMoveDown) { Text("Down") }
                }
                if (canRemove) TextButton(onClick = onRemove) { Text("Remove") }
            }
            OutlinedTextField(
                phase.name,
                { onChange(phase.copy(name = it)) },
                Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
            )
            ProfileDropdown(
                "Phase type",
                phase.phase,
                listOf("preinfusion", "brew"),
            ) { onChange(phase.copy(phase = it)) }
            ProfileNumberField("Duration (seconds)", phase.duration) {
                onChange(phase.copy(duration = it))
            }
            ProfileNumberField("Temperature override (°C; 0 = profile default)", phase.temperature) {
                onChange(phase.copy(temperature = it))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Checkbox(checked = phase.valve == 1, onCheckedChange = {
                    onChange(phase.copy(valve = if (it) 1 else 0))
                })
                Text("Group valve open")
            }
            ProfileDropdown(
                "Pump control",
                phase.pumpMode,
                listOf("pressure", "flow", "fixed"),
            ) { onChange(phase.copy(pumpMode = it)) }
            if (phase.pumpMode == "fixed") {
                ProfileNumberField("Pump duty (%)", phase.pumpDuty) {
                    onChange(phase.copy(pumpDuty = it))
                }
            } else {
                ProfileNumberField("Pressure (bar; -1 holds current)", phase.pumpPressure) {
                    onChange(phase.copy(pumpPressure = it))
                }
                ProfileNumberField("Flow (g/s; -1 holds current)", phase.pumpFlow) {
                    onChange(phase.copy(pumpFlow = it))
                }
            }
            ProfileDropdown(
                "Ramp easing",
                phase.transitionType,
                listOf("instant", "linear", "ease-in", "ease-out", "ease-in-out"),
            ) { onChange(phase.copy(transitionType = it)) }
            ProfileNumberField("Ramp duration (seconds)", phase.transitionDuration) {
                onChange(phase.copy(transitionDuration = it))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Checkbox(checked = phase.adaptive, onCheckedChange = {
                    onChange(phase.copy(adaptive = it))
                })
                Text("Adaptive ramp starts from measured value")
            }
            Text("Stop targets", style = MaterialTheme.typography.titleSmall)
            phase.targets.forEachIndexed { targetIndex, target ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProfileDropdown(
                        "Target ${targetIndex + 1}",
                        target.type,
                        listOf("volumetric", "ratio", "pressure", "flow", "pumped"),
                    ) { selected ->
                        onChange(phase.updateTarget(targetIndex) { target.copy(type = selected) })
                    }
                    ProfileDropdown(
                        "Comparison",
                        target.operator,
                        listOf("gte", "lte"),
                    ) { selected ->
                        onChange(phase.updateTarget(targetIndex) { target.copy(operator = selected) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileNumberField("Target value", target.value, Modifier.weight(1f)) { value ->
                            onChange(phase.updateTarget(targetIndex) { target.copy(value = value) })
                        }
                        TextButton(onClick = {
                            onChange(phase.copy(targets = phase.targets.toMutableList().also { it.removeAt(targetIndex) }))
                        }) { Text("Remove") }
                    }
                }
            }
            TextButton(
                onClick = { onChange(phase.copy(targets = phase.targets + ProfileTargetDraft())) },
            ) { Text("Add stop target") }
        }
    }
}

@Composable
private fun ProfileCurveChart(
    phases: List<ProfilePhaseDraft>,
    compact: Boolean,
    onPhaseChange: ((Int, ProfilePhaseDraft) -> Unit)? = null,
) {
    val currentPhases = rememberUpdatedState(phases)
    val currentOnPhaseChange = rememberUpdatedState(onPhaseChange)
    val density = LocalDensity.current.density
    val pressureColor = MaterialTheme.colorScheme.primary
    val flowColor = MaterialTheme.colorScheme.tertiary
    val dutyColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val chartHeight = if (compact) 82.dp else 220.dp
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!compact) {
                Text("PUMP TARGET · DRAG POINTS TO EDIT", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Horizontal movement adjusts phase duration; vertical movement adjusts its pump target.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .then(
                        if (onPhaseChange == null) Modifier else Modifier.pointerInput(Unit) {
                            var draggingIndex: Int? = null
                            detectDragGestures(
                                onDragStart = { position ->
                                    val points = currentPhases.value.chartPoints(
                                        size.width.toFloat(),
                                        size.height.toFloat(),
                                        density,
                                        pressureColor,
                                        flowColor,
                                        dutyColor,
                                    )
                                    draggingIndex = points.indices.minByOrNull { index ->
                                        (points[index].position - position).getDistance()
                                    }?.takeIf { index ->
                                        (points[index].position - position).getDistance() <=
                                            48.dp.value * density
                                    }
                                },
                                onDragEnd = { draggingIndex = null },
                                onDragCancel = { draggingIndex = null },
                            ) { change, dragAmount ->
                                val index = draggingIndex
                                val latestPhases = currentPhases.value
                                val update = currentOnPhaseChange.value
                                if (index != null && update != null && index in latestPhases.indices) {
                                    val phase = latestPhases[index]
                                    val totalDuration = latestPhases.sumOf {
                                        it.duration.toDoubleOrNull() ?: 25.0
                                    }.coerceAtLeast(0.5)
                                    val usableWidth = (size.width - 32.dp.value * density).coerceAtLeast(1f)
                                    val usableHeight = (size.height - 32.dp.value * density).coerceAtLeast(1f)
                                    val durationDelta = dragAmount.x / usableWidth * totalDuration * 2.0
                                    val newDuration = (
                                        (phase.duration.toDoubleOrNull() ?: 25.0) + durationDelta
                                        ).coerceIn(0.5, 300.0)
                                    val maximum = phase.targetMaximum()
                                    val currentValue = phase.targetValue()
                                    val newTarget = (
                                        currentValue - dragAmount.y / usableHeight * maximum
                                        ).coerceIn(0.0, maximum)
                                    update(
                                        index,
                                        phase.copy(
                                            duration = newDuration.toOneDecimalString(),
                                            pumpPressure = if (phase.pumpMode == "pressure") {
                                                newTarget.toOneDecimalString()
                                            } else {
                                                phase.pumpPressure
                                            },
                                            pumpFlow = if (phase.pumpMode == "flow") {
                                                newTarget.toOneDecimalString()
                                            } else {
                                                phase.pumpFlow
                                            },
                                            pumpDuty = if (phase.pumpMode == "fixed") {
                                                newTarget.toInt().toString()
                                            } else {
                                                phase.pumpDuty
                                            },
                                        ),
                                    )
                                }
                                change.consume()
                            }
                        },
                    ),
            ) {
                val points = phases.chartPoints(
                    size.width,
                    size.height,
                    density,
                    pressureColor,
                    flowColor,
                    dutyColor,
                )
                val left = 16.dp.value * density
                val right = size.width - left
                val top = 12.dp.value * density
                val bottom = size.height - top
                repeat(4) { row ->
                    val y = top + (bottom - top) * row / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(left, y),
                        end = Offset(right, y),
                        strokeWidth = 1.dp.value * density,
                    )
                }
                repeat(5) { column ->
                    val x = left + (right - left) * column / 4f
                    drawLine(
                        color = gridColor.copy(alpha = 0.6f),
                        start = Offset(x, top),
                        end = Offset(x, bottom),
                        strokeWidth = 1.dp.value * density,
                    )
                }
                points.zipWithNext().forEach { (first, second) ->
                    if (first.mode == second.mode) {
                        drawLine(
                            color = first.color,
                            start = first.position,
                            end = second.position,
                            strokeWidth = (if (compact) 2.dp.value else 3.dp.value) * density,
                        )
                    }
                }
                points.forEach { point ->
                    drawCircle(
                        color = point.color.copy(alpha = 0.25f),
                        radius = (if (compact) 7.dp.value else 13.dp.value) * density,
                        center = point.position,
                    )
                    drawCircle(
                        color = point.color,
                        radius = (if (compact) 3.dp.value else 6.dp.value) * density,
                        center = point.position,
                    )
                }
            }
            if (!compact) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    phases.forEachIndexed { index, phase ->
                        val unit = when (phase.pumpMode) {
                            "flow" -> "g/s"
                            "fixed" -> "%"
                            else -> "bar"
                        }
                        Text(
                            "P${index + 1} ${phase.targetValue().toOneDecimalString()} $unit",
                            style = MaterialTheme.typography.labelSmall,
                            color = when (phase.pumpMode) {
                                "flow" -> MaterialTheme.colorScheme.tertiary
                                "fixed" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }
            }
        }
    }
}

private data class ProfileChartPoint(
    val position: Offset,
    val mode: String,
    val color: androidx.compose.ui.graphics.Color,
)

private fun List<ProfilePhaseDraft>.chartPoints(
    width: Float,
    height: Float,
    density: Float,
    pressureColor: androidx.compose.ui.graphics.Color,
    flowColor: androidx.compose.ui.graphics.Color,
    dutyColor: androidx.compose.ui.graphics.Color,
): List<ProfileChartPoint> {
    if (isEmpty()) return emptyList()
    val paddingX = 16.dp.value * density
    val paddingY = 12.dp.value * density
    val usableWidth = (width - paddingX * 2).coerceAtLeast(1f)
    val usableHeight = (height - paddingY * 2).coerceAtLeast(1f)
    val totalDuration = sumOf { it.duration.toDoubleOrNull() ?: 25.0 }.coerceAtLeast(0.5)
    var elapsed = 0.0
    return map { phase ->
        val duration = (phase.duration.toDoubleOrNull() ?: 25.0).coerceAtLeast(0.5)
        val x = paddingX + ((elapsed + duration / 2.0) / totalDuration * usableWidth).toFloat()
        val y = paddingY + (1f - (phase.targetValue() / phase.targetMaximum()).toFloat()) * usableHeight
        elapsed += duration
        ProfileChartPoint(
            position = Offset(x, y.coerceIn(paddingY, height - paddingY)),
            mode = phase.pumpMode,
            color = when (phase.pumpMode) {
                "flow" -> flowColor
                "fixed" -> dutyColor
                else -> pressureColor
            },
        )
    }
}

private fun ProfilePhaseDraft.targetValue(): Double =
    when (pumpMode) {
        "flow" -> pumpFlow.toDoubleOrNull() ?: 0.0
        "fixed" -> pumpDuty.toDoubleOrNull() ?: 0.0
        else -> pumpPressure.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }

private fun ProfilePhaseDraft.targetMaximum(): Double =
    when (pumpMode) {
        "flow" -> 15.0
        "fixed" -> 100.0
        else -> 12.0
    }

private fun Double.toOneDecimalString(): String =
    java.math.BigDecimal.valueOf(this).setScale(1, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros().toPlainString()

@Composable
private fun ProfileDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember(label, value) { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = true }) { Text("$label: $value") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ProfileNumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

private data class ProfilePhaseDraft(
    val name: String,
    val phase: String,
    val valve: Int,
    val duration: String,
    val temperature: String,
    val pumpMode: String,
    val pumpPressure: String,
    val pumpFlow: String,
    val pumpDuty: String,
    val transitionType: String,
    val transitionDuration: String,
    val adaptive: Boolean,
    val targets: List<ProfileTargetDraft>,
) {
    fun updateTarget(index: Int, transform: (ProfileTargetDraft) -> ProfileTargetDraft) =
        copy(targets = targets.toMutableList().also { it[index] = transform(it[index]) })

    fun toProfilePhase(): GaggiMateProfilePhase {
        val pump: JsonElement = if (pumpMode == "fixed") {
            JsonPrimitive(pumpDuty.toIntRequired("Pump duty"))
        } else {
            buildJsonObject {
                put("target", pumpMode)
                put("pressure", pumpPressure.toNumberRequired("Pressure"))
                put("flow", pumpFlow.toNumberRequired("Flow"))
            }
        }
        val transition = buildJsonObject {
            put("type", transitionType)
            put("duration", transitionDuration.toNumberRequired("Ramp duration"))
            put("adaptive", adaptive)
        }
        val targetList = targets.map { it.toJson() }
        return GaggiMateProfilePhase(
            name = name.trim(),
            phase = phase,
            valve = valve,
            duration = duration.toNumberRequired("Phase duration"),
            temperature = temperature.toOptionalNumber("Phase temperature"),
            transition = transition,
            pump = pump,
            targets = targetList.ifEmpty { null },
        )
    }

    companion object {
        fun from(phase: GaggiMateProfilePhase): ProfilePhaseDraft {
            val pumpPrimitive = phase.pump as? JsonPrimitive
            val pumpObject = phase.pump as? JsonObject
            val transition = phase.transition as? JsonObject
            val targets = phase.targets.orEmpty().map { target ->
                val values = target.jsonObject
                ProfileTargetDraft(
                    type = values["type"]?.jsonPrimitive?.content ?: "volumetric",
                    operator = values["operator"]?.jsonPrimitive?.content ?: "gte",
                    value = values["value"]?.jsonPrimitive?.doubleOrNull?.toString().orEmpty(),
                )
            }
            return ProfilePhaseDraft(
                name = phase.name,
                phase = phase.phase,
                valve = phase.valve,
                duration = phase.duration.toString(),
                temperature = phase.temperature?.toString() ?: "0",
                pumpMode = when {
                    pumpPrimitive?.intOrNull != null -> "fixed"
                    else -> pumpObject?.get("target")?.jsonPrimitive?.content ?: "pressure"
                },
                pumpPressure = pumpObject?.get("pressure")?.jsonPrimitive?.doubleOrNull?.toString() ?: "9",
                pumpFlow = pumpObject?.get("flow")?.jsonPrimitive?.doubleOrNull?.toString() ?: "0",
                pumpDuty = pumpPrimitive?.intOrNull?.toString() ?: "100",
                transitionType = transition?.get("type")?.jsonPrimitive?.content ?: "instant",
                transitionDuration = transition?.get("duration")?.jsonPrimitive?.doubleOrNull?.toString() ?: "0",
                adaptive = transition?.get("adaptive")?.jsonPrimitive?.content == "true",
                targets = targets,
            )
        }

        fun default(index: Int) = ProfilePhaseDraft(
            name = "Phase $index",
            phase = "brew",
            valve = 1,
            duration = "25",
            temperature = "0",
            pumpMode = "pressure",
            pumpPressure = "9",
            pumpFlow = "0",
            pumpDuty = "100",
            transitionType = "instant",
            transitionDuration = "0",
            adaptive = false,
            targets = emptyList(),
        )
    }
}

private data class ProfileTargetDraft(
    val type: String = "volumetric",
    val operator: String = "gte",
    val value: String = "36",
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("type", type)
        put("operator", operator)
        put("value", value.toNumberRequired("Target value"))
    }
}

private fun String.toOptionalNumber(label: String): Double? {
    if (isBlank()) return null
    return toDoubleOrNull() ?: throw IllegalArgumentException("$label must be a valid number.")
}

private fun String.toNumberRequired(label: String): Double =
    toDoubleOrNull() ?: throw IllegalArgumentException("$label must be a valid number.")

private fun String.toIntRequired(label: String): Int =
    toIntOrNull() ?: throw IllegalArgumentException("$label must be a whole number.")
