package org.unishiasy.vehiclelog.ui.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStorage(name: String?): ThemeMode = when (name?.uppercase()) {
            "LIGHT" -> LIGHT
            "DARK" -> DARK
            else -> SYSTEM
        }
    }
}

class AppearancePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("vehicle_log_appearance", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        ThemeMode.fromStorage(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name))
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
