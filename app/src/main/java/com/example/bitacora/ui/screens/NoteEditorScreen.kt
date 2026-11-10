package com.example.bitacora.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bitacora.data.Note
import com.example.bitacora.ui.theme.noteAccent
import com.example.bitacora.ui.theme.noteEmojis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note?,
    isDark: Boolean,
    onBack: () -> Unit,
    onSave: (id: Long, title: String, content: String, emoji: String, colorIndex: Int) -> Unit,
) {
    val noteId = note?.id ?: 0L

    var title by rememberSaveable(noteId) { mutableStateOf(note?.title.orEmpty()) }
    var content by rememberSaveable(noteId) { mutableStateOf(note?.content.orEmpty()) }
    var emoji by rememberSaveable(noteId) { mutableStateOf(note?.emoji ?: noteEmojis.first()) }
    var colorIndex by rememberSaveable(noteId) { mutableStateOf(note?.colorIndex ?: 0) }
    var confirmExit by remember { mutableStateOf(false) }

    val dirty = when {
        note == null -> title.isNotBlank() || content.isNotBlank() ||
            emoji != noteEmojis.first() || colorIndex != 0
        else -> title != note.title || content != note.content ||
            emoji != note.emoji || colorIndex != note.colorIndex
    }

    val canSave = title.isNotBlank() || content.isNotBlank()

    fun performSave() {
        onSave(
            noteId,
            title.trim().ifBlank { "Sin título" },
            content.trimEnd(),
            emoji,
            colorIndex,
        )
    }

    fun attemptExit() {
        if (dirty) confirmExit = true else onBack()
    }

    BackHandler(enabled = dirty) { confirmExit = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = if (noteId == 0L) "Nueva nota" else "Editar nota",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { attemptExit() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            performSave()
                            onBack()
                        },
                        enabled = canSave,
                    ) {
                        Text("Guardar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val accent = noteAccent(colorIndex, isDark)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji, fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(items = noteEmojis, key = { it }) { item ->
                            EmojiChip(
                                emoji = item,
                                selected = item == emoji,
                                onClick = { emoji = item },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(6) { index ->
                            ColorDot(
                                index = index,
                                isDark = isDark,
                                selected = index == colorIndex,
                                onClick = { colorIndex = index },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val transparentFieldColors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            )

            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Título de la nota",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall,
                colors = transparentFieldColors,
                maxLines = 4,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Escribe aquí lo que quieras recordar…") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                colors = transparentFieldColors,
            )
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("¿Descartar cambios?") },
            text = { Text("Los cambios que escribiste no se guardarán.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmExit = false
                        onBack()
                    }
                ) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("Seguir editando") }
            },
        )
    }
}

@Composable
private fun EmojiChip(emoji: String, selected: Boolean, onClick: () -> Unit) {
    val chipColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(chipColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 17.sp)
    }
}

@Composable
private fun ColorDot(index: Int, isDark: Boolean, selected: Boolean, onClick: () -> Unit) {
    val accent = noteAccent(index, isDark)
    Spacer(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(accent.container)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}
