package com.example.jarvis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TodoItem::class, MemoryItem::class, TimerItem::class],
    version = 1,
    exportSchema = false,
)
abstract class JarvisDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao

    abstract fun memoryDao(): MemoryDao

    abstract fun timerDao(): TimerDao

    companion object {
        private const val DB_NAME = "jarvis.db"

        @Volatile
        private var instance: JarvisDatabase? = null

        fun get(context: Context): JarvisDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
    }
}
