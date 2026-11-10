package com.example.bitacora.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bitacora.data.Attachment
import com.example.bitacora.data.AttachmentType
import com.example.bitacora.data.Note
import com.example.bitacora.ui.components.HandwritingDialog
import com.example.bitacora.ui.theme.noteAccent
import com.example.bitacora.ui.theme.noteEmojis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note?,
    attachments: List<Attachment>,
    isDark: Boolean,
    onBack: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        content: String,
        emoji: String,
        colorIndex: Int,
        pendingImages: List<Uri>,
        pendingVideos: List<Uri>,
        pendingDrawings: List<Bitmap>,
        onSaved: (Long) -> Unit
    ) -> Unit,
    onAddImage: (Uri) -> Unit,
    onAddVideo: (Uri) -> Unit,
    onAddDrawing: (Bitmap) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    attachmentFile: (String) -> File,
    attachmentUri: (String) -> Uri,
) {
    val noteId = note?.id ?: 0L
    val context = LocalContext.current

    var title by rememberSaveable(noteId) { mutableStateOf(note?.title.orEmpty()) }
    var content by rememberSaveable(noteId) { mutableStateOf(note?.content.orEmpty()) }
    var emoji by rememberSaveable(noteId) { mutableStateOf(note?.emoji ?: noteEmojis.first()) }
    var colorIndex by rememberSaveable(noteId) { mutableStateOf(note?.colorIndex ?: 0) }
    var confirmExit by remember { mutableStateOf(false) }
    var showDrawing by remember { mutableStateOf(false) }
    var dictationTarget by remember { mutableStateOf<String?>(null) }

    val pendingImages = remember(noteId) { mutableStateListOf<Uri>() }
    val pendingVideos = remember(noteId) { mutableStateListOf<Uri>() }
    val pendingDrawings = remember(noteId) { mutableStateListOf<Bitmap>() }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()?.trim().orEmpty()
            if (text.isNotBlank()) {
                when (dictationTarget) {
                    "title" -> title = if (title.isBlank()) text else title + " " + text
                    "content" -> content = if (content.isBlank()) text else content + " " + text
                }
            }
        }
        dictationTarget = null
    }

    val dictate: (String) -> Unit = { target ->
        dictationTarget = target
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora…")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { speechLauncher.launch(intent) }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (noteId == 0L) pendingImages.add(uri) else onAddImage(uri)
        }
    }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (noteId == 0L) pendingVideos.add(uri) else onAddVideo(uri)
        }
    }

    val hasPending = pendingImages.isNotEmpty() || pendingVideos.isNotEmpty() || pendingDrawings.isNotEmpty()

    val dirty = when {
        note == null -> title.isNotBlank() || content.isNotBlank() ||
            emoji != noteEmojis.first() || colorIndex != 0 || hasPending
        else -> title != note.title || content != note.content ||
            emoji != note.emoji || colorIndex != note.colorIndex
    }

    val canSave = title.isNotBlank() || content.isNotBlank() || hasPending || attachments.isNotEmpty()

    fun performSave() {
        onSave(
            noteId,
            title.trim().ifBlank { "Sin título" },
            content.trimEnd(),
            emoji,
            colorIndex,
            pendingImages.toList(),
            pendingVideos.toList(),
            pendingDrawings.toList(),
        ) { onBack() }
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
                    TextButton(onClick = { performSave() }, enabled = canSave) {
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
                .verticalScroll(rememberScrollState())
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
                trailingIcon = {
                    IconButton(onClick = { dictate("title") }) {
                        Icon(Icons.Rounded.Mic, contentDescription = "Dictar título")
                    }
                },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { dictate("content") }) {
                    Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dictar")
                }
            }

            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                placeholder = { Text("Escribe aquí lo que quieras recordar…") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                colors = transparentFieldColors,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Adjuntos",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickImage.launch("image/*") }) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Imagen")
                }
                OutlinedButton(onClick = { pickVideo.launch("video/*") }) {
                    Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Vídeo")
                }
                OutlinedButton(onClick = { showDrawing = true }) {
                    Icon(Icons.Rounded.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mano")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (attachments.isNotEmpty() || hasPending) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items = attachments, key = { "att-${it.id}" }) { att ->
                        AttachmentThumb(
                            file = attachmentFile(att.path),
                            type = att.type,
                            isPending = false,
                            onRemove = { onDeleteAttachment(att) },
                            onOpen = {
                                val uri = attachmentUri(att.path)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, if (att.type == AttachmentType.VIDEO) "video/*" else "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                        )
                    }
                    items(items = pendingImages.toList(), key = { "pi-${it}" }) { uri ->
                        PendingImageThumb(
                            uri = uri,
                            label = "Imagen",
                            onRemove = { pendingImages.remove(uri) },
                        )
                    }
                    items(items = pendingVideos.toList(), key = { "pv-${it}" }) { uri ->
                        PendingVideoThumb(onRemove = { pendingVideos.remove(uri) })
                    }
                    items(items = pendingDrawings.toList(), key = { "pd-${System.identityHashCode(it)}" }) { bmp ->
                        PendingDrawingThumb(bitmap = bmp, onRemove = { pendingDrawings.remove(bmp) })
                    }
                }
            } else {
                Text(
                    text = "Puedes añadir imágenes, vídeos o escribir a mano.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDrawing) {
        HandwritingDialog(
            onDismiss = { showDrawing = false },
            onSave = { bitmap ->
                if (noteId == 0L) pendingDrawings.add(bitmap) else onAddDrawing(bitmap)
                showDrawing = false
            },
        )
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
private fun AttachmentThumb(
    file: File,
    type: String,
    isPending: Boolean,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, file.path, type) {
        value = withContext(Dispatchers.IO) {
            if (type == AttachmentType.VIDEO) null
            else runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(file.path, opts)
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Adjunto",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (type == AttachmentType.VIDEO) Icons.Rounded.PlayArrow else Icons.Rounded.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        RemoveBadge(modifier = Modifier.align(Alignment.TopEnd), onClick = onRemove)
    }
}

@Composable
private fun PendingImageThumb(uri: Uri, label: String, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeStream(input, null, opts)
                }
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        RemoveBadge(modifier = Modifier.align(Alignment.TopEnd), onClick = onRemove)
    }
}

@Composable
private fun PendingVideoThumb(onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.PlayArrow,
            contentDescription = "Vídeo",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(36.dp),
        )
        RemoveBadge(modifier = Modifier.align(Alignment.TopEnd), onClick = onRemove)
    }
}

@Composable
private fun PendingDrawingThumb(bitmap: Bitmap, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White),
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Dibujo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        RemoveBadge(modifier = Modifier.align(Alignment.TopEnd), onClick = onRemove)
    }
}

@Composable
private fun RemoveBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(Color(0xCC000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Close,
            contentDescription = "Quitar",
            tint = Color.White,
            modifier = Modifier.size(14.dp),
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
