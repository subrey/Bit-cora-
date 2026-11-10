package com.example.bitacora.ui

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitacora.NotasApplication
import com.example.bitacora.data.Attachment
import com.example.bitacora.data.AttachmentType
import com.example.bitacora.data.Note
import com.example.bitacora.widget.BitacoraWidgetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotasApplication
    private val repository = app.repository
    private val settings = app.settings

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) repository.observeAll() else repository.search(q.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Todas las notas, sin filtrar: la usan la vista de nota y el editor. */
    val allNotes: StateFlow<List<Note>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** "system", "light" o "dark". */
    val themeMode: StateFlow<String> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    fun setQuery(value: String) {
        _query.value = value
    }

    fun attachments(noteId: Long): kotlinx.coroutines.flow.Flow<List<Attachment>> =
        if (noteId == 0L) flowOf(emptyList()) else repository.observeAttachments(noteId)

    fun saveNote(
        id: Long,
        title: String,
        content: String,
        emoji: String,
        colorIndex: Int,
        pendingImages: List<Uri> = emptyList(),
        pendingVideos: List<Uri> = emptyList(),
        pendingDrawings: List<Bitmap> = emptyList(),
        onSaved: (Long) -> Unit = {},
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val savedId = if (id == 0L) {
                repository.insert(
                    Note(
                        title = title,
                        content = content,
                        emoji = emoji,
                        colorIndex = colorIndex,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            } else {
                val current = repository.getById(id)
                if (current == null) {
                    onSaved(id)
                    return@launch
                }
                repository.update(
                    current.copy(
                        title = title,
                        content = content,
                        emoji = emoji,
                        colorIndex = colorIndex,
                        updatedAt = now,
                    )
                )
                id
            }

            pendingImages.forEach { uri ->
                runCatching { repository.addAttachmentFromUri(savedId, AttachmentType.IMAGE, uri) }
            }
            pendingVideos.forEach { uri ->
                runCatching { repository.addAttachmentFromUri(savedId, AttachmentType.VIDEO, uri) }
            }
            pendingDrawings.forEach { bitmap ->
                runCatching { repository.addDrawing(savedId, bitmap) }
            }

            refreshWidget()
            onSaved(savedId)
        }
    }

    fun attachmentFile(path: String): File = repository.mediaFile(path)

    fun attachmentUri(path: String): Uri = repository.mediaUri(path)

    fun delete(note: Note) {
        viewModelScope.launch {
            repository.delete(note)
            refreshWidget()
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.update(note.copy(pinned = !note.pinned))
            refreshWidget()
        }
    }

    fun addAttachmentFromUri(noteId: Long, type: String, uri: Uri, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.addAttachmentFromUri(noteId, type, uri) }
            refreshWidget()
            onDone()
        }
    }

    fun addDrawing(noteId: Long, bitmap: Bitmap, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.addDrawing(noteId, bitmap) }
            refreshWidget()
            onDone()
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            repository.deleteAttachment(attachment)
            refreshWidget()
        }
    }

    fun toggleDarkTheme() {
        viewModelScope.launch {
            val current = themeMode.value
            val next = when {
                current == "dark" -> "light"
                current == "light" -> "dark"
                systemIsDark() -> "light"
                else -> "dark"
            }
            settings.setThemeMode(next)
        }
    }

    private fun systemIsDark(): Boolean {
        val mode = getApplication<Application>().resources.configuration.uiMode
        return (mode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun refreshWidget() {
        BitacoraWidgetProvider.updateAll(getApplication())
    }
}
