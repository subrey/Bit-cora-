package com.example.jarvis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TodoItem::class,
        MemoryItem::class,
        TimerItem::class,
        ChatEntry::class,
        ReminderItem::class,
        RoutineItem::class,
        AlarmItem::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class JarvisDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao

    abstract fun memoryDao(): MemoryDao

    abstract fun timerDao(): TimerDao

    abstract fun chatDao(): ChatDao

    abstract fun reminderDao(): ReminderDao

    abstract fun routineDao(): RoutineDao

    abstract fun alarmDao(): AlarmDao

    companion object {
        private const val DB_NAME = "jarvis.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        text TEXT NOT NULL,
                        fromUser INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Fase 5: tabla de recordatorios con fecha y hora exactas. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        text TEXT NOT NULL,
                        triggerAt INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        done INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Fase 9: tabla de rutinas (disparador → acción). */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS routines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trigger TEXT NOT NULL,
                        triggerLabel TEXT NOT NULL,
                        action TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        enabled INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Fase 10: las tareas pueden tener fecha y hora. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN dueAt INTEGER")
            }
        }

        /** Fase 11: alarmas del despertador. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS alarms (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        repeatDaily INTEGER NOT NULL,
                        enabled INTEGER NOT NULL,
                        triggerAtMillis INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instance: JarvisDatabase? = null

        fun get(context: Context): JarvisDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { instance = it }
            }
    }
}
