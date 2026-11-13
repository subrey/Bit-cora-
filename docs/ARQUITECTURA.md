# Arquitectura de JARVIS AI

## Capas

```
ui/            Compose + ViewModel (MVVM). No habla con Room ni con HTTP.
├── navigation/  rutas de Navigation Compose
├── *Screen.kt   pantallas sin lógica de negocio
└── *ViewModel   estado de pantalla y llamadas a casos de uso / repositorios

domain/        Modelos y contratos. Kotlin puro, sin Android.
├── model/       ChatMessage, AssistantState, JarvisPersona…
└── repository/  interfaces (ChatRepository…)

data/          Implementaciones: Room, DataStore, MediaStore…
├── Entities/Daos/JarvisDatabase
└── JarvisRepository : ChatRepository

assistant/     Motor de órdenes y utilidades del dispositivo
├── IntentParser, AssistantEngine, MusicPlayer, Speaker…

ai/            (Fase 4) proveedores de modelos de lenguaje
service/       escucha permanente en primer plano
receiver/      alarmas y recordatorios
```

Regla de dependencias: `ui → domain ← data`. El dominio no importa nada de
Android, Room ni OkHttp, así que se puede probar con JUnit normal.

## Estado actual (Fase 1)

- El histórico ya está desacoplado: `JarvisRepository` implementa
  `ChatRepository` y traduce `ChatEntry` (Room) a `ChatMessage` (dominio).
- La UI usa `AssistantState` como única fuente de verdad del núcleo
  (en espera, escuchando, procesando, hablando, error).
- Navegación con Navigation Compose y estado por pestaña.
- `JarvisPersona` centraliza la personalidad que se enviará al modelo en la
  Fase 4.

Las entidades de tareas, memorias y temporizadores siguen en `data/` y se
migrarán al dominio cuando se toquen en las fases 7 y 8. Es una migración
incremental a propósito: cada fase deja la app compilando y funcionando.

## Cómo añadir una pantalla

1. Añade la ruta en `ui/navigation/JarvisDestination.kt`.
2. Crea el `composable` en `ui/JarvisApp.kt` con su pantalla.
3. Si necesita datos, crea el método en el ViewModel y el contrato en
   `domain/repository` cuando implique una fuente nueva.

## Cómo añadir una orden de voz

1. Añade la intención en `assistant/IntentParser.kt` (y su test).
2. Ejecútala en `assistant/AssistantEngine.kt` devolviendo `AssistantResponse`.
3. Si toca el teléfono, comprueba permisos y pide confirmación si es sensible.

## Fases

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Proyecto, capas, navegación, estado del núcleo | Hecha |
| 2 | Interfaz holográfica (núcleo animado, ondas, paneles) | Pendiente |
| 3 | Voz continua e interrumpible | Pendiente |
| 4 | IA conversacional con API gratuita + clave cifrada | Pendiente |
| 5 | Control del teléfono (apps, llamadas, alarmas) | Pendiente |
| 6 | Música integrada | Pendiente |
| 7 | Memoria e historial inteligente | Pendiente |
| 8 | Automatizaciones y funciones avanzadas | Pendiente |
| 9 | Pruebas, rendimiento y seguridad | Pendiente |
| 10 | APK firmado y publicación | Pendiente |
