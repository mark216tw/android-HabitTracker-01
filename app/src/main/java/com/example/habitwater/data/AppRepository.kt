package com.example.habitwater.data

import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

data class DayWindow(val startMillis: Long, val endMillis: Long)

fun dayWindow(date: LocalDate, zone: ZoneId): DayWindow = DayWindow(
    startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
    endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
)

class AppRepository(private val database: AppDatabase) {
    val habits: Flow<List<Habit>> = database.habitDao().observeHabits()
    val archivedHabits: Flow<List<Habit>> = database.habitDao().observeArchivedHabits()
    val completions: Flow<List<HabitCompletion>> = database.habitDao().observeCompletions()
    val archivePeriods: Flow<List<HabitArchivePeriod>> = database.habitDao().observeArchivePeriods()

    fun waterEntries(date: LocalDate, zone: ZoneId): Flow<List<WaterEntry>> {
        val window = dayWindow(date, zone)
        return database.waterDao().observeBetween(window.startMillis, window.endMillis)
    }

    suspend fun addWater(amount: Int): Long = database.waterDao().insert(WaterEntry(amountMl = amount))
    suspend fun removeWater(id: Long) = database.waterDao().deleteById(id)
    suspend fun saveHabit(habit: Habit): Long = if (habit.id == 0L) {
        database.habitDao().insert(habit)
    } else {
        val updated = database.habitDao().updateActive(
            habit.id,
            habit.name,
            habit.daysMask,
            habit.reminderHour,
            habit.reminderMinute,
        )
        if (updated > 0) habit.id else 0L
    }

    suspend fun archiveHabit(habitId: Long, date: LocalDate): Boolean =
        database.habitDao().archive(habitId, date.toString())

    suspend fun restoreHabit(habitId: Long, date: LocalDate): Boolean =
        database.habitDao().restore(habitId, date.toString())

    suspend fun deleteArchivedHabit(habitId: Long): Boolean =
        database.habitDao().deleteArchived(habitId) > 0

    suspend fun toggleCompleted(habitId: Long, date: LocalDate) =
        database.habitDao().toggleCompletion(habitId, date.toString())
}
