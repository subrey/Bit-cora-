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

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
            items(items = messages, key = { it.id }) { message ->
                TerminalMessage(message)
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
private fun TerminalMessage(message: ChatMessage) {
    var revealed by remember(message.id) { mutableStateOf(if (message.fromUser) message.text.length else 0) }

    LaunchedEffect(message.id) {
        if (!message.fromUser) {
            var index = 0
            while (index < message.text.length) {
                index = (index + 3).coerceAtMost(message.text.length)
                revealed = index
                delay(14)
            }
        }
    }

    var cursorVisible by remember(message.id) { mutableStateOf(true) }
    LaunchedEffect(message.id) {
        while (true) {
            cursorVisible = !cursorVisible
            delay(450)
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
            text = message.text.take(revealed) + if (typing || cursorVisible) "▌" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = if (message.fromUser) JarvisColors.Muted else JarvisColors.Green,
        )
    }
}
