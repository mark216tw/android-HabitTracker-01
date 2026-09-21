package com.example.habitwater.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Habit::class, HabitArchivePeriod::class, HabitCompletion::class, WaterEntry::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun waterDao(): WaterDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN archivedOn TEXT")
                db.execSQL("ALTER TABLE habits ADD COLUMN streakResetOn TEXT")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS habit_archive_periods (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        habitId INTEGER NOT NULL,
                        archivedOn TEXT NOT NULL,
                        restoredOn TEXT,
                        FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_archive_periods_habitId ON habit_archive_periods (habitId)")
            }
        }

        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "habit-water.db",
        ).addMigrations(MIGRATION_1_2).addCallback(
            object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val now = System.currentTimeMillis()
                    listOf("運動", "冥想", "閱讀").forEachIndexed { index, name ->
                        db.execSQL(
                            "INSERT INTO habits (name, daysMask, reminderHour, reminderMinute, createdAt) VALUES (?, ?, NULL, NULL, ?)",
                            arrayOf<Any>(name, EVERY_DAY_MASK, now + index),
                        )
                    }
                }
            },
        ).build()
    }
}
