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

/** Recordatorio programado para una fecha y hora concretas. */
@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    /** Momento exacto en milisegundos desde epoch en el que debe saltar. */
    val triggerAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
    /** true cuando ya ha saltado o el usuario lo ha marcado como hecho. */
    val done: Boolean = false,
)

/** Mensaje del histórico de conversación con el asistente. */
@Entity(tableName = "chat")
data class ChatEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val fromUser: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
)
