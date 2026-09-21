package com.example.habitwater.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hue: Float = 205f,
    val waterGoalMl: Int = 2000,
    val waterReminders: Boolean = false,
    val wakeHour: Int = 8,
    val sleepHour: Int = 22,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val hue = floatPreferencesKey("theme_hue")
        val waterGoal = intPreferencesKey("water_goal")
        val waterReminders = booleanPreferencesKey("water_reminders")
        val wakeHour = intPreferencesKey("wake_hour")
        val sleepHour = intPreferencesKey("sleep_hour")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(preferences[Keys.themeMode] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            hue = preferences[Keys.hue] ?: 205f,
            waterGoalMl = preferences[Keys.waterGoal] ?: 2000,
            waterReminders = preferences[Keys.waterReminders] ?: false,
            wakeHour = preferences[Keys.wakeHour] ?: 8,
            sleepHour = preferences[Keys.sleepHour] ?: 22,
        )
    }

    suspend fun setThemeMode(value: ThemeMode) = context.dataStore.edit { it[Keys.themeMode] = value.name }
    suspend fun setHue(value: Float) = context.dataStore.edit { it[Keys.hue] = value }
    suspend fun setWaterGoal(value: Int) = context.dataStore.edit { it[Keys.waterGoal] = value }
    suspend fun setWaterReminders(value: Boolean) = context.dataStore.edit { it[Keys.waterReminders] = value }
    suspend fun setWakeHour(value: Int) = context.dataStore.edit { it[Keys.wakeHour] = value }
    suspend fun setSleepHour(value: Int) = context.dataStore.edit { it[Keys.sleepHour] = value }
}
