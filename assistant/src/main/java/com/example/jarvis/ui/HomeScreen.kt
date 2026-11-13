package com.example.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.domain.model.AssistantState
import com.example.jarvis.domain.model.ChatMessage
import com.example.jarvis.ui.components.HoloPanel
import com.example.jarvis.ui.components.JarvisCore

/**
 * Pantalla principal: el núcleo de JARVIS, el botón de voz, el campo de texto
 * y un resumen de la conversación. El historial completo y los ajustes están
 * a un toque en la barra inferior.
 */
@Composable
fun HomeScreen(
    state: AssistantState,
    messages: List<ChatMessage>,
    onSend: (String) -> Unit,
    onMicClick: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current

    fun send() {
        val clean = input.trim()
        if (clean.isEmpty()) return
        input = ""
        focus.clearFocus()
        onSend(clean)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            StatePill(state)
            Spacer(modifier = Modifier.height(6.dp))

            val interaction = remember { MutableInteractionSource() }
            JarvisCore(
                state = state,
                modifier = Modifier
                    .fillMaxWidth(0.74f)
                    .widthIn(max = 320.dp)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onMicClick,
                    ),
            )

            Text(
                text = "J.A.R.V.I.S",
                style = MaterialTheme.typography.titleLarge,
                color = JarvisColors.Ice,
                letterSpacing = 8.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stateHint(state),
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisColors.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(
                    icon = Icons.Rounded.History,
                    label = "HISTORIAL",
                    onClick = onOpenHistory,
                )
                QuickAction(
                    icon = Icons.Rounded.Settings,
                    label = "AJUSTES",
                    onClick = onOpenSettings,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            ConversationPreview(messages)
            Spacer(modifier = Modifier.height(16.dp))
        }

        CommandBar(
            input = input,
            onInputChange = { input = it },
            onSend = ::send,
            onMicClick = onMicClick,
            listening = state == AssistantState.LISTENING,
        )
    }
}

@Composable
private fun CommandBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    listening: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "Escriba o hable…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisColors.Muted,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = JarvisColors.Text),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            trailingIcon = {
                IconButton(onClick = onSend, enabled = input.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = "Enviar",
                        tint = if (input.isNotBlank()) JarvisColors.Cyan else JarvisColors.Outline,
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisColors.Cyan.copy(alpha = 0.65f),
                unfocusedBorderColor = JarvisColors.Outline,
                focusedContainerColor = JarvisColors.Panel.copy(alpha = 0.65f),
                unfocusedContainerColor = JarvisColors.Panel.copy(alpha = 0.35f),
                cursorColor = JarvisColors.Cyan,
                focusedTextColor = JarvisColors.Text,
                unfocusedTextColor = JarvisColors.Text,
            ),
        )

        Spacer(modifier = Modifier.width(10.dp))

        val micShape = CircleShape
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(micShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            JarvisColors.Primary.copy(alpha = 0.35f),
                            JarvisColors.Cyan.copy(alpha = 0.20f),
                        ),
                    ),
                )
                .border(
                    width = if (listening) 2.dp else 1.dp,
                    color = if (listening) JarvisColors.Cyan else JarvisColors.Primary.copy(alpha = 0.55f),
                    shape = micShape,
                )
                .clickable(onClick = onMicClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = "Hablar",
                tint = if (listening) JarvisColors.Cyan else JarvisColors.Ice,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun StatePill(state: AssistantState) {
    val color = when (state) {
        AssistantState.IDLE -> JarvisColors.Primary
        AssistantState.LISTENING -> JarvisColors.Cyan
        AssistantState.THINKING -> JarvisColors.Primary
        AssistantState.SPEAKING -> JarvisColors.Ice
        AssistantState.ERROR -> JarvisColors.Red
    }
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.45f), shape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = state.label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(JarvisColors.Panel.copy(alpha = 0.45f))
            .border(1.dp, JarvisColors.Outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = JarvisColors.Muted,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = JarvisColors.Muted)
    }
}

@Composable
private fun ConversationPreview(messages: List<ChatMessage>) {
    val recent = messages.takeLast(3)
    HoloPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CONVERSACIÓN",
            style = MaterialTheme.typography.labelSmall,
            color = JarvisColors.Muted,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (recent.isEmpty()) {
            Text(
                text = "Diga «Jarvis, pon música» o pregúnteme cualquier cosa.",
                style = MaterialTheme.typography.bodySmall,
                color = JarvisColors.Muted,
            )
        } else {
            recent.forEachIndexed { index, message ->
                val prefix = if (message.fromUser) "TÚ" else "JARVIS"
                val tint = if (message.fromUser) JarvisColors.Cyan else JarvisColors.Primary
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = prefix,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        modifier = Modifier.width(52.dp),
                    )
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (message.fromUser) JarvisColors.Muted else JarvisColors.Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (index != recent.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

private fun stateHint(state: AssistantState): String = when (state) {
    AssistantState.IDLE -> "Sistemas preparados. Pulse el núcleo o el micrófono."
    AssistantState.LISTENING -> "Le escucho… hable con naturalidad."
    AssistantState.THINKING -> "Procesando su orden…"
    AssistantState.SPEAKING -> "Respondiendo…"
    AssistantState.ERROR -> "Algo ha fallado. Inténtelo de nuevo."
}
