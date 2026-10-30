package com.beanpilot.ui.dashboard

import kotlinx.serialization.Serializable

@Serializable
enum class DashboardWidget {
    MachineStatus,
    MachineControls,
    LiveTelemetry,
    BrewCoach,
    ShotHistory,
}

@Serializable
enum class DashboardWidgetSize {
    Compact,
    Full,
}

@Serializable
data class SavedDashboardLayout(
    val id: String,
    val name: String,
    val widgets: List<DashboardWidget> = DashboardWidget.entries,
    val hiddenWidgets: Set<DashboardWidget> = emptySet(),
    val sizes: Map<DashboardWidget, DashboardWidgetSize> = emptyMap(),
) {
    fun normalized(): SavedDashboardLayout {
        val ordered = (widgets + DashboardWidget.entries).distinct()
        return copy(
            name = name.trim().take(MAX_LAYOUT_NAME_LENGTH),
            widgets = ordered,
            hiddenWidgets = hiddenWidgets.intersect(DashboardWidget.entries.toSet()),
            sizes = sizes.filterKeys { it in DashboardWidget.entries },
        )
    }

    companion object {
        const val MAX_LAYOUT_NAME_LENGTH = 24
        const val MAX_SAVED_LAYOUTS = 3

        fun default(id: String = "default", name: String = "Brew Station") =
            SavedDashboardLayout(id = id, name = name)
    }
}
