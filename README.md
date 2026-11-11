# Bitácora · blog de notas  +  JARVIS · asistente personal

Este repositorio contiene **dos apps nativas de Android**, ambas en **Kotlin + Jetpack Compose**, con datos guardados solo en el teléfono (Room / SQLite, DataStore):

| Módulo | Paquete | Qué es |
|---|---|---|
| `app/` | `com.example.bitacora` | **Bitácora**: un blog privado de notas. |
| `assistant/` | `com.example.jarvis` | **JARVIS**: asistente personal tipo Alexa con interfaz Matrix. |

Se compilan por separado, generan **dos APK** y pueden instalarse los dos en el mismo teléfono.

---

# 1 · Bitácora (blog de notas)

## Funciones

- Feed tipo blog con las notas ordenadas por fecha y sección de **destacadas**.
- Crear, editar y eliminar notas con confirmación.
- **Buscar** en títulos y contenido.
- **Destacar** notas (se quedan arriba).
- Emoji, color de portada y vista de lectura con tiempo estimado y número de palabras.
- **Compartir** una nota como texto con cualquier app.
- **Tema claro / oscuro** (botón de luna/sol), con Material 3 y edge-to-edge.
- Base de datos local con Room y tres notas de bienvenida en el primer arranque.
- **Widget de pantalla de inicio** con la nota destacada/reciente y botón «+» para escribir una nueva. Se actualiza al guardar, borrar o destacar notas.
- **Adjuntos**: imágenes, vídeos y **escritura a mano** (lienzo con colores, grosores y deshacer).
- **Dictado por voz** para escribir el título y el contenido de la nota.

## Estructura

```
app/src/main/java/com/example/bitacora/
├── MainActivity.kt           # también recibe el atajo "nueva nota" del widget
├── NotasApplication.kt
├── data/
│   ├── Note.kt              # entidad Room
│   ├── Attachment.kt        # entidad de adjuntos (imagen / vídeo / dibujo)
│   ├── NoteDao.kt           # consultas
│   ├── AttachmentDao.kt
│   ├── MediaStore.kt        # copia de ficheros a almacenamiento interno
│   ├── AppDatabase.kt       # base de datos (v2 con migración)
│   ├── NoteRepository.kt    # acceso a datos + notas de ejemplo
│   └── SettingsRepository.kt# tema con DataStore
├── widget/
│   └── BitacoraWidgetProvider.kt
└── ui/
    ├── BitacoraApp.kt       # navegación (lista → nota → editor)
    ├── NotesViewModel.kt
    ├── Format.kt            # fechas, palabras, extractos
    ├── components/
    │   └── HandwritingDialog.kt   # lienzo para escribir a mano
    ├── theme/               # colores, tipografía y paletas
    └── screens/
        ├── NoteListScreen.kt
        ├── NoteDetailScreen.kt
        └── NoteEditorScreen.kt
```

Recursos del widget: `res/layout/widget_bitacora.xml`, `res/xml/bitacora_widget_info.xml` y los colores `widget_*` con variante nocturna.

---

# 2 · JARVIS (asistente personal)

## Interfaz

- Estética **tipo Matrix**: fondo negro, texto verde monoespaciado, **lluvia de caracteres animada** y efecto de escritura letra a letra con cursor parpadeante.
- Barra de estado con reloj, batería y el indicador `● REC` cuando está escuchando.
- Cuatro secciones: **TERMINAL**, **MÚSICA**, **TAREAS** y **AJUSTES**.

## Conversación larga

- Todo el histórico se guarda en `jarvis.db`: al cerrar y abrir la app, la conversación sigue ahí.
- Se guardan hasta **400 mensajes**; los más antiguos se descartan solos para que no crezca sin fin.
- Puedes retroceder en el terminal y preguntarle: «¿qué dijiste antes?».
- Entiende **continuaciones**: tras «pon música», un «y sube el volumen» o «y ahora pausa» se interpreta como seguimiento de la orden anterior.
- *Ajustes → Reiniciar conversación* borra el histórico.

## Tareas del asistente

