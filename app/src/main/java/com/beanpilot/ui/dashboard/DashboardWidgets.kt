package com.beanpilot.ui.dashboard

import kotlinx.serialization.Serializable

@Serializable
enum class DashboardWidget {
    MachineStatus,
    @Deprecated("Retained to migrate layouts saved by older app versions.")
    MachineControls,
    LiveTelemetry,
    BrewCoach,
    ShotHistory,

    ;

    companion object {
        val dashboardWidgets = listOf(
            MachineStatus,
            LiveTelemetry,
            BrewCoach,
            ShotHistory,
        )
    }
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
    val widgets: List<DashboardWidget> = DashboardWidget.dashboardWidgets,
    val hiddenWidgets: Set<DashboardWidget> = emptySet(),
    val sizes: Map<DashboardWidget, DashboardWidgetSize> = emptyMap(),
) {
    @Suppress("DEPRECATION")
    fun normalized(): SavedDashboardLayout {
        val allowedWidgets = DashboardWidget.dashboardWidgets.toSet()
        val ordered = (widgets.filterNot { it == DashboardWidget.MachineControls } +
            DashboardWidget.dashboardWidgets)
            .distinct()
            .filter { it in allowedWidgets }
        return copy(
            name = name.trim().take(MAX_LAYOUT_NAME_LENGTH),
            widgets = ordered,
            hiddenWidgets = hiddenWidgets.intersect(allowedWidgets),
            sizes = sizes.filterKeys { it in allowedWidgets },
        )
    }

    companion object {
        const val MAX_LAYOUT_NAME_LENGTH = 24
        const val MAX_SAVED_LAYOUTS = 3

        fun default(id: String = "default", name: String = "Brew Station") =
            SavedDashboardLayout(id = id, name = name)
    }
}
