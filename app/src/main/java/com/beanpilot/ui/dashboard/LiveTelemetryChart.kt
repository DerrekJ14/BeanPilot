package com.beanpilot.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.beanpilot.domain.model.TelemetrySnapshot
import java.util.Locale

@Composable
fun LiveTelemetryChart(
    points: List<TelemetrySnapshot>,
    measurementColors: Map<String, Color> = emptyMap(),
    onGraphColorChange: (String, Int?) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val lines = listOf(
        TelemetryLine("pressure", "PRESSURE", "bar", colors.primary, measurementColors["pressure"] ?: colors.primary) {
            it.pressureBar
        },
        TelemetryLine("flow", "FLOW", "ml/min", colors.secondary, measurementColors["flow"] ?: colors.secondary) {
            it.flowMlPerMin
        },
        TelemetryLine(
            "temperature",
            "TEMP",
            "°C",
            colors.tertiary,
            measurementColors["temperature"] ?: colors.tertiary,
        ) {
            it.temperatureC
        },
        TelemetryLine("weight", "WEIGHT", "g", Color(0xFFFFB86B), measurementColors["weight"] ?: Color(0xFFFFB86B)) {
            it.weightGr
        },
    )

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "LIVE EXTRACTION",
                    style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                )
                Text("${points.size} samples", style = MaterialTheme.typography.labelSmall)
            }
            if (compact) {
                lines.chunked(2).forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        group.forEach { line ->
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(line.label, color = line.color, style = MaterialTheme.typography.labelSmall)
                                    TelemetryColorControl(line, onGraphColorChange)
                                }
                                Text(
                                    points.lastOrNull()?.let(line.valueOf)
                                        ?.let { "${it.oneDecimal()} ${line.unit}" } ?: "— ${line.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            } else {
                lines.forEach { line ->
                    val value = points.lastOrNull()?.let(line.valueOf)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(line.label, color = line.color, style = MaterialTheme.typography.labelMedium)
                            TelemetryColorControl(line, onGraphColorChange)
                        }
                        Text(
                            value?.let { "${it.oneDecimal()} ${line.unit}" } ?: "— ${line.unit}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 80.dp else 120.dp),
            ) {
                val validLines = lines.mapNotNull { line ->
                    val samples = points.mapNotNull { point ->
                        point.timestamp?.let { timestamp ->
                            line.valueOf(point)?.let { timestamp to it }
                        }
                    }
                    if (samples.size < 2) null else line to samples
                }
                if (validLines.isEmpty()) return@Canvas

                val minTime = validLines.flatMap { it.second }.minOf { it.first }.toFloat()
                val maxTime = validLines.flatMap { it.second }.maxOf { it.first }.toFloat()
                val timeSpan = (maxTime - minTime).coerceAtLeast(1f)
                for (index in 1..3) {
                    val y = size.height * index / 4f
                    drawLine(
                        color = colors.outline.copy(alpha = 0.32f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                validLines.forEach { (line, samples) ->
                    val minValue = samples.minOf { it.second }.toFloat()
                    val valueSpan = (samples.maxOf { it.second } - minValue).toFloat().coerceAtLeast(0.01f)
                    val path = Path()
                    samples.forEachIndexed { index, sample ->
                        val x = ((sample.first.toFloat() - minTime) / timeSpan) * size.width
                        val y = size.height - ((sample.second.toFloat() - minValue) / valueSpan) * size.height
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, line.color, style = Stroke(width = 2.dp.toPx()))
                }
            }
            if (!compact) {
                Text(
                    "Each trace is independently scaled. Weight appears when supported scale telemetry is available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class TelemetryLine(
    val id: String,
    val label: String,
    val unit: String,
    val defaultColor: Color,
    val color: Color,
    val valueOf: (TelemetrySnapshot) -> Double?,
)

@Composable
private fun TelemetryColorControl(
    line: TelemetryLine,
    onGraphColorChange: (String, Int?) -> Unit,
) {
    var expanded by rememberSaveable(line.id) { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(32.dp)
                .semantics { contentDescription = "Change ${line.label} graph color" },
        ) {
            Icon(Icons.Filled.ColorLens, contentDescription = null, tint = line.color)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Theme default") },
                leadingIcon = {
                    Icon(Icons.Filled.Circle, contentDescription = null, tint = line.defaultColor)
                },
                onClick = {
                    onGraphColorChange(line.id, null)
                    expanded = false
                },
            )
            GRAPH_COLOR_CHOICES.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.name) },
                    leadingIcon = {
                        Icon(Icons.Filled.Circle, contentDescription = null, tint = choice.color)
                    },
                    onClick = {
                        onGraphColorChange(line.id, choice.color.toArgb())
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun Double.oneDecimal(): String = String.format(Locale.getDefault(), "%.1f", this)
