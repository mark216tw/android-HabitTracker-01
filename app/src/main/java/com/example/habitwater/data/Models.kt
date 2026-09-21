package com.example.habitwater.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val daysMask: Int = EVERY_DAY_MASK,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val archivedOn: String? = null,
    val streakResetOn: String? = null,
)

@Entity(
    tableName = "habit_archive_periods",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId")],
)
data class HabitArchivePeriod(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val archivedOn: String,
    val restoredOn: String? = null,
)

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId")],
)
data class HabitCompletion(val habitId: Long, val date: String)

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMl: Int,
    val recordedAt: Long = System.currentTimeMillis(),
)

const val EVERY_DAY_MASK = 0b1111111
const val WEEKDAY_MASK = 0b0011111

fun Habit.isScheduledOn(date: LocalDate): Boolean =
    daysMask and (1 shl (date.dayOfWeek.value - 1)) != 0

fun daysMaskOf(days: Set<DayOfWeek>): Int =
    days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

fun isCompletionEditable(date: LocalDate, today: LocalDate): Boolean =
    !date.isAfter(today) && !date.isBefore(today.minusDays(6))
