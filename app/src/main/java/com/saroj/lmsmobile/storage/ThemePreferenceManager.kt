package com.saroj.lmsmobile.storage

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object ThemePreferenceManager {
    const val KEY_APP_THEME_MODE = "app_theme_mode"
    private const val THEME_LIGHT = "light"
    private const val THEME_DARK = "dark"

    private val appThemeModeKey = stringPreferencesKey(KEY_APP_THEME_MODE)

    fun isDarkModeEnabled(context: Context): Flow<Boolean> =
        context.applicationContext.lmsDataStore.data.map { preferences ->
            preferences[appThemeModeKey] == THEME_DARK
        }

    suspend fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.lmsDataStore.edit { preferences ->
            preferences[appThemeModeKey] = if (enabled) THEME_DARK else THEME_LIGHT
        }
        applyNightMode(enabled)
    }

    fun applySavedThemeBlocking(context: Context) {
        val enabled = runBlocking {
            context.applicationContext.lmsDataStore.data
                .map { preferences -> preferences[appThemeModeKey] == THEME_DARK }
                .firstOrNull() ?: false
        }
        applyNightMode(enabled)
    }

    fun applyNightMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
