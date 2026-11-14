package com.example.jarvis.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    ttsEnabled: Boolean,
    alwaysListening: Boolean,
    continuousVoice: Boolean,
    wakeWord: String,
    onDeviceVoice: Boolean,
    aiConfigured: Boolean,
    aiModel: String,
    aiStatus: String?,
    city: String,
    onTtsChange: (Boolean) -> Unit,
    onAlwaysListeningChange: (Boolean) -> Unit,
    onContinuousVoiceChange: (Boolean) -> Unit,
    onWakeWordChange: (String) -> Unit,
    onOnDeviceVoiceChange: (Boolean) -> Unit,
    onSaveAiKey: (String) -> Unit,
    onClearAiKey: () -> Unit,
    onSaveAiModel: (String) -> Unit,
    onTestAi: () -> Unit,
    onSaveCity: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var wordInput by remember(wakeWord) { mutableStateOf(wakeWord) }
    var keyInput by remember { mutableStateOf("") }
    var modelInput by remember(aiModel) { mutableStateOf(aiModel) }
    var cityInput by remember(city) { mutableStateOf(city) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxSize()

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
            title = "Conversación continua",
            subtitle = "Tras responder a una orden hablada vuelve a escuchar para que siga hablando",
            checked = continuousVoice,
            onCheckedChange = onContinuousVoiceChange,
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
            colors = fieldColors(),
        )
        TextButton(onClick = {
            val clean = wordInput.trim()
            if (clean.isNotEmpty()) onWakeWordChange(clean)
        }) {
            Text("Guardar palabra clave", color = JarvisColors.Green)
        }

        HorizontalDivider(color = JarvisColors.Outline)
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "METEOROLOGÍA",
            style = MaterialTheme.typography.labelMedium,
            color = JarvisColors.GreenDim,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Ciudad para el tiempo. Se consulta en Open-Meteo, un servicio gratuito " +
                "que no necesita clave ni permiso de ubicación: solo se envía el nombre de la ciudad.",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisColors.Muted,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = cityInput,
            onValueChange = { cityInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Ciudad") },
            placeholder = { Text("Madrid") },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = JarvisColors.Green),
            colors = fieldColors(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                onSaveCity(cityInput.trim().ifBlank { "Madrid" })
            }) {
                Text("Guardar ciudad", color = JarvisColors.Green)
            }
            TextButton(onClick = { onSaveCity("Madrid") }) {
                Text("Madrid", color = JarvisColors.Muted)
            }
        }

        HorizontalDivider(color = JarvisColors.Outline)
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "INTELIGENCIA ARTIFICIAL",
            style = MaterialTheme.typography.labelMedium,
            color = JarvisColors.GreenDim,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Conecte una clave gratuita de Google Gemini para conversar, explicar temas y " +
                "responder preguntas abiertas. La clave se guarda cifrada en este teléfono y solo " +
                "se envía a Google al hacer una consulta.",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisColors.Muted,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (aiConfigured) "✓ Clave configurada" else "Sin clave configurada",
            style = MaterialTheme.typography.bodySmall,
            color = if (aiConfigured) JarvisColors.Green else JarvisColors.Amber,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Clave de API de Gemini") },
            placeholder = {
                Text(if (aiConfigured) "•••••••• (guardada)" else "AIza…")
            },
            visualTransformation = PasswordVisualTransformation(),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = JarvisColors.Green),
            colors = fieldColors(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = {
                    if (keyInput.isNotBlank()) {
                        onSaveAiKey(keyInput)
                        keyInput = ""
                    }
                },
            ) {
                Text("Guardar clave", color = JarvisColors.Green)
            }
            if (aiConfigured) {
                TextButton(onClick = onClearAiKey) {
                    Text("Borrar", color = JarvisColors.Amber)
                }
            }
        }
        OutlinedTextField(
            value = modelInput,
            onValueChange = { modelInput = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Modelo") },
            placeholder = { Text("gemini-3.5-flash-lite") },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = JarvisColors.Green),
            colors = fieldColors(),
        )
        Text(
            text = "Gratuitos actuales: «gemini-3.5-flash-lite» (más cuota diaria) y " +
                "«gemini-3.8-flash» (mejor calidad).",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisColors.Muted,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onSaveAiModel("gemini-3.5-flash-lite") }) {
                Text("Flash-Lite", color = JarvisColors.Green)
            }
            TextButton(onClick = { onSaveAiModel("gemini-3.8-flash") }) {
                Text("Flash 3.8", color = JarvisColors.Green)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onSaveAiModel(modelInput) }) {
                Text("Guardar modelo", color = JarvisColors.Green)
            }
            TextButton(onClick = onTestAi) {
                Text("Probar conexión", color = JarvisColors.Green)
            }
        }
        aiStatus?.let { status ->
            val isProblem = status.startsWith("Error") || status.startsWith("No ") ||
                status.startsWith("Pegue") || status.startsWith("Configure") ||
                status.startsWith("El ")
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = if (isProblem) JarvisColors.Amber else JarvisColors.Green,
            )
        }
        TextButton(
            onClick = {
                runCatching { uriHandler.openUri("https://aistudio.google.com/apikey") }
            },
        ) {
            Text("Obtener una clave gratuita", color = JarvisColors.Green)
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
                • «explícame la fotosíntesis», «tengo una idea para un proyecto»
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
            text = "JARVIS · v2.0-fase7.1",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisColors.Outline,
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = JarvisColors.Green,
    unfocusedBorderColor = JarvisColors.Outline,
    focusedTextColor = JarvisColors.Green,
    unfocusedTextColor = JarvisColors.Green,
    cursorColor = JarvisColors.Green,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedLabelColor = JarvisColors.Green,
    unfocusedLabelColor = JarvisColors.Muted,
    focusedPlaceholderColor = JarvisColors.Outline,
    unfocusedPlaceholderColor = JarvisColors.Outline,
)

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
