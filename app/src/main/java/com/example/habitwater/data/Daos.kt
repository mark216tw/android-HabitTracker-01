package com.example.habitwater.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archivedOn IS NULL ORDER BY createdAt, id")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE archivedOn IS NOT NULL ORDER BY archivedOn DESC, createdAt, id")
    fun observeArchivedHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habit_completions")
    fun observeCompletions(): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_archive_periods ORDER BY archivedOn, id")
    fun observeArchivePeriods(): Flow<List<HabitArchivePeriod>>

    @Insert
    suspend fun insert(habit: Habit): Long

    @Query(
        """UPDATE habits SET name = :name, daysMask = :daysMask,
            reminderHour = :reminderHour, reminderMinute = :reminderMinute
            WHERE id = :habitId AND archivedOn IS NULL""",
    )
    suspend fun updateActive(
        habitId: Long,
        name: String,
        daysMask: Int,
        reminderHour: Int?,
        reminderMinute: Int?,
    ): Int

    @Query("UPDATE habits SET archivedOn = :date WHERE id = :habitId AND archivedOn IS NULL")
    suspend fun markArchived(habitId: Long, date: String): Int

    @Insert
    suspend fun insertArchivePeriod(period: HabitArchivePeriod)

    @Transaction
    suspend fun archive(habitId: Long, date: String): Boolean {
        if (markArchived(habitId, date) == 0) return false
        insertArchivePeriod(HabitArchivePeriod(habitId = habitId, archivedOn = date))
        return true
    }

    @Query(
        """UPDATE habits SET archivedOn = NULL, streakResetOn = :date
            WHERE id = :habitId AND archivedOn IS NOT NULL""",
    )
    suspend fun markRestored(habitId: Long, date: String): Int

    @Query("UPDATE habit_archive_periods SET restoredOn = :date WHERE habitId = :habitId AND restoredOn IS NULL")
    suspend fun closeArchivePeriod(habitId: Long, date: String)

    @Transaction
    suspend fun restore(habitId: Long, date: String): Boolean {
        if (markRestored(habitId, date) == 0) return false
        closeArchivePeriod(habitId, date)
        return true
    }

    @Query("DELETE FROM habits WHERE id = :habitId AND archivedOn IS NOT NULL")
    suspend fun deleteArchived(habitId: Long): Int

    @Query(
        """INSERT OR IGNORE INTO habit_completions (habitId, date)
            SELECT :habitId, :date
            WHERE EXISTS (SELECT 1 FROM habits WHERE id = :habitId AND archivedOn IS NULL)""",
    )
    suspend fun complete(habitId: Long, date: String)

    @Query(
        """DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date
            AND EXISTS (SELECT 1 FROM habits WHERE id = :habitId AND archivedOn IS NULL)""",
    )
    suspend fun uncomplete(habitId: Long, date: String)

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND date = :date)")
    suspend fun isCompleted(habitId: Long, date: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM habits WHERE id = :habitId AND archivedOn IS NULL)")
    suspend fun isActive(habitId: Long): Boolean

    @Transaction
    suspend fun toggleCompletion(habitId: Long, date: String) {
        if (!isActive(habitId)) return
        if (isCompleted(habitId, date)) uncomplete(habitId, date)
        else complete(habitId, date)
    }
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_entries WHERE recordedAt >= :start AND recordedAt < :end ORDER BY recordedAt DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<WaterEntry>>

    @Insert
    suspend fun insert(entry: WaterEntry): Long

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
