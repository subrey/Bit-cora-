package com.example.jarvis

import android.graphics.Color
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
}
