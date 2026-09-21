package com.example.habitwater.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var database: AppDatabase? = null

    @Before
    fun deleteExistingDatabase() {
        context.deleteDatabase("habit-water.db")
    }

    @After
    fun closeDatabase() {
        database?.close()
        context.deleteDatabase("habit-water.db")
    }

    @Test
    fun defaultsAreOnlyCreatedWhenDatabaseIsFirstCreated() = runBlocking {
        database = AppDatabase.create(context)
        val defaults = database!!.habitDao().observeHabits().first()
        assertEquals(listOf("運動", "冥想", "閱讀"), defaults.map { it.name })

        defaults.forEach {
            database!!.habitDao().archive(it.id, "2026-09-21")
            database!!.habitDao().deleteArchived(it.id)
        }
        database!!.close()
        database = AppDatabase.create(context)

        assertEquals(emptyList<Habit>(), database!!.habitDao().observeHabits().first())
    }

    @Test
    fun permanentlyDeletingArchivedHabitAlsoDeletesItsCompletions() = runBlocking {
        database = AppDatabase.create(context)
        val habit = database!!.habitDao().observeHabits().first().first()
        database!!.habitDao().complete(habit.id, "2026-09-21")

        database!!.habitDao().archive(habit.id, "2026-09-21")
        database!!.habitDao().deleteArchived(habit.id)

        assertEquals(emptyList<HabitCompletion>(), database!!.habitDao().observeCompletions().first())
    }

    @Test
    fun togglingCompletionTwiceReturnsToIncomplete() = runBlocking {
        database = AppDatabase.create(context)
        val habit = database!!.habitDao().observeHabits().first().first()

        database!!.habitDao().toggleCompletion(habit.id, "2026-09-21")
        database!!.habitDao().toggleCompletion(habit.id, "2026-09-21")

        assertEquals(emptyList<HabitCompletion>(), database!!.habitDao().observeCompletions().first())
    }

    @Test
    fun archivePreservesHistoryAndBlocksCompletionChanges() = runBlocking {
        database = AppDatabase.create(context)
        val habit = database!!.habitDao().observeHabits().first().first()
        database!!.habitDao().complete(habit.id, "2026-09-20")

        database!!.habitDao().archive(habit.id, "2026-09-21")
        database!!.habitDao().toggleCompletion(habit.id, "2026-09-21")

        assertEquals(false, database!!.habitDao().observeHabits().first().any { it.id == habit.id })
        assertEquals(listOf(habit.id), database!!.habitDao().observeArchivedHabits().first().map { it.id })
        assertEquals(listOf("2026-09-20"), database!!.habitDao().observeCompletions().first().map { it.date })
    }

    @Test
    fun restoreMakesHabitActiveAndSetsStreakBoundary() = runBlocking {
        database = AppDatabase.create(context)
        val habit = database!!.habitDao().observeHabits().first().first()
        database!!.habitDao().archive(habit.id, "2026-09-20")

        database!!.habitDao().restore(habit.id, "2026-09-21")

        val restored = database!!.habitDao().observeHabits().first().first { it.id == habit.id }
        assertEquals(null, restored.archivedOn)
        assertEquals("2026-09-21", restored.streakResetOn)
        assertEquals(emptyList<Habit>(), database!!.habitDao().observeArchivedHabits().first())
        val period = database!!.habitDao().observeArchivePeriods().first().single { it.habitId == habit.id }
        assertEquals("2026-09-20", period.archivedOn)
        assertEquals("2026-09-21", period.restoredOn)
    }

    @Test
    fun activeHabitCannotBePermanentlyDeleted() = runBlocking {
        database = AppDatabase.create(context)
        val habit = database!!.habitDao().observeHabits().first().first()

        val deleted = database!!.habitDao().deleteArchived(habit.id)

        assertEquals(0, deleted)
        assertEquals(true, database!!.habitDao().observeHabits().first().any { it.id == habit.id })
    }
}
