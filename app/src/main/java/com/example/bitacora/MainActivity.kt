package com.example.bitacora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.bitacora.ui.BitacoraApp

class MainActivity : ComponentActivity() {

    private var newNoteRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            newNoteRequest = intent?.getBooleanExtra(EXTRA_NEW_NOTE, false) == true
        }

        setContent {
            BitacoraApp(
                newNoteRequest = newNoteRequest,
                onNewNoteHandled = { newNoteRequest = false },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_NEW_NOTE, false)) {
            newNoteRequest = true
        }
    }

    companion object {
        const val EXTRA_NEW_NOTE = "com.example.bitacora.extra.NEW_NOTE"
    }
}
