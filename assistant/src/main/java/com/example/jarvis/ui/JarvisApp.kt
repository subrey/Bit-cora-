package com.example.jarvis.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jarvis.assistant.JarvisBus
import com.example.jarvis.domain.model.AssistantState
import com.example.jarvis.ui.components.HoloBackground
import com.example.jarvis.ui.navigation.JarvisDestination
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun JarvisApp(viewModel: JarvisViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val timers by viewModel.timers.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.ttsEnabled.collectAsStateWithLifecycle()
    val alwaysListening by viewModel.alwaysListening.collectAsStateWithLifecycle()
    val wakeWord by viewModel.wakeWord.collectAsStateWithLifecycle()
    val capturing by JarvisBus.listening.collectAsStateWithLifecycle()
    val onDeviceVoice by viewModel.onDeviceVoice.collectAsStateWithLifecycle()
    val continuousVoice by viewModel.continuousVoice.collectAsStateWithLifecycle()
    val aiConfigured by viewModel.aiConfigured.collectAsStateWithLifecycle()
    val aiModel by viewModel.aiModel.collectAsStateWithLifecycle()
    val aiStatus by viewModel.aiStatus.collectAsStateWithLifecycle()
    val city by viewModel.city.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()

    // El estado más reciente manda: si JARVIS habla o piensa, se muestra eso
    // aunque el servicio de escucha esté abierto en segundo plano.
    val visualState = when {
        assistantState == AssistantState.THINKING -> AssistantState.THINKING
        assistantState == AssistantState.SPEAKING -> AssistantState.SPEAKING
        assistantState == AssistantState.LISTENING || capturing -> AssistantState.LISTENING
        else -> AssistantState.IDLE
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = JarvisDestination.fromRoute(backStackEntry?.destination?.route)

    fun open(destination: JarvisDestination) {
        if (destination == currentTab) return
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.startVoice()
            } else {
                Toast.makeText(
                    context,
                    "Sin permiso de micrófono Jarvis no puede escucharle",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

    val audioPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.scanMusic()
        }

    val listenPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.setAlwaysListening(true)
            } else {
                Toast.makeText(
                    context,
                    "Sin permiso de micrófono Jarvis no puede escuchar siempre",
                    Toast.LENGTH_LONG,
                ).show()
                viewModel.setAlwaysListening(false)
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            viewModel.setAlwaysListening(true)
        }

    fun enableAlwaysListening() {
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            listenPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val notificationsGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!notificationsGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        viewModel.setAlwaysListening(true)
    }

    fun micClick() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.toggleVoice()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        JarvisBus.commands.collect { command ->
            viewModel.forwardFromBus(command)
        }
    }

    LaunchedEffect(Unit) {
        JarvisBus.notices.collect { notice ->
            viewModel.systemMessage(notice)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisColors.Background),
    ) {
        HoloBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            JarvisTopBar(state = visualState)

            NavHost(
                navController = navController,
                startDestination = JarvisDestination.start.route,
                modifier = Modifier.weight(1f),
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(160)) },
            ) {
                composable(JarvisDestination.HOME.route) {
                    HomeScreen(
                        state = visualState,
                        messages = messages,
                        liveTranscript = liveTranscript,
                        onSend = { viewModel.submit(it) },
                        onMicClick = ::micClick,
                        onOpenHistory = { open(JarvisDestination.TERMINAL) },
                        onOpenSettings = { open(JarvisDestination.SETTINGS) },
                    )
                }

                composable(JarvisDestination.TERMINAL.route) {
                    TerminalScreen(
                        messages = messages,
                        listening = visualState == AssistantState.LISTENING,
                        onSend = { viewModel.submit(it) },
                        onMicClick = ::micClick,
                        onExport = { viewModel.exportHistory() },
                    )
                }

                composable(JarvisDestination.MUSIC.route) {
                    MusicScreen(
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
                }

                composable(JarvisDestination.TASKS.route) {
                    TasksScreen(
                        todos = todos,
                        memories = memories,
                        timers = timers,
                        reminders = reminders,
                        onAddTodo = viewModel::addTodo,
                        onToggleTodo = viewModel::toggleTodo,
                        onDeleteTodo = viewModel::deleteTodo,
                        onAddMemory = viewModel::addMemory,
                        onDeleteMemory = viewModel::deleteMemory,
                        onDeleteTimer = viewModel::deleteTimer,
                        onCompleteReminder = viewModel::completeReminder,
                        onDeleteReminder = viewModel::deleteReminder,
                    )
                }

                composable(JarvisDestination.SETTINGS.route) {
                    SettingsScreen(
                        ttsEnabled = ttsEnabled,
                        alwaysListening = alwaysListening,
                        continuousVoice = continuousVoice,
                        wakeWord = wakeWord,
                        onDeviceVoice = onDeviceVoice,
                        aiConfigured = aiConfigured,
                        aiModel = aiModel,
                        aiStatus = aiStatus,
                        city = city,
                        onTtsChange = viewModel::setTtsEnabled,
                        onAlwaysListeningChange = { enabled ->
                            if (enabled) {
                                enableAlwaysListening()
                            } else {
                                viewModel.setAlwaysListening(false)
                            }
                        },
                        onContinuousVoiceChange = viewModel::setContinuousVoice,
                        onWakeWordChange = viewModel::setWakeWord,
                        onOnDeviceVoiceChange = viewModel::setOnDeviceVoice,
                        onSaveAiKey = viewModel::saveAiKey,
                        onClearAiKey = viewModel::clearAiKey,
                        onSaveAiModel = viewModel::saveAiModel,
                        onTestAi = viewModel::testAi,
                        onSaveCity = viewModel::setCity,
                        onClearChat = viewModel::clearChat,
                    )
                }
            }

            JarvisDock(
                current = currentTab,
                onSelect = ::open,
            )
        }
    }
}

