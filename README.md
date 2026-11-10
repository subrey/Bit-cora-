# Bitácora · blog de notas

Aplicación **nativa de Android** escrita en **Kotlin + Jetpack Compose** para llevar un blog privado de notas. Los datos viven únicamente en el teléfono (Room / SQLite) y el tema se guarda con DataStore.

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

## Estructura

```
app/src/main/java/com/example/bitacora/
├── MainActivity.kt           # también recibe el atajo "nueva nota" del widget
├── NotasApplication.kt
├── data/
│   ├── Note.kt              # entidad Room
│   ├── NoteDao.kt           # consultas
│   ├── AppDatabase.kt       # base de datos
│   ├── NoteRepository.kt    # acceso a datos + notas de ejemplo
│   └── SettingsRepository.kt# tema con DataStore
├── widget/
│   └── BitacoraWidgetProvider.kt
└── ui/
    ├── BitacoraApp.kt       # navegación (lista → nota → editor)
    ├── NotesViewModel.kt
    ├── Format.kt            # fechas, palabras, extractos
    ├── theme/               # colores, tipografía y paletas
    └── screens/
        ├── NoteListScreen.kt
        ├── NoteDetailScreen.kt
        └── NoteEditorScreen.kt
```

Recursos del widget: `res/layout/widget_bitacora.xml`, `res/xml/bitacora_widget_info.xml` y los colores `widget_*` con variante nocturna.

## Cómo compilar en local

Requisitos: **JDK 17** y **Android Studio** (o el SDK de Android con Gradle).

1. Abre la carpeta del proyecto en Android Studio y deja que sincronice Gradle.
2. Conecta un teléfono con depuración USB o crea un emulador.
3. Pulsa **Run ▶**.

Por terminal, con el SDK configurado (`ANDROID_HOME` o `local.properties`):

```bash
./gradlew assembleDebug
# APK en app/build/outputs/apk/debug/app-debug.apk
```

```bash
./gradlew installDebug
```

Para ejecutar los tests unitarios de los formateadores:

```bash
./gradlew test
```

> Nota: este proyecto se escribió en un entorno sin JDK ni Android SDK, por lo que no se ha podido compilar aquí. Las versiones están fijadas en `build.gradle.kts` (AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Room 2.6.1) y el wrapper de Gradle 8.9 viene incluido.

## Compilar en GitHub (GitHub Actions)

El proyecto incluye `.github/workflows/android.yml`. Cada `push`, pull request o ejecución manual:

1. Prepara JDK 17 y el Android SDK 35.
2. Ejecuta `./gradlew assembleDebug testDebugUnitTest`.
3. Publica el APK como artefacto descargable.

Pasos para usarlo:

1. Sube el proyecto a un repositorio de GitHub (`git push`).
2. Entra en **Actions → Compilar Android** y espera a que termine (o lánzalo con **Run workflow**).
3. Abre la ejecución y descarga **bitacora-debug-apk** en la sección *Artifacts*.
4. Instala el APK en el teléfono (permite instalar apps de origen desconocido).

El APK de depuración va firmado con la clave de debug generada en el runner, así que es instalable directamente. El artefacto se conserva 30 días.

## Datos y privacidad

No hay permisos de red ni cuentas: todo se guarda en `bitacora.db` dentro del almacenamiento privado de la app. La copia de seguridad de Android está habilitada (`backup_rules.xml`).
