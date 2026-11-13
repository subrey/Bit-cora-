package com.example.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.jarvis.assistant.PlayerState
import com.example.jarvis.assistant.Track

@Composable
fun MusicScreen(
    state: PlayerState,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onVolume: (Float) -> Unit,
    onShuffle: (Boolean) -> Unit,
    onRepeat: (Boolean) -> Unit,
    onDemoTone: () -> Unit,
    onSelect: (Track) -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            
            .padding(horizontal = 14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(JarvisColors.Panel)
                .padding(14.dp),
        ) {
            Text(
                text = if (state.trackTitle.isBlank()) "— sin reproducción —" else state.trackTitle,
                style = MaterialTheme.typography.titleMedium,
                color = JarvisColors.Green,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.trackArtist.ifBlank { "Jarvis" },
                style = MaterialTheme.typography.bodySmall,
                color = JarvisColors.Muted,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Slider(
                value = if (state.durationMs > 0) {
                    state.positionMs.toFloat().coerceIn(0f, state.durationMs.toFloat())
                } else {
                    0f
                },
                onValueChange = { },
                valueRange = 0f..(if (state.durationMs > 0) state.durationMs.toFloat() else 1f),
                enabled = false,
                colors = SliderDefaults.colors(
                    disabledThumbColor = JarvisColors.Green,
                    disabledActiveTrackColor = JarvisColors.GreenDim,
                    disabledInactiveTrackColor = JarvisColors.Outline,
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onShuffle(!state.shuffle) }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Aleatorio",
                        tint = if (state.shuffle) JarvisColors.Green else JarvisColors.Outline,
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = JarvisColors.Green)
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                        tint = JarvisColors.Green,
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = JarvisColors.Green)
                }
                IconButton(onClick = { onRepeat(!state.repeat) }) {
                    Icon(
                        imageVector = if (state.repeat) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repetir",
                        tint = if (state.repeat) JarvisColors.Green else JarvisColors.Outline,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolumeDown, contentDescription = null, tint = JarvisColors.Muted)
                Slider(
                    value = state.volume,
                    onValueChange = onVolume,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisColors.Green,
                        activeTrackColor = JarvisColors.GreenDim,
                        inactiveTrackColor = JarvisColors.Outline,
                    ),
                )
                Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = JarvisColors.Muted)
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisColors.Amber,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDemoTone) { Text("Tono de prueba", color = JarvisColors.Cyan) }
            TextButton(onClick = onRequestPermission) { Text("Permiso de audio", color = JarvisColors.Cyan) }
        }

        Text(
            text = "BIBLIOTECA",
            style = MaterialTheme.typography.labelMedium,
            color = JarvisColors.GreenDim,
        )

        if (state.tracks.isEmpty()) {
            Text(
                text = "Sin pistas cargadas. Use «pon música» o conceda el permiso de audio.",
                style = MaterialTheme.typography.bodySmall,
                color = JarvisColors.Muted,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items = state.tracks, key = { it.id }) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(track) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = JarvisColors.GreenDim,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisColors.Green,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = JarvisColors.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
