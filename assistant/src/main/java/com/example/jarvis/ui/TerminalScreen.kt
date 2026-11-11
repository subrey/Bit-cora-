package com.example.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TerminalScreen(
    messages: List<ChatMessage>,
    listening: Boolean,
    onSend: (String) -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Con conversaciones largas solo se pintan los últimos mensajes: el
    // resto sigue guardado en la base de datos y no consume recursos.
    val visible = remember(messages) { messages.takeLast(MAX_VISIBLE_MESSAGES) }
    val hidden = messages.size - visible.size

    LaunchedEffect(visible.size) {
        if (visible.isNotEmpty()) {
            listState.animateScrollToItem(visible.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (hidden > 0) {
                item(key = "hidden") {
                    Text(
                        text = "… $hidden mensajes anteriores (guardados en el histórico)",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisColors.Outline,
                    )
                }
            }
            items(items = visible, key = { it.id }) { message ->
                TerminalMessage(
                    message = message,
                    isLast = message.id == visible.lastOrNull()?.id,
                )
            }
        }

        HorizontalDivider(color = JarvisColors.Outline)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMicClick) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Hablar",
                    tint = if (listening) JarvisColors.Amber else JarvisColors.Green,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Escribe una orden…", color = JarvisColors.Muted)
                },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = JarvisColors.Green),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisColors.Green,
                    unfocusedBorderColor = JarvisColors.Outline,
                    focusedTextColor = JarvisColors.Green,
                    unfocusedTextColor = JarvisColors.Green,
                    cursorColor = JarvisColors.Green,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = {
                    val clean = input.trim()
                    if (clean.isNotEmpty()) {
                        onSend(clean)
                        input = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = "Enviar",
                    tint = JarvisColors.Green,
                )
            }
        }
    }
}

@Composable
private fun TerminalMessage(message: ChatMessage, isLast: Boolean) {
    // Solo el último mensaje se escribe letra a letra y parpadea: así el hilo
    // principal no se satura y el reconocimiento de voz sigue respondiendo.
    var revealed by remember(message.id) {
        mutableStateOf(if (message.fromUser || !isLast) message.text.length else 0)
    }
    var cursorVisible by remember(message.id) { mutableStateOf(true) }

    LaunchedEffect(message.id, isLast) {
        if (isLast && !message.fromUser) {
            var index = 0
            while (index < message.text.length) {
                index = (index + 4).coerceAtMost(message.text.length)
                revealed = index
                delay(16)
            }
        }
    }

    LaunchedEffect(message.id, isLast) {
        if (isLast) {
            while (true) {
                cursorVisible = !cursorVisible
                delay(500)
            }
        }
    }

    val typing = revealed < message.text.length
    val prefix = if (message.fromUser) "> " else "JARVIS: "
    val prefixColor = if (message.fromUser) JarvisColors.Cyan else JarvisColors.GreenDim

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = prefix,
            style = MaterialTheme.typography.bodyMedium,
            color = prefixColor,
        )
        Text(
            text = message.text.take(revealed) +
                if (isLast && (typing || cursorVisible)) "▌" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = if (message.fromUser) JarvisColors.Muted else JarvisColors.Green,
        )
    }
}

/** Máximo de mensajes pintados a la vez en el terminal. */
private const val MAX_VISIBLE_MESSAGES = 120
