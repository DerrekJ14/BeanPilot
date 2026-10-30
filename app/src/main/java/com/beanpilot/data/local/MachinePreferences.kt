package com.beanpilot.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MachinePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getMachineAddress(): String =
        preferences.getString(MACHINE_ADDRESS_KEY, "").orEmpty()

    fun saveMachineAddress(address: String) {
        preferences.edit().putString(MACHINE_ADDRESS_KEY, address).apply()
    }

    fun getDashboardLayout(): String =
        preferences.getString(DASHBOARD_LAYOUT_KEY, DEFAULT_DASHBOARD_LAYOUT)
            ?.takeIf { it in DASHBOARD_LAYOUTS }
            ?: DEFAULT_DASHBOARD_LAYOUT

    fun saveDashboardLayout(layout: String) {
        require(layout in DASHBOARD_LAYOUTS) { "Unknown dashboard layout." }
        preferences.edit().putString(DASHBOARD_LAYOUT_KEY, layout).apply()
    }

    fun getSavedDashboardLayouts(): String? =
        preferences.getString(SAVED_DASHBOARD_LAYOUTS_KEY, null)

    fun saveSavedDashboardLayouts(layouts: String) {
        preferences.edit().putString(SAVED_DASHBOARD_LAYOUTS_KEY, layouts).apply()
    }

    fun getSelectedCustomDashboardLayout(): String =
        preferences.getString(SELECTED_CUSTOM_DASHBOARD_LAYOUT_KEY, DEFAULT_CUSTOM_LAYOUT_ID)
            .orEmpty()

    fun saveSelectedCustomDashboardLayout(id: String) {
        preferences.edit().putString(SELECTED_CUSTOM_DASHBOARD_LAYOUT_KEY, id).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "beanpilot_preferences"
        const val MACHINE_ADDRESS_KEY = "gaggimate_address"
        const val DASHBOARD_LAYOUT_KEY = "dashboard_layout"
        const val SAVED_DASHBOARD_LAYOUTS_KEY = "saved_dashboard_layouts"
        const val SELECTED_CUSTOM_DASHBOARD_LAYOUT_KEY = "selected_custom_dashboard_layout"
        const val DEFAULT_DASHBOARD_LAYOUT = "brew_station"
        const val DEFAULT_CUSTOM_LAYOUT_ID = "default"
        val DASHBOARD_LAYOUTS = setOf("brew_station", "classic", "compact")
    }
}
