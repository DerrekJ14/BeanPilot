package com.beanpilot.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@Suppress("DEPRECATION")
class SavedDashboardLayoutTest {
    @Test
    fun normalizedLayoutRemovesRetiredControlsAndKeepsTelemetryAndCoach() {
        val saved = SavedDashboardLayout(
            id = "legacy",
            name = "Old dashboard",
            widgets = listOf(
                DashboardWidget.MachineControls,
                DashboardWidget.ShotHistory,
            ),
        )

        val normalized = saved.normalized()

        assertFalse(normalized.widgets.contains(DashboardWidget.MachineControls))
        assertEquals(
            listOf(
                DashboardWidget.ShotHistory,
                DashboardWidget.MachineStatus,
                DashboardWidget.LiveTelemetry,
                DashboardWidget.BrewCoach,
            ),
            normalized.widgets,
        )
    }

    @Test
    fun defaultLayoutPrioritizesMachineTelemetryAndRecommendations() {
        val layout = SavedDashboardLayout.default()

        assertEquals(
            listOf(
                DashboardWidget.MachineStatus,
                DashboardWidget.LiveTelemetry,
                DashboardWidget.BrewCoach,
                DashboardWidget.ShotHistory,
            ),
            layout.widgets,
        )
    }
}
