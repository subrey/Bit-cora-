# Fase 1 · Proyecto y estructura

Objetivo: dejar una base compilable y verificada sobre la que construir el
resto de JARVIS AI, sin romper la app que ya funcionaba.

## Entregado

- **Identidad**: la app pasa a llamarse **JARVIS AI** (`versionCode 2`,
  `versionName 2.0-fase1`), conservando el paquete `com.example.jarvis` para
  actualizar sobre la instalación existente sin perder datos.
- **Gradle**:
  - plugin de serialización de Kotlin;
  - `navigation-compose` para navegación real con estado por pestaña;
  - `okhttp` y `kotlinx-serialization-json` preparados para la Fase 4;
  - `kotlinx-coroutines-test` para pruebas.
- **Capas** (`docs/ARQUITECTURA.md`):
  - `domain/model/ChatMessage.kt`: mensaje de conversación puro.
  - `domain/model/AssistantState.kt`: estados del núcleo.
  - `domain/model/JarvisPersona.kt`: personalidad y prompt del asistente.
  - `domain/repository/ChatRepository.kt`: contrato del histórico.
  - `data/JarvisRepository.kt`: implementa el contrato y mapea Room → dominio.
- **UI**:
  - navegación por rutas (terminal, música, tareas, ajustes) que conserva el
    estado de cada pestaña;
  - barra de estado que muestra el estado real del núcleo;
  - el ViewModel marca procesando/hablando y vuelve a en espera en `finally`
    (nunca se queda bloqueado si falla una orden).
- **Pruebas**: `JarvisPersonaTest` valida el prompt (fecha en español, límites
  de seguridad y estados ocupados).

## Comprobación

Local (requiere JDK 17 + SDK 35):

```bash
./gradlew :assistant:assembleDebug :assistant:testDebugUnitTest
```

En GitHub Actions, cada push compila los dos módulos, ejecuta los tests y
publica `jarvis-debug-apk`. El resultado de la Fase 1 se comprueba en la
pestaña **Actions → Compilar Android**.
