package com.beanpilot.ui.dashboard

enum class DashboardLayout(val preferenceValue: String, val label: String) {
    BrewStation("brew_station", "Brew Station"),
    Classic("classic", "Classic"),
    Compact("compact", "Compact");

    companion object {
        fun fromPreference(value: String): DashboardLayout =
            entries.firstOrNull { it.preferenceValue == value } ?: BrewStation
    }
}
