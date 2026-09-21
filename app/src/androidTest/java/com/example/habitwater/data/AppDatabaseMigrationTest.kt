package com.example.habitwater.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @After
    fun cleanUp() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate1To2PreservesDataAndAddsActiveArchiveFields() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO habits (id, name, daysMask, reminderHour, reminderMinute, createdAt) VALUES (10, '運動', 127, NULL, NULL, 1)",
            )
            execSQL("INSERT INTO habits (id, name, daysMask, reminderHour, reminderMinute, createdAt) VALUES (11, '冥想', 127, NULL, NULL, 2)")
            execSQL("INSERT INTO habits (id, name, daysMask, reminderHour, reminderMinute, createdAt) VALUES (12, '閱讀', 127, 8, 30, 3)")
            execSQL("INSERT INTO habit_completions (habitId, date) VALUES (12, '2026-09-21')")
            execSQL("INSERT INTO water_entries (id, amountMl, recordedAt) VALUES (20, 250, 1)")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2).apply {
            query("SELECT name, archivedOn, streakResetOn FROM habits ORDER BY id").use { cursor ->
                val names = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    names += cursor.getString(0)
                    assertEquals(true, cursor.isNull(1))
                    assertEquals(true, cursor.isNull(2))
                }
                assertEquals(listOf("運動", "冥想", "閱讀"), names)
            }
            query("SELECT COUNT(*) FROM habit_archive_periods").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            query("SELECT COUNT(*) FROM habit_completions WHERE habitId = 12").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            query("SELECT COUNT(*) FROM water_entries WHERE id = 20").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
