package com.example.habitwater.data

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    @Test
    fun weekdayMaskSchedulesMondayButNotSunday() {
        val habit = Habit(name = "閱讀", daysMask = WEEKDAY_MASK)

        assertTrue(habit.isScheduledOn(LocalDate.of(2026, 9, 21)))
        assertFalse(habit.isScheduledOn(LocalDate.of(2026, 9, 20)))
    }

    @Test
    fun customMaskSchedulesOnlySelectedDays() {
        val mask = daysMaskOf(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY))
        val habit = Habit(name = "運動", daysMask = mask)

        assertTrue(habit.isScheduledOn(LocalDate.of(2026, 9, 22)))
        assertFalse(habit.isScheduledOn(LocalDate.of(2026, 9, 23)))
        assertTrue(habit.isScheduledOn(LocalDate.of(2026, 9, 26)))
    }

    @Test
    fun completionCanOnlyBeEditedWithinTodayAndPreviousSixDays() {
        val today = LocalDate.of(2026, 9, 21)

        assertTrue(isCompletionEditable(today, today))
        assertTrue(isCompletionEditable(today.minusDays(6), today))
        assertFalse(isCompletionEditable(today.minusDays(7), today))
        assertFalse(isCompletionEditable(today.plusDays(1), today))
    }

    @Test
    fun dayWindowUsesLocalDstBoundaries() {
        val zone = java.time.ZoneId.of("Europe/Berlin")
        val spring = dayWindow(LocalDate.of(2026, 3, 29), zone)
        val autumn = dayWindow(LocalDate.of(2026, 10, 25), zone)

        assertEquals(23, (spring.endMillis - spring.startMillis) / 3_600_000)
        assertEquals(25, (autumn.endMillis - autumn.startMillis) / 3_600_000)
    }
}
