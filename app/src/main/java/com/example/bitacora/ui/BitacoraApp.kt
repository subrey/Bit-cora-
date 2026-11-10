package com.example.bitacora.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bitacora.ui.screens.NoteDetailScreen
import com.example.bitacora.ui.screens.NoteEditorScreen
import com.example.bitacora.ui.screens.NoteListScreen
import com.example.bitacora.ui.theme.BitacoraTheme

@Composable
fun BitacoraApp(
    newNoteRequest: Boolean = false,
    onNewNoteHandled: () -> Unit = {},
) {
    val viewModel: NotesViewModel = viewModel()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    BitacoraTheme(darkTheme = isDark) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "list") {
            composable("list") {
                NoteListScreen(
                    notes = notes,
                    query = query,
                    onQueryChange = viewModel::setQuery,
                    isDark = isDark,
                    onToggleTheme = viewModel::toggleDarkTheme,
                    onOpen = { note -> navController.navigate("note/${note.id}") },
                    onNewNote = { navController.navigate("editor?id=0") },
                    onTogglePin = viewModel::togglePin,
                    onDelete = viewModel::delete,
                )
            }

            composable(
                route = "note/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val note = allNotes.firstOrNull { it.id == id }
                val attachments by viewModel.attachments(id)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                NoteDetailScreen(
                    note = note,
                    attachments = attachments,
                    isDark = isDark,
                    onBack = { navController.popBackStack() },
                    onEdit = { noteId -> navController.navigate("editor?id=$noteId") },
                    onDelete = { noteToDelete ->
                        viewModel.delete(noteToDelete)
                        navController.popBackStack()
                    },
                    onTogglePin = viewModel::togglePin,
                    attachmentFile = viewModel::attachmentFile,
                    attachmentUri = viewModel::attachmentUri,
                )
            }

            composable(
                route = "editor?id={id}",
                arguments = listOf(
                    navArgument("id") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val note = if (id == 0L) null else allNotes.firstOrNull { it.id == id }
                val attachments by viewModel.attachments(id)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                if (id != 0L && note == null) {
                    LoadingScreen()
                } else {
                    NoteEditorScreen(
                        note = note,
                        attachments = attachments,
                        isDark = isDark,
                        onBack = { navController.popBackStack() },
                        onSave = { noteId, title, content, emoji, colorIndex, images, videos, drawings, onSaved ->
                            viewModel.saveNote(
                                noteId, title, content, emoji, colorIndex,
                                pendingImages = images,
                                pendingVideos = videos,
                                pendingDrawings = drawings,
                                onSaved = onSaved,
                            )
                        },
                        onAddImage = { uri -> viewModel.addAttachmentFromUri(id, "IMAGE", uri) },
                        onAddVideo = { uri -> viewModel.addAttachmentFromUri(id, "VIDEO", uri) },
                        onAddDrawing = { bitmap -> viewModel.addDrawing(id, bitmap) },
                        onDeleteAttachment = viewModel::deleteAttachment,
                        attachmentFile = viewModel::attachmentFile,
                        attachmentUri = viewModel::attachmentUri,
                    )
                }
            }
        }

        LaunchedEffect(newNoteRequest) {
            if (newNoteRequest) {
                navController.navigate("editor?id=0") { launchSingleTop = true }
                onNewNoteHandled()
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
