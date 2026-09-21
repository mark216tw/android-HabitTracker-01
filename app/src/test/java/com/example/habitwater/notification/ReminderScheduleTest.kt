package com.example.habitwater.notification

import com.example.habitwater.data.Habit
import com.example.habitwater.data.WEEKDAY_MASK
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderScheduleTest {
    @Test
    fun passedWeekdayReminderMovesToNextScheduledDay() {
        val zone = ZoneId.of("Asia/Taipei")
        val now = ZonedDateTime.of(2026, 9, 21, 9, 0, 0, 0, zone)
        val habit = Habit(name = "閱讀", daysMask = WEEKDAY_MASK, reminderHour = 8, reminderMinute = 30)

        val next = nextHabitReminder(habit, now)

        assertEquals(ZonedDateTime.of(2026, 9, 22, 8, 30, 0, 0, zone), next)
    }

    @Test
    fun reminderKeepsLocalTimeAcrossDstChange() {
        val zone = ZoneId.of("Europe/Berlin")
        val now = ZonedDateTime.of(2026, 3, 28, 9, 0, 0, 0, zone)
        val habit = Habit(name = "散步", reminderHour = 8, reminderMinute = 0)

        val next = nextHabitReminder(habit, now)

        assertEquals(ZonedDateTime.of(2026, 3, 29, 8, 0, 0, 0, zone), next)
    }
}
