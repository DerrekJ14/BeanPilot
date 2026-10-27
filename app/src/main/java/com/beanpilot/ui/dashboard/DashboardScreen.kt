package com.beanpilot.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beanpilot.data.remote.MachineConnectionStatus
import com.beanpilot.domain.model.ShotRecord
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var address by rememberSaveable { mutableStateOf(uiState.machineAddress) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BeanPilot Dashboard") },
            )
        },
    ) { paddingValues ->
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
                    ShotSummaryCard(shot = shot)
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
private fun ShotSummaryCard(shot: ShotRecord) {
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
        }
    }
}

private fun Double.oneDecimal(): String =
    String.format(Locale.getDefault(), "%.1f", this)
