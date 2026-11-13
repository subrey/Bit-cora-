package com.example.jarvis.ui.navigation

/**
 * Rutas de la aplicación.
 *
 * La navegación real usa Navigation Compose: cada pestaña es una ruta con su
 * propio estado guardado. Añadir una pantalla nueva (memoria, ajustes de IA…)
 * es tan simple como añadir una entrada aquí y un `composable(...)` en la
 * raíz de la interfaz.
 */
enum class JarvisDestination(val route: String, val label: String) {
    HOME("home", "INICIO"),
    TERMINAL("terminal", "HISTORIAL"),
    MUSIC("music", "MÚSICA"),
    TASKS("tasks", "TAREAS"),
    SETTINGS("settings", "AJUSTES"),
    ;

    companion object {
        val start: JarvisDestination = HOME

        fun fromRoute(route: String?): JarvisDestination =
            entries.firstOrNull { it.route == route } ?: start
    }
}
