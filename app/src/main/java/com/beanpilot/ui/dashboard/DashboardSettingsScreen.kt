package com.beanpilot.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardSettingsScreen(
    selectedLayout: DashboardLayout,
    onSelectLayout: (DashboardLayout) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            Text("Dashboard layout", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Back to dashboard") }
            Text("Choose a layout. You can change this at any time.", style = MaterialTheme.typography.bodyLarge)
            DashboardLayout.entries.forEach { layout ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(layout.label, style = MaterialTheme.typography.titleMedium)
                        Text(layout.description())
                        Button(
                            onClick = { onSelectLayout(layout) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = layout != selectedLayout,
                        ) {
                            Text(if (layout == selectedLayout) "Selected" else "Use this layout")
                    }
                }
            }
        }
    }
}

private fun DashboardLayout.description(): String =
    when (this) {
        DashboardLayout.BrewStation ->
            "Machine controls and the active brew profile come first, followed by live metrics and shot history."
        DashboardLayout.Classic ->
            "Connection and machine status lead, with controls below telemetry and before shot history."
        DashboardLayout.Compact ->
            "Controls and a compact metric grid are grouped near the top to reduce scrolling."
    }
