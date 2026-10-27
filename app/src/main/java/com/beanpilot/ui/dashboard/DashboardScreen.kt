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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beanpilot.domain.model.ShotRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
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
                                text = uiState.telemetry.machineState,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Text(
                            text = "Profile: ${uiState.telemetry.activeProfile}",
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
                        title = "Temp",
                        value = "${uiState.telemetry.temperatureC}°C",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        title = "Pressure",
                        value = "${uiState.telemetry.pressureBar} bar",
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
                        value = "${uiState.telemetry.flowMlPerMin} ml/min",
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        title = "Weight",
                        value = "${uiState.telemetry.weightGr} g",
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

            items(uiState.recentShots) { shot ->
                ShotSummaryCard(shot = shot)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${shot.doseInGr}g in / ${shot.doseOutGr}g out",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Ratio: ${shot.ratio} · Grinder ${shot.grinderSetting} · Profile ${shot.profileUsed}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Rating: ${shot.rating}/5",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
