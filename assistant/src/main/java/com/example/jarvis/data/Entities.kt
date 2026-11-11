package com.example.jarvis.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tarea pendiente de la lista de cosas que hacer. */
@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Recuerdo o nota breve memorizada por el asistente. */
@Entity(tableName = "memories")
data class MemoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Temporizador o recordatorio programado. */
@Entity(tableName = "timers")
data class TimerItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val durationSeconds: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val fired: Boolean = false,
)