@Composable
private fun JarvisTopBar(state: AssistantState) {
    val context = LocalContext.current
    val clock by produceState(initialValue = "", 0) {
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es"))
        while (true) {
            value = LocalDateTime.now().format(formatter)
            delay(1_000)
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
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "J.A.R.V.I.S",
            style = MaterialTheme.typography.titleSmall,
            color = JarvisColors.Ice,
            fontWeight = FontWeight.Medium,
            letterSpacing = 3.sp,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "AI",
            style = MaterialTheme.typography.labelSmall,
            color = JarvisColors.Primary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "● ${state.label}",
            style = MaterialTheme.typography.labelSmall,
            color = stateColor(state),
        )
        Spacer(modifier = Modifier.width(10.dp))
        if (battery >= 0) {
            Text(
                text = "$battery%",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisColors.Muted,
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = clock,
            style = MaterialTheme.typography.labelSmall,
            color = JarvisColors.Muted,
        )
    }
}

@Composable
private fun JarvisDock(
    current: JarvisDestination,
    onSelect: (JarvisDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisColors.Background.copy(alpha = 0.94f))
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = JarvisColors.Outline.copy(alpha = 0.45f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JarvisDestination.entries.forEach { destination ->
                val selected = destination == current
                Column(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .clickable { onSelect(destination) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(JarvisColors.Cyan.copy(alpha = 0.12f)),
                            )
                        }
                        Icon(
                            imageVector = iconFor(destination),
                            contentDescription = destination.label,
                            tint = if (selected) JarvisColors.Cyan else JarvisColors.Muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) JarvisColors.Ice else JarvisColors.Muted,
                    )
                }
            }
        }
    }
}

private fun iconFor(destination: JarvisDestination): ImageVector = when (destination) {
    JarvisDestination.HOME -> Icons.Rounded.Home
    JarvisDestination.TERMINAL -> Icons.Rounded.History
    JarvisDestination.MUSIC -> Icons.Rounded.MusicNote
    JarvisDestination.TASKS -> Icons.Rounded.TaskAlt
    JarvisDestination.SETTINGS -> Icons.Rounded.Settings
}

private fun stateColor(state: AssistantState): Color = when (state) {
    AssistantState.IDLE -> JarvisColors.PrimaryDim
    AssistantState.LISTENING -> JarvisColors.Cyan
    AssistantState.THINKING -> JarvisColors.Primary
    AssistantState.SPEAKING -> JarvisColors.Ice
    AssistantState.ERROR -> JarvisColors.Red
}
