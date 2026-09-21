package com.example.habitwater.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitStatsTest {
    private val today = LocalDate.of(2026, 9, 21)

    @Test
    fun countsRecentAndCurrentMonthIncludingUnscheduledCompletions() {
        val habit = Habit(id = 1, name = "閱讀", daysMask = WEEKDAY_MASK)
        val completions = listOf(
            completion(1, today),
            completion(1, today.minusDays(2)),
            completion(1, today.minusDays(6)),
            completion(1, today.minusDays(7)),
            completion(1, LocalDate.of(2026, 8, 31)),
        )

        val stats = calculateHabitStats(habit, completions, today)

        assertEquals(3, stats.completedLast7Days)
        assertEquals(4, stats.completedThisMonth)
    }

    @Test
    fun streakUsesScheduledDaysAndAllowsTodayToRemainIncomplete() {
        val habit = Habit(id = 1, name = "運動", daysMask = WEEKDAY_MASK)
        val completions = listOf(
            completion(1, LocalDate.of(2026, 9, 16)),
            completion(1, LocalDate.of(2026, 9, 17)),
            completion(1, LocalDate.of(2026, 9, 18)),
            completion(1, LocalDate.of(2026, 9, 20)), // Sunday does not extend the streak.
        )

        val stats = calculateHabitStats(habit, completions, today)

        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
    }

    @Test
    fun missedScheduledDayBreaksCurrentStreak() {
        val habit = Habit(id = 1, name = "運動")
        val completions = listOf(
            completion(1, today.minusDays(2)),
            completion(1, today),
        )

        val stats = calculateHabitStats(habit, completions, today)

        assertEquals(1, stats.currentStreak)
        assertEquals(1, stats.bestStreak)
    }

    @Test
    fun restoreDateResetsOnlyCurrentStreak() {
        val habit = Habit(
            id = 1,
            name = "冥想",
            streakResetOn = today.minusDays(1).toString(),
        )
        val completions = listOf(
            completion(1, today.minusDays(4)),
            completion(1, today.minusDays(3)),
            completion(1, today.minusDays(2)),
            completion(1, today.minusDays(1)),
            completion(1, today),
        )

        val periods = listOf(
            HabitArchivePeriod(
                habitId = 1,
                archivedOn = today.minusDays(2).toString(),
                restoredOn = today.minusDays(1).toString(),
            ),
        )
        val stats = calculateHabitStats(habit, completions, today, periods)

        assertEquals(2, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
    }

    @Test
    fun everyRestoreBoundarySplitsBestStreak() {
        val habit = Habit(id = 1, name = "冥想", streakResetOn = today.minusDays(1).toString())
        val completions = (0L..6L).map { completion(1, today.minusDays(it)) }
        val periods = listOf(
            HabitArchivePeriod(habitId = 1, archivedOn = today.minusDays(5).toString(), restoredOn = today.minusDays(4).toString()),
            HabitArchivePeriod(habitId = 1, archivedOn = today.minusDays(2).toString(), restoredOn = today.minusDays(1).toString()),
        )

        val stats = calculateHabitStats(habit, completions, today, periods)

        assertEquals(2, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
    }

    @Test
    fun archivedStatsUseProvidedArchiveDate() {
        val archivedOn = LocalDate.of(2026, 8, 31)
        val habit = Habit(id = 1, name = "閱讀", archivedOn = archivedOn.toString())
        val completions = listOf(
            completion(1, archivedOn.minusDays(1)),
            completion(1, archivedOn),
            completion(1, archivedOn.plusDays(1)),
        )

        val stats = calculateHabitStats(habit, completions, archivedOn)

        assertEquals(2, stats.completedLast7Days)
        assertEquals(2, stats.completedThisMonth)
        assertEquals(2, stats.currentStreak)
    }

    private fun completion(habitId: Long, date: LocalDate) = HabitCompletion(habitId, date.toString())
}
