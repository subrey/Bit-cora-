# Bitácora · blog de notas  +  JARVIS · asistente personal

Este repositorio contiene **dos apps nativas de Android**, ambas en **Kotlin + Jetpack Compose**, con datos guardados solo en el teléfono (Room / SQLite, DataStore):

| Módulo | Paquete | Qué es |
|---|---|---|
| `app/` | `com.example.bitacora` | **Bitácora**: un blog privado de notas. |
| `assistant/` | `com.example.jarvis` | **JARVIS AI**: asistente personal con voz, órdenes del teléfono y memoria. |

Se compilan por separado, generan **dos APK** y pueden instalarse los dos en el mismo teléfono.

> **Estado de JARVIS AI**: Fases 1, 2, 3, 4, 5 y 6 completadas (estructura por
> capas, interfaz holográfica, voz continua e interrumpible, IA conversacional
> con clave cifrada, recordatorios con hora exacta e historial exportable, y
> widget de escritorio). Consulta
> [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md),
> [`docs/FASE-1.md`](docs/FASE-1.md), [`docs/FASE-2.md`](docs/FASE-2.md),
> [`docs/FASE-3.md`](docs/FASE-3.md), [`docs/FASE-4.md`](docs/FASE-4.md),
> [`docs/FASE-5.md`](docs/FASE-5.md) y [`docs/FASE-6.md`](docs/FASE-6.md).

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

# 2 · JARVIS AI (asistente personal)

## Interfaz

- Estética **holográfica**: fondo azul casi negro, rejilla tenue, azul
  eléctrico y cian.
- **Núcleo animado** en el centro: late en reposo, emite ondas al escuchar,
  gira como un radar al procesar y muestra un ecualizador circular al hablar.
- Pantalla de inicio con estado del asistente, botón de micrófono, campo de
  texto, resumen de la conversación y accesos a historial y ajustes.
- Barra inferior con cinco secciones: **Inicio**, **Historial**, **Música**,
  **Tareas** y **Ajustes**.
- Barra superior con reloj, batería y estado del núcleo.

## Conversación larga

- Todo el histórico se guarda en `jarvis.db`: al cerrar y abrir la app, la conversación sigue ahí.
- Se guardan hasta **400 mensajes**; los más antiguos se descartan solos para que no crezca sin fin.
- Puedes retroceder en el terminal y preguntarle: «¿qué dijiste antes?».
- Entiende **continuaciones**: tras «pon música», un «y sube el volumen» o «y ahora pausa» se interpreta como seguimiento de la orden anterior.
- *Ajustes → Reiniciar conversación* borra el histórico.

## IA conversacional

- **Conversación libre** con Google Gemini (nivel gratuito): preguntas abiertas,
  explicaciones y charla, además de las órdenes del teléfono.
- **Clave cifrada**: se guarda con el Keystore de Android y se puede borrar desde
  Ajustes. Nunca se escribe en el historial ni en git.
- **Contexto**: la IA recibe la personalidad de JARVIS, los últimos 12 turnos y
  hasta 10 recuerdos guardados con «memoriza…».
- **Prioridad local**: música, tareas, temporizadores, hora, batería, linterna…
  se resuelven sin red; la IA solo entra cuando hace falta.
- **Configuración**: Ajustes → Inteligencia artificial → pegar la clave gratuita
  de `aistudio.google.com/apikey` → Probar conexión.
- **Modelo**: `gemini-3.5-flash-lite` por defecto (más cuota diaria);
  `gemini-3.8-flash` para más calidad. Se cambia en Ajustes con un toque.
- Si no hay clave o se alcanza el límite gratuito, JARVIS lo explica y las
  órdenes locales siguen funcionando.

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
| «recuérdame comprar pan a las 6 de la tarde» | recordatorio con fecha y hora exactas |
| «qué recordatorios tengo» | lista los recordatorios pendientes |
| «cancela el recordatorio de comprar pan» | lo borra y cancela su alarma |
| «exporta el historial» | guarda la conversación en un archivo y abre el menú de compartir |
| «añade tarea comprar leche» | lista de tareas (puedes decir solo «comprar leche») |
| «memoriza el número del hotel» | memorias persistentes |
| «qué hora es», «qué día es» | hora y fecha en español |
| «batería», «almacenamiento» | estado del dispositivo |
| «enciende la linterna» | linterna del teléfono |
| «abre la cámara» | abre una aplicación por su nombre |
| «qué dijiste antes?» | resumen de lo último que habéis hablado |
| «qué sabes hacer» / «ayuda» | lista de órdenes |

## Voz

- **Micrófono** en la app: pulsa y habla. Usa el reconocedor directamente, sin
  el diálogo del sistema, con transcripción en vivo. Con «Voz en el
  dispositivo» tampoco suenan los pitidos del motor.
- **Conversación continua**: tras una orden hablada, JARVIS vuelve a escuchar
  solo cuando termina de responder. Se activa o desactiva en Ajustes.
- **Interrupción**: pulsa el micrófono mientras habla para callarlo y tomar la
  palabra.
- **Escucha permanente**: servicio en primer plano que espera la palabra clave
  (por defecto «Jarvis») y ejecuta la orden. Se aparta mientras la app usa el
  micrófono.
