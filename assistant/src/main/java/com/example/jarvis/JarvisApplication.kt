package com.example.jarvis

import android.app.Application
import com.example.jarvis.ai.AiProvider
import com.example.jarvis.ai.GeminiProvider
import com.example.jarvis.assistant.AssistantEngine
import com.example.jarvis.assistant.CrashReporter
import com.example.jarvis.assistant.MusicPlayer
import com.example.jarvis.assistant.ReminderScheduler
import com.example.jarvis.assistant.Speaker
import com.example.jarvis.assistant.TimerScheduler
import com.example.jarvis.data.JarvisDatabase
import com.example.jarvis.data.JarvisRepository
import com.example.jarvis.data.SecureSettings
import com.example.jarvis.data.SettingsRepository
import com.example.jarvis.weather.OpenMeteoProvider
import com.example.jarvis.weather.WeatherProvider
import kotlinx.coroutines.flow.first

class JarvisApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Captura los cierres para poder explicarlos en el chat al reabrir.
        CrashReporter.install(this)
    }

    val database: JarvisDatabase by lazy { JarvisDatabase.get(this) }
    val repository: JarvisRepository by lazy {
        JarvisRepository(
            database.todoDao(),
            database.memoryDao(),
            database.timerDao(),
            database.chatDao(),
            database.reminderDao(),
        )
    }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val secureSettings: SecureSettings by lazy { SecureSettings(this) }
    val aiProvider: AiProvider by lazy {
        GeminiProvider(
            apiKey = { secureSettings.apiKey() },
            model = { secureSettings.model() },
        )
    }
    val musicPlayer: MusicPlayer by lazy { MusicPlayer(this) }
    val speaker: Speaker by lazy { Speaker(this) }
    val scheduler: TimerScheduler by lazy { TimerScheduler(this) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }

    /** Fase 7: meteorología con Open-Meteo (gratis y sin clave). */
    val weatherProvider: WeatherProvider by lazy { OpenMeteoProvider() }

    val engine: AssistantEngine by lazy {
        AssistantEngine(
            context = this,
            repository = repository,
            player = musicPlayer,
            scheduler = scheduler,
            ai = aiProvider,
            reminderScheduler = reminderScheduler,
            weather = weatherProvider,
            cityProvider = { settings.city.first() },
        )
    }
}
