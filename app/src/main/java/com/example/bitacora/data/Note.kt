package com.example.bitacora.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una entrada de la bitácora. Se guarda en SQLite a través de Room.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val emoji: String = "📝",
    val colorIndex: Int = 0,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
