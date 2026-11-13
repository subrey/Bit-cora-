# Fase 4 · IA conversacional con API gratuita y clave cifrada

Objetivo: que JARVIS pueda mantener una conversación libre, explicar temas y
responder preguntas abiertas, usando una API gratuita y guardando la clave de
forma segura en el teléfono.

## Qué cambia para el usuario

- **Conversación libre.** Además de las órdenes del dispositivo, JARVIS puede
  responder preguntas como «explícame la fotosíntesis», «tengo una idea para un
  proyecto» o «¿cómo estás?».
- **Prioridad local.** Las órdenes de música, tareas, temporizadores, hora,
  batería, linterna, etc. siguen resolviéndose en el teléfono, sin red y sin
  gastar la cuota de la IA.
- **Contexto.** La IA recibe la personalidad de JARVIS, los últimos turnos de la
  conversación y los recuerdos guardados con «memoriza…».
- **Clave cifrada.** La clave de API se guarda con el Keystore de Android
  (`EncryptedSharedPreferences`), no en texto plano. Se puede borrar en
  cualquier momento desde Ajustes.
- **Proveedor gratuito.** Se usa **Google Gemini** por su nivel gratuito. El
  modelo se puede cambiar en Ajustes (`gemini-3.5-flash-lite` por defecto, con más
  cuota diaria; `gemini-3.8-flash` para más calidad).

## Cómo activarlo

1. Abra **Ajustes → Inteligencia artificial**.
2. Pulse **Obtener una clave gratuita** (abre `aistudio.google.com/apikey`).
3. Cree una clave de API y péguela en el campo **Clave de API de Gemini**.
4. Pulse **Guardar clave** y luego **Probar conexión**.
5. Vuelva al inicio y pregunte algo abierto, por ejemplo:
   «explícame la fotosíntesis en dos frases».

Sin clave, la app sigue funcionando con todas las órdenes locales y, si se le
pregunta algo abierto, explica cómo configurarla.

## Cómo está hecho

```
ai/
├── AiProvider.kt      interfaz + AiRequest / AiTurn / AiResult
├── AiPrompt.kt        personalidad + recuerdos + recorte del historial
├── GeminiJson.kt      petición y respuesta JSON de Gemini (puro, testeable)
└── GeminiProvider.kt  cliente OkHttp, errores legibles y cancelación
data/SecureSettings.kt clave y modelo cifrados con EncryptedSharedPreferences
```

- `AssistantEngine` resuelve primero la intención local. Solo cuando la orden es
  desconocida, o cuando una pregunta no está en la base local ni en Wikipedia,
  llama a `AiProvider`.
- `AiPrompt` limita el contexto a los **12 turnos** más recientes y añade hasta
  **10 recuerdos** al prompt. Así la petición no crece sin control.
- `GeminiProvider` envía la clave en la cabecera `x-goog-api-key`, nunca en la
  URL ni en los registros. La llamada se ejecuta fuera del hilo principal y se
  cancela si se cierra la pantalla.
- Los errores se traducen a mensajes en español: clave inválida, límite gratuito
  alcanzado, modelo inexistente, falta de red o servicio caído.

## Privacidad y seguridad

- La clave se guarda cifrada con una clave maestra del Keystore de Android. Si
  el Keystore no está disponible, la app no guarda nada y lo avisa.
- La clave solo se envía a Google en la cabecera de la petición. No se escribe
  en el historial, en los registros ni en git.
- El historial y los recuerdos se guardan solo en el teléfono. Al preguntar, se
  envían a Gemini los últimos turnos y los recuerdos; el resto no.
- Se puede borrar la clave desde Ajustes sin borrar la conversación.

## Pruebas

- `AiPromptTest` cubre el recorte del historial, la deduplicación del mensaje
  actual y la inclusión de recuerdos.
- `GeminiJsonTest` comprueba la forma de la petición (personalidad y roles) y la
  lectura de respuestas, errores y bloqueos de seguridad.
- `IntentParserTest` añade la intención de conversación libre.
- La compilación y los tests se ejecutan en GitHub Actions.

## Límites conocidos

- El nivel gratuito de Gemini tiene límites de peticiones por minuto y por día.
  Si se alcanzan, JARVIS lo explica y las órdenes locales siguen funcionando.
- La IA no tiene acceso en tiempo real a noticias, clima ni precios: si no lo
  sabe, debe decirlo. Para el clima se mantiene el aviso honesto actual.
- La calidad y el idioma dependen del modelo elegido. El prompt pide español de
  España, pero el modelo puede colarse en otro registro.
- Las acciones sensibles del teléfono (llamadas, mensajes, compras) no se
  ejecutan desde la IA en esta fase: solo se conversa y se responde.
