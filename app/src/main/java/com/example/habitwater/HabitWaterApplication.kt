package com.example.habitwater

import android.app.Application
import com.example.habitwater.data.AppDatabase
import com.example.habitwater.data.AppRepository
import com.example.habitwater.data.SettingsRepository
import com.example.habitwater.notification.NotificationHelper

class HabitWaterApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { AppRepository(database) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
