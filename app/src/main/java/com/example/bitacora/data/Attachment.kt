package com.example.bitacora.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Archivo adjunto a una nota: imagen, vídeo o dibujo a mano.
 * [path] es el nombre del fichero dentro de filesDir/attachments/.
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("noteId")],
)
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val noteId: Long,
    val type: String,
    val path: String,
    val createdAt: Long = System.currentTimeMillis(),
)

object AttachmentType {
    const val IMAGE = "IMAGE"
    const val VIDEO = "VIDEO"
    const val DRAWING = "DRAWING"
}
