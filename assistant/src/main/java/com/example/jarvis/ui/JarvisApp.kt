package com.example.jarvis.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jarvis.assistant.JarvisBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val tabs = listOf("TERMINAL", "MÚSICA", "TAREAS", "AJUSTES")

@Composable
fun JarvisApp(viewModel: JarvisViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val timers by viewModel.timers.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.ttsEnabled.collectAsStateWithLifecycle()
    val alwaysListening by viewModel.alwaysListening.collectAsStateWithLifecycle()
    val wakeWord by viewModel.wakeWord.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(0) }
    val context = LocalContext.current

    val speechLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.submit(spoken)
            }
        }

    fun startSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga su orden")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { speechLauncher.launch(intent) }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startSpeech()
        }

    val audioPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.scanMusic()
        }

    LaunchedEffect(Unit) {
        JarvisBus.commands.collect { command ->
            viewModel.forwardFromBus(command)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        MatrixRain(modifier = Modifier.fillMaxSize(), brightness = 0.45f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 26.dp),
        ) {
            StatusBar(alwaysListening = alwaysListening)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                tabs.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (index == tab) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == tab) JarvisColors.Green else JarvisColors.Outline,
                        modifier = Modifier
                            .clickable { tab = index }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    0 -> TerminalScreen(
                        messages = messages,
                        listening = alwaysListening,
                        onSend = viewModel::submit,
                        onMicClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                startSpeech()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    )

                    1 -> MusicScreen(
                        state = playerState,
                        onToggle = { viewModel.togglePlayback() },
                        onNext = { viewModel.submit("siguiente canción") },
                        onPrevious = { viewModel.submit("canción anterior") },
                        onVolume = { viewModel.setVolume(it) },
                        onShuffle = { viewModel.setShuffle(it) },
                        onRepeat = { viewModel.setRepeat(it) },
                        onDemoTone = { viewModel.playDemoTone() },
                        onSelect = { viewModel.playTrack(it) },
                        onRequestPermission = {
                            val permission = if (android.os.Build.VERSION.SDK_INT >= 33) {
                                Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            audioPermissionLauncher.launch(permission)
                        },
                    )

                    2 -> TasksScreen(
                        todos = todos,
                        memories = memories,
                        timers = timers,
                        onAddTodo = viewModel::addTodo,
                        onToggleTodo = viewModel::toggleTodo,
                        onDeleteTodo = viewModel::deleteTodo,
                        onAddMemory = viewModel::addMemory,
                        onDeleteMemory = viewModel::deleteMemory,
                        onDeleteTimer = viewModel::deleteTimer,
                    )

                    else -> SettingsScreen(
                        ttsEnabled = ttsEnabled,
                        alwaysListening = alwaysListening,
                        wakeWord = wakeWord,
                        onTtsChange = viewModel::setTtsEnabled,
                        onAlwaysListeningChange = viewModel::setAlwaysListening,
                        onWakeWordChange = viewModel::setWakeWord,
                        onClearChat = viewModel::clearChat,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBar(alwaysListening: Boolean) {
    val context = LocalContext.current
    val clock by produceState(initialValue = "", 0) {
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es"))
        while (true) {
            value = LocalDateTime.now().format(formatter)
            delay(1000)
        }
    }
    val battery by produceState(initialValue = -1, 0) {
        while (true) {
            val manager = context.getSystemService(android.content.Context.BATTERY_SERVICE) as BatteryManager
            value = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            delay(30_000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "JARVIS",
            style = MaterialTheme.typography.titleMedium,
            color = JarvisColors.Green,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "SISTEMA ONLINE",
            style = MaterialTheme.typography.labelSmall,
            color = JarvisColors.GreenDim,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (alwaysListening) {
            Text(
                text = "● REC",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisColors.Amber,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (battery >= 0) {
            Text(
                text = "BAT $battery%",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisColors.Muted,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = clock,
            style = MaterialTheme.typography.labelSmall,
            color = JarvisColors.Muted,
        )
    }
}
