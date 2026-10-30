package com.beanpilot.ui.dashboard

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.beanpilot.data.remote.GaggiMateWarning

@Composable
fun MachineControlCard(
    connected: Boolean,
    activeProfile: String?,
    state: ProfileUiState,
    onOpenProfiles: () -> Unit,
    onStartBrew: () -> Unit,
    onConfirmBrew: () -> Unit,
    onCancelBrew: () -> Unit,
    onStartFlush: () -> Unit,
    onStopFlush: () -> Unit,
    onStopFlushManually: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (state.pendingWarnings.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onCancelBrew,
            title = { Text("GaggiMate warning") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("The machine reported these warnings before starting the process:")
                    state.pendingWarnings.forEach { warning ->
                        Text("• ${warning.title()}")
                    }
                    Text("Continue only after checking the machine.")
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmBrew) { Text("Start anyway") }
            },
            dismissButton = {
                TextButton(onClick = onCancelBrew) { Text("Cancel") }
            },
        )
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
        ) {
            Text(
                if (compact) "CONTROLS" else "Machine controls",
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            )
            Text(
                "Profile: ${activeProfile ?: "Not available"}",
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            )
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(
                        onClick = onOpenProfiles,
                        enabled = connected,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Profiles")
                    }
                    Button(
                        onClick = onStartBrew,
                        enabled = connected && state.pendingWarnings.isEmpty() && !state.brewStarting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.brewStarting) "Starting…" else "Start process")
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onOpenProfiles, enabled = connected) {
                        Text("Profiles")
                    }
                    Button(
                        onClick = onStartBrew,
                        enabled = connected && state.pendingWarnings.isEmpty() && !state.brewStarting,
                    ) {
                        Text(if (state.brewStarting) "Starting…" else "Start process")
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Hold to flush. Release sends a stop request."
                        role = Role.Button
                        onClick(label = "Start flush") {
                            onStartFlush()
                            true
                        }
                    }
                    .pointerInput(connected) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (!connected) return@awaitEachGesture
                            onStartFlush()
                            try {
                                waitForUpOrCancellation()
                            } finally {
                                onStopFlush()
                            }
                            down.consume()
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (connected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (connected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Text(
                    "Hold to flush · release sends stop",
                    modifier = Modifier.padding(
                        horizontal = if (compact) 10.dp else 16.dp,
                        vertical = if (compact) 10.dp else 14.dp,
                    ),
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                )
            }
            if (compact) {
                Column {
                    TextButton(onClick = onStartFlush, enabled = connected, modifier = Modifier.fillMaxWidth()) {
                        Text("Start flush")
                    }
                    TextButton(
                        onClick = onStopFlushManually,
                        enabled = connected,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Stop flush")
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onStartFlush, enabled = connected) {
                        Text("Start flush")
                    }
                    TextButton(onClick = onStopFlushManually, enabled = connected) {
                        Text("Stop flush")
                    }
                }
            }
            if (!connected) {
                Text("Connect to GaggiMate to enable machine controls.")
            }
            if (!compact) {
                Text(
                    "Flush release behavior depends on the machine’s configured flush duration. Brew stop is not exposed as a documented remote command; use the machine’s normal stop control.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    "Stop brew at the machine; flush release uses its configured duration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.message?.let { message ->
                Text(
                    message,
                    color = if (message.startsWith("Could not")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun GaggiMateWarning.title(): String =
    when (key) {
        "water" -> "Water level warning"
        "flush" -> "Flush reminder"
        "switch" -> "Machine switch warning"
        "scaleConnected" -> "Scale connection warning"
        "scaleBattery" -> "Scale battery warning"
        "temperature" -> "Temperature warning"
        "backflush" -> "Backflush reminder"
        "descaling" -> "Descaling reminder"
        else -> "Machine warning: $key"
    } + if (severity == 2) " (error severity)" else ""
