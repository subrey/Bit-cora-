package com.example.jarvis

import android.app.Application
import com.example.jarvis.assistant.AssistantEngine
import com.example.jarvis.assistant.MusicPlayer
import com.example.jarvis.assistant.Speaker
import com.example.jarvis.assistant.TimerScheduler
import com.example.jarvis.data.JarvisDatabase
import com.example.jarvis.data.JarvisRepository
import com.example.jarvis.data.SettingsRepository

class JarvisApplication : Application() {

    val database: JarvisDatabase by lazy { JarvisDatabase.get(this) }
    val repository: JarvisRepository by lazy {
        JarvisRepository(
            database.todoDao(),
            database.memoryDao(),
            database.timerDao(),
            database.chatDao(),
        )
    }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val musicPlayer: MusicPlayer by lazy { MusicPlayer(this) }
    val speaker: Speaker by lazy { Speaker(this) }
    val scheduler: TimerScheduler by lazy { TimerScheduler(this) }
    val engine: AssistantEngine by lazy {
        AssistantEngine(this, repository, musicPlayer, scheduler)
    }
}
