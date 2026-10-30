package com.beanpilot.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SavedDashboardLayoutsScreen(
    layouts: List<SavedDashboardLayout>,
    selectedLayoutId: String,
    message: String?,
    onSelect: (String) -> Unit,
    onSaveAs: (String) -> String?,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var layoutName by rememberSaveable { mutableStateOf("") }
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }
    val customLayoutCount = layouts.count { it.id != "default" }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save current layout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Save the current widget order, visibility, and sizes as a named layout.")
                    OutlinedTextField(
                        value = layoutName,
                        onValueChange = { layoutName = it.take(SavedDashboardLayout.MAX_LAYOUT_NAME_LENGTH) },
                        label = { Text("Layout name") },
                        singleLine = true,
                        supportingText = {
                            Text("${layoutName.length}/${SavedDashboardLayout.MAX_LAYOUT_NAME_LENGTH}")
                        },
                    )
                    saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveError = onSaveAs(layoutName)
                        if (saveError == null) {
                            showSaveDialog = false
                            layoutName = ""
                        }
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
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
        TextButton(onClick = onBack) { Text("Back to dashboard") }
        Text("Saved dashboards", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Save up to three named layouts. Each stores the current widget order, visibility, and size.",
            style = MaterialTheme.typography.bodyMedium,
        )
        layouts.forEach { layout ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(layout.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${layout.widgets.count { it !in layout.hiddenWidgets }} widgets",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(
                        onClick = { onSelect(layout.id); onBack() },
                        enabled = layout.id != selectedLayoutId,
                    ) {
                        Text(if (layout.id == selectedLayoutId) "Active" else "Use")
                    }
                    if (layout.id != "default") {
                        TextButton(onClick = { onDelete(layout.id) }) { Text("Delete") }
                    }
                }
            }
        }
        Button(
            onClick = {
                saveError = null
                showSaveDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = customLayoutCount < SavedDashboardLayout.MAX_SAVED_LAYOUTS,
        ) {
            Text(
                if (customLayoutCount < SavedDashboardLayout.MAX_SAVED_LAYOUTS) {
                    "Save current as…"
                } else {
                    "Three custom layouts saved"
                },
            )
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
