package com.example.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.jarvis.service.AlwaysListeningService
import com.example.jarvis.ui.JarvisApp
import com.example.jarvis.ui.JarvisTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JarvisTheme {
                JarvisApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Con la app en pantalla el reconocimiento de voz es mucho más fiable,
        // así que se relanza la escucha por si el sistema la había cortado.
        AlwaysListeningService.rearm(this)
    }
}
