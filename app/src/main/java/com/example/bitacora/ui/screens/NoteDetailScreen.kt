package com.example.bitacora.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bitacora.data.Attachment
import com.example.bitacora.data.AttachmentType
import com.example.bitacora.data.Note
import com.example.bitacora.ui.formatDate
import com.example.bitacora.ui.readingMinutes
import com.example.bitacora.ui.theme.noteAccent
import com.example.bitacora.ui.wordCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    note: Note?,
    attachments: List<Attachment>,
    isDark: Boolean,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    attachmentFile: (String) -> File,
    attachmentUri: (String) -> Uri,
) {
    if (note == null) {
        MissingNoteScreen(onBack = onBack)
        return
    }

    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var previewAttachment by remember { mutableStateOf<Attachment?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { onTogglePin(note) }) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = if (note.pinned) "Quitar de destacadas" else "Destacar",
                            tint = if (note.pinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(onClick = { shareNote(context, note) }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Compartir")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onEdit(note.id)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val accent = noteAccent(note.colorIndex, isDark)

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(accent.container),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = note.emoji, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = note.title.ifBlank { "Sin título" },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = buildString {
                        append(formatDate(note.updatedAt))
                        append(" · ")
                        append("${readingMinutes(note.content)} min de lectura")
                        append(" · ")
                        append("${wordCount(note.content)} palabras")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(20.dp))

            SelectionContainer {
                Text(
                    text = note.content.ifBlank { "Esta nota todavía está vacía. Pulsa «Editar» para escribir algo." },
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 30.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Adjuntos",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(10.dp))
                attachments.forEach { att ->
                    AttachmentItem(
                        attachment = att,
                        file = attachmentFile(att.path),
                        onClick = {
                            if (att.type == AttachmentType.VIDEO) {
                                openUri(context, attachmentUri(att.path), "video/*")
                            } else {
                                previewAttachment = att
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onEdit(note.id) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar nota")
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    previewAttachment?.let { att ->
        ImagePreviewDialog(
            file = attachmentFile(att.path),
            onDismiss = { previewAttachment = null },
            onOpenExternal = {
                openUri(context, attachmentUri(att.path), "image/*")
                previewAttachment = null
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar nota") },
            text = {
                Text("«${note.title.ifBlank { "Sin título" }}» se borrará para siempre.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete(note)
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun AttachmentItem(attachment: Attachment, file: File, onClick: () -> Unit) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, file.path, attachment.type) {
        value = withContext(Dispatchers.IO) {
            if (attachment.type == AttachmentType.VIDEO) null
            else runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Adjunto",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Toca para reproducir el vídeo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewDialog(file: File, onDismiss: () -> Unit, onOpenExternal: () -> Unit) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, file.path) {
        value = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull()
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Imagen",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    TextButton(onClick = onOpenExternal) { Text("Abrir") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingNoteScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "🕳️", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Esta nota ya no existe",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onBack) { Text("Volver a la bitácora") }
        }
    }
}

private fun openUri(context: Context, uri: Uri, mime: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}

private fun shareNote(context: Context, note: Note) {
    val text = buildString {
        appendLine(note.title.ifBlank { "Sin título" })
        appendLine()
        appendLine(note.content)
        append("Compartido desde Bitácora")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, note.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Compartir nota"))
}
