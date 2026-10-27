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

    private companion object {
        const val PREFERENCES_NAME = "beanpilot_preferences"
        const val MACHINE_ADDRESS_KEY = "gaggimate_address"
    }
}
