package com.beanpilot.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.beanpilot.domain.model.TelemetrySnapshot
import java.util.Locale

@Composable
fun LiveTelemetryChart(
    points: List<TelemetrySnapshot>,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val lines = listOf(
        TelemetryLine("PRESSURE", "bar", colors.primary) { it.pressureBar },
        TelemetryLine("FLOW", "ml/min", colors.secondary) { it.flowMlPerMin },
        TelemetryLine("TEMP", "°C", colors.tertiary) { it.temperatureC },
        TelemetryLine("WEIGHT", "g", Color(0xFFFFB86B)) { it.weightGr },
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
                                Text(line.label, color = line.color, style = MaterialTheme.typography.labelSmall)
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
                    ) {
                        Text(line.label, color = line.color, style = MaterialTheme.typography.labelMedium)
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
    val label: String,
    val unit: String,
    val color: Color,
    val valueOf: (TelemetrySnapshot) -> Double?,
)

private fun Double.oneDecimal(): String = String.format(Locale.getDefault(), "%.1f", this)
