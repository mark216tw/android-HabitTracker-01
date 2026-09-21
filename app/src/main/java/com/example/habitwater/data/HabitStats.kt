package com.example.habitwater.data

import java.time.LocalDate
import java.time.YearMonth

data class HabitStats(
    val completedLast7Days: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completedThisMonth: Int = 0,
)

fun calculateHabitStats(
    habit: Habit,
    completions: List<HabitCompletion>,
    asOf: LocalDate,
    archivePeriods: List<HabitArchivePeriod> = emptyList(),
): HabitStats {
    val completedDates = completions.asSequence()
        .filter { it.habitId == habit.id }
        .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        .filterNot { it.isAfter(asOf) }
        .toSet()
    val recentStart = asOf.minusDays(6)
    val month = YearMonth.from(asOf)

    return HabitStats(
        completedLast7Days = completedDates.count { !it.isBefore(recentStart) },
        currentStreak = currentStreak(habit, completedDates, asOf),
        bestStreak = bestStreak(habit, completedDates, asOf, archivePeriods),
        completedThisMonth = completedDates.count { YearMonth.from(it) == month },
    )
}

private fun currentStreak(habit: Habit, completedDates: Set<LocalDate>, asOf: LocalDate): Int {
    val resetOn = habit.streakResetOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val relevantDates = completedDates.filter { resetOn == null || !it.isBefore(resetOn) }
    val lowerBound = resetOn ?: relevantDates.minOrNull() ?: return 0
    var cursor = asOf
    if (habit.isScheduledOn(cursor) && cursor !in completedDates) cursor = cursor.minusDays(1)
    var streak = 0
    while (!cursor.isBefore(lowerBound)) {
        if (habit.isScheduledOn(cursor)) {
            if (cursor !in completedDates) break
            streak++
        }
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun bestStreak(
    habit: Habit,
    completedDates: Set<LocalDate>,
    asOf: LocalDate,
    archivePeriods: List<HabitArchivePeriod>,
): Int {
    var cursor = completedDates.minOrNull() ?: return 0
    val restoredDates = archivePeriods.asSequence()
        .filter { it.habitId == habit.id }
        .mapNotNull { it.restoredOn?.let { date -> runCatching { LocalDate.parse(date) }.getOrNull() } }
        .filterNot { it.isAfter(asOf) }
        .toSet()
    var current = 0
    var best = 0
    while (!cursor.isAfter(asOf)) {
        if (cursor in restoredDates) current = 0
        if (habit.isScheduledOn(cursor)) {
            if (cursor in completedDates) {
                current++
                best = maxOf(best, current)
            } else {
                current = 0
            }
        }
        cursor = cursor.plusDays(1)
    }
    return best
}
