package com.mohammad.orion.domain.settings

enum class ThemeMode { DARK, LIGHT, SYSTEM }
data class AppearanceSettings(val theme: ThemeMode = ThemeMode.DARK, val dynamicColor: Boolean = false)
sealed interface AppearanceLoad {
    data object Loading : AppearanceLoad
    data object Unavailable : AppearanceLoad
    data class Ready(val settings: AppearanceSettings) : AppearanceLoad
}

/** Unknown stored values fall back to the branded dark theme. */
fun parseThemeMode(value: String?): ThemeMode = ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.DARK
fun useDarkTheme(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> systemDark
}
