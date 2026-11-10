package com.example.bitacora.data

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val dao: NoteDao,
    private val attachmentDao: AttachmentDao,
    private val mediaStore: MediaStore,
) {

    fun observeAll(): Flow<List<Note>> = dao.observeAll()

    fun search(query: String): Flow<List<Note>> = dao.search(query)

    fun observeById(id: Long): Flow<Note?> = dao.observeById(id)

    fun observeAttachments(noteId: Long): Flow<List<Attachment>> = attachmentDao.observeByNote(noteId)

    suspend fun getById(id: Long): Note? = dao.getById(id)

    suspend fun getAttachments(noteId: Long): List<Attachment> = attachmentDao.getByNote(noteId)

    /** Nota que muestra el widget: la destacada más reciente o, si no hay, la última editada. */
    suspend fun latestNote(): Note? = dao.latest()

    suspend fun insert(note: Note): Long = dao.insert(note)

    suspend fun update(note: Note) = dao.update(note)

    suspend fun delete(note: Note) {
        val attachments = attachmentDao.getByNote(note.id)
        attachments.forEach { mediaStore.delete(it.path) }
        dao.delete(note) // CASCADE elimina filas de adjuntos
    }

    suspend fun deleteAll() {
        // elimina ficheros de todos los adjuntos conocidos
        // (no hay consulta global; basta con borrar carpeta si existiera, pero por seguridad)
        dao.deleteAll()
    }

    suspend fun addAttachmentFromUri(noteId: Long, type: String, uri: Uri): Attachment {
        val fallback = when (type) {
            AttachmentType.VIDEO -> "mp4"
            else -> "jpg"
        }
        val path = mediaStore.importFromUri(uri, fallback)
        val attachment = Attachment(noteId = noteId, type = type, path = path)
        val id = attachmentDao.insert(attachment)
        return attachment.copy(id = id)
    }

    suspend fun addDrawing(noteId: Long, bitmap: Bitmap): Attachment {
        val path = mediaStore.saveBitmap(bitmap)
        val attachment = Attachment(noteId = noteId, type = AttachmentType.DRAWING, path = path)
        val id = attachmentDao.insert(attachment)
        return attachment.copy(id = id)
    }

    suspend fun deleteAttachment(attachment: Attachment) {
        mediaStore.delete(attachment.path)
        attachmentDao.delete(attachment)
    }

    fun mediaFile(path: String) = mediaStore.file(path)

    fun mediaUri(path: String) = mediaStore.contentUri(path)

    /**
     * Si la base de datos está vacía (primer arranque), añade unas notas de ejemplo.
     */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        dao.insertAll(
            listOf(
                Note(
                    title = "Bienvenida a tu bitácora",
                    content = "Esta es tu bitácora personal: un blog privado para guardar lo que se te ocurra.\n\n" +
                        "• Toca «Nueva nota» para escribir.\n" +
                        "• Usa el menú «⋮» de cada nota para destacarla o borrarla.\n" +
                        "• Usa la lupa para buscar entre todas tus notas.\n" +
                        "• El icono de la luna cambia entre tema claro y oscuro.\n" +
                        "• Añade imágenes, vídeos o escribe a mano desde el editor.\n\n" +
                        "Todo se guarda únicamente en tu teléfono.",
                    emoji = "📖",
                    colorIndex = 4,
                    pinned = true,
                    createdAt = now - 2 * day,
                    updatedAt = now - 2 * day
                ),
                Note(
                    title = "Ideas para esta semana",
                    content = "• Probar esa receta de pasta con limón.\n" +
                        "• Apuntarme al taller de fotografía del sábado.\n" +
                        "• Terminar el libro que empecé en el tren.\n" +
                        "• Llamar a Marta y quedar para el café del jueves.",
                    emoji = "💡",
                    colorIndex = 1,
                    createdAt = now - day,
                    updatedAt = now - day
                ),
                Note(
                    title = "Café frío de verano",
                    content = "Ingredientes:\n" +
                        "• 2 cucharadas de café molido\n" +
                        "• 200 ml de agua fría\n" +
                        "• Hielo y un chorrito de leche\n\n" +
                        "Mezclar el café con el agua y dejar reposar en la nevera 12 horas. " +
                        "Colar, servir sobre hielo y añadir la leche al gusto.",
                    emoji = "☕",
                    colorIndex = 0,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )
    }
}
