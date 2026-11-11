package com.example.jarvis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TodoItem::class, MemoryItem::class, TimerItem::class, ChatEntry::class],
    version = 2,
    exportSchema = false,
)
abstract class JarvisDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao

    abstract fun memoryDao(): MemoryDao

    abstract fun timerDao(): TimerDao

    abstract fun chatDao(): ChatDao

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

        @Volatile
        private var instance: JarvisDatabase? = null

        fun get(context: Context): JarvisDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
