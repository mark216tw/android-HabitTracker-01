package com.example.habitwater.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.habitwater.HabitWaterApplication
import com.example.habitwater.data.Habit
import com.example.habitwater.data.HabitCompletion
import com.example.habitwater.data.HabitArchivePeriod
import com.example.habitwater.data.ThemeMode
import com.example.habitwater.data.UserSettings
import com.example.habitwater.data.WaterEntry
import com.example.habitwater.notification.ReminderScheduler
import com.example.habitwater.notification.NotificationHelper
import com.example.habitwater.notification.NotificationStatus
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val habits: List<Habit> = emptyList(),
    val archivedHabits: List<Habit> = emptyList(),
    val completions: List<HabitCompletion> = emptyList(),
    val archivePeriods: List<HabitArchivePeriod> = emptyList(),
    val waterEntries: List<WaterEntry> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val today: LocalDate = LocalDate.now(),
    val notificationStatus: NotificationStatus = NotificationStatus(),
    val ready: Boolean = false,
) {
    val waterTotal: Int get() = waterEntries.sumOf { it.amountMl }
    fun isCompleted(habitId: Long, date: LocalDate = today): Boolean =
        completions.any { it.habitId == habitId && it.date == date.toString() }
}

private data class TodayKey(val date: LocalDate, val zoneId: ZoneId) {
    companion object {
        fun now(): TodayKey {
            val zone = ZoneId.systemDefault()
            return TodayKey(LocalDate.now(zone), zone)
        }
    }
}

private data class DatedWaterEntries(val key: TodayKey, val entries: List<WaterEntry>)
private data class HabitLists(
    val active: List<Habit>,
    val archived: List<Habit>,
    val archivePeriods: List<HabitArchivePeriod>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HabitWaterApplication
    private val repository = app.repository
    private val settingsRepository = app.settingsRepository
    private val todayKey = MutableStateFlow(TodayKey.now())
    private val notificationStatus = MutableStateFlow(NotificationHelper.status(app))
    private val habitLists = combine(
        repository.habits,
        repository.archivedHabits,
        repository.archivePeriods,
    ) { active, archived, archivePeriods ->
        HabitLists(active, archived, archivePeriods)
    }
    private val waterEntries = todayKey.flatMapLatest { key ->
        repository.waterEntries(key.date, key.zoneId).map { DatedWaterEntries(key, it) }
    }

    val state = combine(
        habitLists,
        repository.completions,
        waterEntries,
        settingsRepository.settings,
        notificationStatus,
    ) { habits, completions, water, settings, notificationStatus ->
        MainUiState(
            habits = habits.active,
            archivedHabits = habits.archived,
            completions = completions,
            archivePeriods = habits.archivePeriods,
            waterEntries = water.entries,
            settings = settings,
            today = water.key.date,
            notificationStatus = notificationStatus,
            ready = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun refreshToday() {
        todayKey.value = TodayKey.now()
        notificationStatus.value = NotificationHelper.status(app)
    }

    fun onAppForeground() {
        refreshToday()
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            ReminderScheduler.reconcile(app, settings.waterReminders, settings.wakeHour, repository.habits.first())
        }
    }

    fun addWater(amount: Int, onAdded: (Long) -> Unit = {}) = viewModelScope.launch {
        onAdded(repository.addWater(amount))
    }

    fun removeWater(id: Long) = viewModelScope.launch { repository.removeWater(id) }

    fun toggleHabit(habit: Habit) = viewModelScope.launch {
        repository.toggleCompleted(habit.id, state.value.today)
    }

    fun toggleHabit(habitId: Long, date: LocalDate) = viewModelScope.launch {
        repository.toggleCompleted(habitId, date)
    }

    fun saveHabit(habit: Habit) = viewModelScope.launch {
        val id = repository.saveHabit(habit)
        if (id != 0L) ReminderScheduler.scheduleHabit(app, habit.copy(id = id))
    }

    fun archiveHabit(habit: Habit) = viewModelScope.launch {
        if (repository.archiveHabit(habit.id, state.value.today)) {
            ReminderScheduler.cancelHabit(app, habit.id)
        }
    }

    fun restoreHabit(habit: Habit) = viewModelScope.launch {
        val today = state.value.today
        if (repository.restoreHabit(habit.id, today)) {
            ReminderScheduler.scheduleHabit(
                app,
                habit.copy(
                    archivedOn = null,
                    streakResetOn = today.toString(),
                ),
            )
        }
    }

    fun deleteArchivedHabit(habit: Habit) = viewModelScope.launch {
        if (repository.deleteArchivedHabit(habit.id)) {
            ReminderScheduler.cancelHabit(app, habit.id)
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setHue(hue: Float) = viewModelScope.launch { settingsRepository.setHue(hue) }
    fun setWaterGoal(goal: Int) = viewModelScope.launch { settingsRepository.setWaterGoal(goal) }
    fun setWakeHour(hour: Int) = viewModelScope.launch {
        settingsRepository.setWakeHour(hour)
        if (state.value.settings.waterReminders) ReminderScheduler.scheduleWater(app, true, hour)
    }
    fun setSleepHour(hour: Int) = viewModelScope.launch { settingsRepository.setSleepHour(hour) }

    fun setWaterReminders(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setWaterReminders(enabled)
        ReminderScheduler.scheduleWater(app, enabled, state.value.settings.wakeHour)
    }
}
