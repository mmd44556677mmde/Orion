package com.mohammad.orion.domain.settings

import org.junit.Assert.*
import org.junit.Test

class AppearanceSettingsTest {
    @Test fun defaultsPreserveOrionIdentity() {
        assertEquals(ThemeMode.DARK, AppearanceSettings().theme)
        assertFalse(AppearanceSettings().dynamicColor)
        assertEquals(ThemeMode.DARK, parseThemeMode(null))
        assertEquals(ThemeMode.DARK, parseThemeMode("unknown"))
    }
    @Test fun modesAndSystemStateAreIndependent() {
        assertEquals(ThemeMode.LIGHT, parseThemeMode("LIGHT"))
        assertTrue(useDarkTheme(ThemeMode.DARK, false))
        assertFalse(useDarkTheme(ThemeMode.LIGHT, true))
        assertTrue(useDarkTheme(ThemeMode.SYSTEM, true))
        assertFalse(useDarkTheme(ThemeMode.SYSTEM, false))
    }
}
