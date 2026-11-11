package com.example.jarvis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

@Composable
fun SettingsScreen(
    ttsEnabled: Boolean,
    alwaysListening: Boolean,
    wakeWord: String,
    onDeviceVoice: Boolean,
    onTtsChange: (Boolean) -> Unit,
    onAlwaysListeningChange: (Boolean) -> Unit,
    onWakeWordChange: (String) -> Unit,
    onOnDeviceVoiceChange: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var wordInput by remember(wakeWord) { mutableStateOf(wakeWord) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
    ) {
        Text(text = "CONFIGURACIÓN", style = MaterialTheme.typography.labelMedium, color = JarvisColors.GreenDim)
        Spacer(modifier = Modifier.height(10.dp))

        SettingSwitch(
            title = "Respuestas habladas",
            subtitle = "Jarvis lee sus respuestas en voz alta",
            checked = ttsEnabled,
            onCheckedChange = onTtsChange,
        )
        SettingSwitch(
            title = "Escucha permanente",
            subtitle = "Mantiene el micrófono abierto y espera la palabra clave (pide permiso de micrófono)",
            checked = alwaysListening,
            onCheckedChange = onAlwaysListeningChange,
        )
        SettingSwitch(
            title = "Voz en el dispositivo",
            subtitle = "Sin red ni pitidos, pero necesita el modelo de voz descargado " +
                "(Ajustes → Google → Búsqueda por voz → Idiomas descargados)",
            checked = onDeviceVoice,
            onCheckedChange = onOnDeviceVoiceChange,
        )

        HorizontalDivider(color = JarvisColors.Outline)
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "PALABRA CLAVE",
            style = MaterialTheme.typography.labelMedium,
            color = JarvisColors.GreenDim,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = wordInput,
            onValueChange = { wordInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
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
        )
        TextButton(onClick = {
            val clean = wordInput.trim()
            if (clean.isNotEmpty()) onWakeWordChange(clean)
        }) {
            Text("Guardar palabra clave", color = JarvisColors.Green)
        }

        HorizontalDivider(color = JarvisColors.Outline)
        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "ORDENES DISPONIBLES", style = MaterialTheme.typography.labelMedium, color = JarvisColors.GreenDim)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = """
                • «pon música», «pausa», «siguiente canción», «sube el volumen»
                • «pon un temporizador de 10 minutos», «avisame en 5 minutos»
                • «calcula 12 * (3 + 4)», «convierte 10 km a millas»
                • «añade tarea comprar leche», «memoriza el número del hotel»
                • «qué hora es», «qué día es», «batería», «almacenamiento»
                • «enciende la linterna», «abre la cámara», «resumen», «ayuda»
            """.trimIndent(),
            style = MaterialTheme.typography.bodySmall,
            color = JarvisColors.Muted,
        )

        HorizontalDivider(color = JarvisColors.Outline)
        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = onClearChat) {
            Text("Reiniciar conversación", color = JarvisColors.Amber)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "JARVIS · asistente local · v1.0",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisColors.Outline,
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = JarvisColors.Green)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = JarvisColors.Muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = JarvisColors.Green,
                checkedTrackColor = JarvisColors.GreenDim,
                uncheckedThumbColor = JarvisColors.Outline,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = JarvisColors.Outline,
            ),
        )
    }
}