- **Cambio de micrófono fiable**: al abrir el micrófono de la app, el servicio
  suelta su reconocedor y se espera a que el sistema libere el micrófono. Si
  aún está ocupado, se reintenta sin cerrar la escucha.
- **Respuestas habladas** con el sintetizador de voz del sistema (se puede
  desactivar).

## Música y escucha permanente

- La biblioteca se recorre **en segundo plano**: pedir «pon música» ya no
  bloquea la app ni provoca el aviso «JARVIS AI no responde».
- Con música sonando, al detectar «Jarvis» la música **baja al 12 %** para oír
  la orden y vuelve sola a su volumen al terminar. También baja mientras JARVIS
  responde, para que se entienda la respuesta.
- Mientras hay música, el servicio espera más entre sesiones de escucha y no
  llena el chat con avisos de «te he oído» cuando el micrófono capta la letra.

## Recordatorios y widget

- **Recordatorios con hora exacta**: «recuérdame comprar el pan a las 6 de la
  tarde», «en 20 minutos», «mañana a las 9». Se guardan en su propia tabla y
  sobreviven al cierre de la app.
- **Aviso con acciones**: cuando salta, JARVIS lo dice con la voz y muestra una
  notificación con **Hecho** y **Posponer** (10 minutos más).
- **Historial exportable**: «exporta el historial» genera un archivo de texto con
  la conversación completa y abre el menú de compartir (o el botón **Exportar**
  en la pantalla de historial).
- **Widget de escritorio**: estado (en espera / escuchando / hablando), el
  próximo aviso con su hora, el recuento de pendientes y dos botones: hablar
  (abre la app con el micro listo) y música (pone o quita la canción).

## Estructura

```
assistant/src/main/java/com/example/jarvis/
├── MainActivity.kt
├── JarvisApplication.kt
├── ai/                      # Fase 4: IA conversacional
│   ├── AiProvider.kt        # interfaz, petición y resultado
│   ├── AiPrompt.kt          # personalidad + recuerdos + historial
│   ├── GeminiJson.kt        # JSON de Gemini (puro, testeable)
│   └── GeminiProvider.kt    # cliente OkHttp y errores legibles
├── domain/
│   ├── model/               # ChatMessage, AssistantState, JarvisPersona
│   └── repository/          # ChatRepository (contratos)
├── assistant/
│   ├── IntentParser.kt      # entiende frases en español → intención
│   ├── AssistantEngine.kt   # ejecuta la intención y responde
│   ├── Tools.kt             # calculadora, conversor y formatos (puro, testeable)
│   ├── MusicPlayer.kt       # reproductor con MediaStore + MediaPlayer
│   ├── Speaker.kt           # respuestas habladas (TextToSpeech)
│   ├── VoicePolicy.kt       # reglas puras de la conversación continua
│   ├── voice/
│   │   └── VoiceRecognizer.kt  # micrófono de la app (SpeechRecognizer directo)
│   ├── TimerScheduler.kt    # temporizadores con AlarmManager
│   ├── ReminderParser.kt    # frases naturales → recordatorio con fecha (puro)
│   ├── ReminderScheduler.kt # recordatorios con AlarmManager
│   ├── ChatExporter.kt      # historial a texto + compartir
│   ├── Notifications.kt
│   └── JarvisBus.kt         # bus interno servicio ↔ interfaz
├── data/                    # Room, ajustes y SecureSettings (clave cifrada)
├── service/
│   └── AlwaysListeningService.kt   # escucha permanente y palabra clave
├── receiver/
│   └── JarvisReceiver.kt    # alarma de los temporizadores y recordatorios
├── widget/
│   ├── JarvisWidgetProvider.kt  # widget de escritorio
│   └── WidgetSummary.kt        # texto del widget (puro, testeable)
└── ui/
    ├── JarvisApp.kt         # raíz, barra superior, navegación y dock
    ├── HomeScreen.kt        # núcleo, voz, texto y resumen
    ├── components/
    │   ├── JarvisCore.kt    # núcleo animado (Canvas)
    │   ├── HoloBackground.kt
    │   └── HoloPanel.kt
    ├── navigation/          # rutas de Navigation Compose
    ├── TerminalScreen.kt    # historial de conversación
    ├── MusicScreen.kt       # reproductor
    ├── TasksScreen.kt       # tareas, temporizadores y memorias
    ├── SettingsScreen.kt
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

Los APK de depuración van firmados con la clave de debug generada en el runner, así que son instalables directamente. El flujo de trabajo guarda esa clave en la caché de Actions para que todas las compilaciones usen la misma firma y las actualizaciones se instalen encima sin desinstalar. Los artefactos se conservan 30 días.

---

# Datos y privacidad

- **Bitácora**: sin permisos de red; todo se guarda en `bitacora.db` dentro del almacenamiento privado.
- **JARVIS**: tampoco usa la red. Pide permiso de **micrófono** (voz), **notificaciones** (temporizadores) y **lectura de audio** (biblioteca musical) solo cuando usas esa función. Sus datos están en `jarvis.db`.
- La copia de seguridad de Android está habilitada en ambas apps.
