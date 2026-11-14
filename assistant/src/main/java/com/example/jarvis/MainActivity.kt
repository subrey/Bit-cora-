package com.example.jarvis

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.jarvis.service.AlwaysListeningService
import com.example.jarvis.ui.JarvisApp
import com.example.jarvis.ui.JarvisTheme
import com.example.jarvis.ui.JarvisViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app siempre es oscura: iconos claros en las barras del sistema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            JarvisTheme {
                JarvisApp(viewModel)
            }
        }
        maybeStartListening(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        maybeStartListening(intent)
    }

    /** El botón del widget abre la app con el micrófono ya preparado. */
    private fun maybeStartListening(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_START_LISTENING, false) != true) return
        intent.removeExtra(EXTRA_START_LISTENING)
        viewModel.startVoice(delayMillis = 600)
    }

    override fun onStart() {
        super.onStart()
        // La conversación continua solo encadena turnos con la app a la vista.
        viewModel.setAppVisible(true)
    }

    override fun onStop() {
        viewModel.setAppVisible(false)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Con la app en pantalla el reconocimiento de voz es mucho más fiable,
        // así que se relanza la escucha por si el sistema la había cortado.
        AlwaysListeningService.rearm(this)
    }

    companion object {
        /** El widget de escritorio pide abrir la app con el micrófono listo. */
        const val EXTRA_START_LISTENING = "com.example.jarvis.extra.START_LISTENING"
    }
}
