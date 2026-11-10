package com.example.bitacora

import android.app.Application
import com.example.bitacora.data.AppDatabase
import com.example.bitacora.data.NoteRepository
import com.example.bitacora.data.SettingsRepository
import com.example.bitacora.widget.BitacoraWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotasApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: NoteRepository by lazy { NoteRepository(database.noteDao()) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.seedIfEmpty()
            BitacoraWidgetProvider.updateAll(this@NotasApplication)
        }
    }
}