| Orden | Respuesta |
|---|---|
| «pon música» | reproduce una pista de tu biblioteca |
| «pausa», «siguiente canción», «canción anterior» | control del reproductor |
| «quita la música», «apaga el sonido», «no quiero música» | detiene la reproducción |
| «sube / baja el volumen», «modo aleatorio», «repite» | control de reproducción |
| «calcula 12 * (3 + 4)» | calculadora completa con paréntesis |
| «convierte 10 km a millas» | conversor de unidades (longitud, masa, volumen, tiempo, temperatura) |
| «pon un temporizador de 10 minutos» | temporizador con notificación y aviso hablado |
| «añade tarea comprar leche» | lista de tareas (puedes decir solo «comprar leche») |
| «memoriza el número del hotel» | memorias persistentes |
| «qué hora es», «qué día es» | hora y fecha en español |
| «batería», «almacenamiento» | estado del dispositivo |
| «enciende la linterna» | linterna del teléfono |
| «abre la cámara» | abre una aplicación por su nombre |
| «qué dijiste antes?» | resumen de lo último que habéis hablado |
| «qué sabes hacer» / «ayuda» | lista de órdenes |

## Voz

- **Micrófono** en el terminal: pulsa y habla (reconocimiento de voz del sistema, en español).
- **Escucha permanente**: servicio en primer plano que espera la palabra clave (por defecto «Jarvis») y ejecuta la orden.
- **Respuestas habladas** con el sintetizador de voz del sistema (se puede desactivar).

## Estructura

```
assistant/src/main/java/com/example/jarvis/
├── MainActivity.kt
├── JarvisApplication.kt
├── assistant/
│   ├── IntentParser.kt      # entiende frases en español → intención
│   ├── AssistantEngine.kt   # ejecuta la intención y responde
│   ├── Tools.kt             # calculadora, conversor y formatos (puro, testeable)
│   ├── MusicPlayer.kt       # reproductor con MediaStore + MediaPlayer
│   ├── Speaker.kt           # respuestas habladas (TextToSpeech)
│   ├── TimerScheduler.kt    # temporizadores con AlarmManager
│   ├── Notifications.kt
│   └── JarvisBus.kt         # bus interno servicio ↔ interfaz
├── data/                    # Room: tareas, memorias, temporizadores + ajustes
├── service/
│   └── AlwaysListeningService.kt   # escucha permanente y palabra clave
├── receiver/
│   └── JarvisReceiver.kt    # alarma de los temporizadores
└── ui/
    ├── JarvisApp.kt         # raíz, pestañas y barra de estado
    ├── TerminalScreen.kt    # terminal con efecto de escritura
    ├── MusicScreen.kt       # reproductor
    ├── TasksScreen.kt       # tareas, temporizadores y memorias
    ├── SettingsScreen.kt
    ├── MatrixRain.kt        # lluvia de caracteres estilo Matrix
    └── JarvisTheme.kt
```

---

# Compilar

## En local

Requisitos: **JDK 17** y **Android Studio** (o el SDK de Android con Gradle).

```bash
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk          → Bitácora
# assistant/build/outputs/apk/debug/assistant-debug.apk → JARVIS
```

```bash
./gradlew installDebug          # instala ambas en el dispositivo
./gradlew testDebugUnitTest     # tests unitarios (formateadores, parser, calculadora)
```

> Nota: este proyecto se escribió en un entorno sin JDK ni Android SDK, por lo que no se ha podido compilar aquí. Las versiones están fijadas en `build.gradle.kts` (AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Room 2.6.1) y el wrapper de Gradle 8.9 viene incluido.

## En GitHub (GitHub Actions)

`.github/workflows/android.yml` se ejecuta en cada `push`, pull request o a mano:

1. Prepara JDK 17 y el Android SDK 35.
2. Ejecuta `./gradlew assembleDebug testDebugUnitTest`.
3. Publica **dos artefactos**: `bitacora-debug-apk` y `jarvis-debug-apk`.

Pasos:

1. Entra en **Actions → Compilar Android**.
2. Abre la ejecución terminada y descarga los artefactos en la sección *Artifacts*.
3. Instala los APK en el teléfono (permite instalar apps de origen desconocido).

Los APK de depuración van firmados con la clave de debug generada en el runner, así que son instalables directamente. Los artefactos se conservan 30 días.

---

# Datos y privacidad

- **Bitácora**: sin permisos de red; todo se guarda en `bitacora.db` dentro del almacenamiento privado.
- **JARVIS**: tampoco usa la red. Pide permiso de **micrófono** (voz), **notificaciones** (temporizadores) y **lectura de audio** (biblioteca musical) solo cuando usas esa función. Sus datos están en `jarvis.db`.
- La copia de seguridad de Android está habilitada en ambas apps.
