package com.example.habitwater.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.habitwater.HabitWaterApplication
import com.example.habitwater.MainActivity
import com.example.habitwater.R
import com.example.habitwater.data.Habit
import com.example.habitwater.data.isScheduledOn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

data class NotificationStatus(
    val permissionGranted: Boolean = true,
    val appEnabled: Boolean = true,
    val waterChannelEnabled: Boolean = true,
    val habitChannelEnabled: Boolean = true,
) {
    val waterAvailable: Boolean
        get() = permissionGranted && appEnabled && waterChannelEnabled
    val habitAvailable: Boolean
        get() = permissionGranted && appEnabled && habitChannelEnabled
}

object NotificationHelper {
    const val CHANNEL_WATER = "water_reminders"
    const val CHANNEL_HABITS = "habit_reminders"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_WATER, "飲水提醒", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_HABITS, "習慣提醒", NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }

    fun status(context: Context): NotificationStatus {
        val manager = context.getSystemService(NotificationManager::class.java)
        val permissionGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return NotificationStatus(
            permissionGranted = permissionGranted,
            appEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            waterChannelEnabled = manager.getNotificationChannel(CHANNEL_WATER)?.importance?.let {
                it != NotificationManager.IMPORTANCE_NONE
            } == true,
            habitChannelEnabled = manager.getNotificationChannel(CHANNEL_HABITS)?.importance?.let {
                it != NotificationManager.IMPORTANCE_NONE
            } == true,
        )
    }

    fun show(context: Context, id: Int, title: String, text: String, water: Boolean) {
        val status = status(context)
        if (water && !status.waterAvailable || !water && !status.habitAvailable) return
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, if (water) CHANNEL_WATER else CHANNEL_HABITS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission can change between the status check and notification delivery.
        }
    }
}

class WaterReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as HabitWaterApplication
        val settings = app.settingsRepository.settings.first()
        val hour = LocalTime.now().hour
        if (settings.waterReminders && hour >= settings.wakeHour && hour < settings.sleepHour) {
            NotificationHelper.show(
                applicationContext,
                100,
                "該喝口水囉",
                "補充水分，也起立動一動吧！",
                water = true,
            )
        }
        return Result.success()
    }
}

class HabitReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val habitId = inputData.getLong("habit_id", -1)
        val scheduledDate = inputData.getString("scheduled_date")
        val scheduledHour = inputData.getInt("reminder_hour", -1)
        val scheduledMinute = inputData.getInt("reminder_minute", -1)
        val scheduledDays = inputData.getInt("days_mask", -1)
        val app = applicationContext as HabitWaterApplication
        val habit = app.repository.habits.first().firstOrNull { it.id == habitId }
        val today = LocalDate.now()
        val scheduleIsCurrent = habit?.reminderHour == scheduledHour &&
            habit.reminderMinute == scheduledMinute && habit.daysMask == scheduledDays
        val completed = app.repository.completions.first().any {
            it.habitId == habitId && it.date == today.toString()
        }
        if (habit != null && scheduleIsCurrent && scheduledDate == today.toString() &&
            habit.isScheduledOn(today) && !completed
        ) {
            NotificationHelper.show(
                applicationContext,
                (1000 + habit.id).toInt(),
                "習慣時間到了",
                "別忘了完成「${habit.name}」",
                water = false,
            )
        }
        if (habit != null && scheduleIsCurrent) ReminderScheduler.scheduleHabit(applicationContext, habit)
        return Result.success()
    }
}

object ReminderScheduler {
    private const val WATER_WORK = "water-reminder"

    fun scheduleWater(context: Context, enabled: Boolean, wakeHour: Int = 8) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(WATER_WORK)
            return
        }
        val now = ZonedDateTime.now()
        var next = now.toLocalDate().atTime(wakeHour, 0).atZone(now.zone)
        while (!next.isAfter(now)) next = next.plusHours(2)
        val work = PeriodicWorkRequestBuilder<WaterReminderWorker>(2, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next))
            .build()
        manager.enqueueUniquePeriodicWork(WATER_WORK, ExistingPeriodicWorkPolicy.UPDATE, work)
    }

    fun scheduleHabit(context: Context, habit: Habit) {
        val manager = WorkManager.getInstance(context)
        val name = "habit-reminder-${habit.id}"
        val hour = habit.reminderHour
        val minute = habit.reminderMinute
        if (hour == null || minute == null) {
            manager.cancelUniqueWork(name)
            return
        }
        val now = ZonedDateTime.now()
        val next = nextHabitReminder(habit, now) ?: return
        val delay = Duration.between(now, next)
        val work = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(
                Data.Builder()
                    .putLong("habit_id", habit.id)
                    .putString("scheduled_date", next.toLocalDate().toString())
                    .putInt("reminder_hour", hour)
                    .putInt("reminder_minute", minute)
                    .putInt("days_mask", habit.daysMask)
                    .build(),
            )
            .build()
        manager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, work)
    }

    fun cancelHabit(context: Context, habitId: Long) =
        WorkManager.getInstance(context).cancelUniqueWork("habit-reminder-$habitId")

    fun reconcile(context: Context, waterEnabled: Boolean, wakeHour: Int, habits: List<Habit>) {
        scheduleWater(context, waterEnabled, wakeHour)
        habits.forEach { scheduleHabit(context, it) }
    }
}

fun nextHabitReminder(habit: Habit, now: ZonedDateTime): ZonedDateTime? {
    val hour = habit.reminderHour ?: return null
    val minute = habit.reminderMinute ?: return null
    for (daysAhead in 0L..7L) {
        val date = now.toLocalDate().plusDays(daysAhead)
        val candidate = date.atTime(hour, minute).atZone(now.zone)
        if (habit.isScheduledOn(date) && candidate.isAfter(now)) return candidate
    }
    return null
}
