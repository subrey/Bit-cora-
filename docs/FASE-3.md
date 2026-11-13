# Fase 3 · Voz continua e interrumpible

Objetivo: hablar con JARVIS sin el diálogo del sistema, encadenar turnos y
poder cortar una respuesta para tomar la palabra.

## Qué cambia para el usuario

- **Micrófono dentro de la app.** El botón del micrófono (y el núcleo) abre el
  reconocedor directamente. Ya no aparece el diálogo de Google. Los pitidos de
  inicio dependen del motor de voz: con **Voz en el dispositivo** no suenan.
- **Conversación continua.** Después de responder a una orden hablada, JARVIS
  vuelve a escuchar solo. Puede seguir hablando sin tocar nada.
  Se desactiva en **Ajustes → Conversación continua**.
- **Interrupción.** Mientras JARVIS habla, el botón del micrófono lo calla y
  abre el micrófono en el acto. El botón también sirve para dejar de escuchar.
- **Transcripción en vivo.** En la pantalla de inicio se ve lo que el
  reconocedor va entendiendo antes de ejecutar la orden.
- **Errores comprensibles.** Si falta el micrófono, no hay motor de voz o se
  pierde la red, JARVIS lo explica en la conversación y vuelve a estar listo.

## Cómo se usa

1. Pulse el micrófono o el núcleo y diga, por ejemplo, «pon música».
2. JARVIS responde en voz alta.
3. Al terminar, el micrófono se reabre. Diga «sube el volumen» para continuar.
4. Para cortar una respuesta, pulse el micrófono mientras habla.
5. Para detener la escucha, vuelva a pulsar el micrófono.

La conversación continua solo se activa cuando el turno empezó por voz. Si
usted escribe una orden, JARVIS responde y no abre el micrófono por sorpresa.

## Cómo está hecho

```
assistant/voice/VoiceRecognizer.kt  SpeechRecognizer directo + resultados parciales
assistant/Speaker.kt                TextToSpeech que suspende hasta terminar
assistant/VoicePolicy.kt            reglas puras: ¿toca volver a escuchar?
ui/JarvisViewModel.kt               bucle de voz, interrupciones y coordinación
MainActivity.kt                     avisa al ViewModel si la app está visible
```

- `Speaker.speak()` es `suspend`: la corrutina del turno espera al callback
  `onDone`/`onError`/`onStop` del sintetizador. Así el micrófono se reabre justo
  al acabar la frase, sin pausas estimadas por número de palabras.
- `VoicePolicy.shouldAutoListen()` exige cuatro cosas: conversación continua
  activada, turno iniciado por voz, app en primer plano y que el usuario no
  haya interrumpido.
- `VoiceRecognizer` usa el motor en el dispositivo si está activado en Ajustes
  y cae al motor normal si el modelo local no existe. También se recupera de
  `ERROR_RECOGNIZER_BUSY` con un reintento silencioso.
- **Un solo micrófono.** Android no permite dos reconocedores escuchando a la
  vez. Cuando la app abre su micrófono, marca `JarvisBus.appMic`, el servicio de
  palabra clave se aparta y la app **espera a que el servicio esté realmente
  detenido** antes de abrir el reconocedor; al terminar la conversación se vuelve
  a levantar si la escucha permanente está activada. Antes había una carrera
  entre los dos reconocedores que cerraba la aplicación justo al reabrir el
  micrófono.
- El manifiesto declara `<queries>` para `android.speech.RecognitionService`:
  sin esa visibilidad, Android 11+ puede no ver el motor de voz del teléfono.
- El motor en el dispositivo solo se crea si
  `SpeechRecognizer.isOnDeviceRecognitionAvailable()` lo confirma, para no
  arrancar un motor roto en teléfonos sin el modelo descargado.
- El estado del núcleo refleja el reconocedor real: escuchando, procesando,
  hablando o en espera. La barra superior y el núcleo animado usan ese estado.

## Privacidad

- El audio no se guarda: solo se envía al motor de voz del teléfono para
  transcribirlo, igual que antes.
- La conversación continua está activada por defecto, pero solo reabre el
  micrófono tras una orden hablada y con la app en pantalla. Se puede desactivar
  en cualquier momento.
- Al salir de la app se cierra el micrófono de la app. Si la escucha permanente
  está activada, vuelve el servicio de palabra clave.

## Pruebas

- `VoicePolicyTest` cubre las reglas de continuación y la clasificación de
  errores del reconocedor.
- Los tests existentes del parser, herramientas y personalidad siguen pasando.
- La compilación y los tests se ejecutan en GitHub Actions; el APK resultante es
  `assistant-debug.apk`.

## Límites conocidos

- La interrupción es por toque: el micrófono se mantiene cerrado mientras JARVIS
  habla para que no se escuche a sí mismo.
- La calidad del reconocimiento depende del motor de voz del teléfono. Si no
  hay ninguno instalado, la app lo avisa y se puede seguir usando por texto.
- El servicio de palabra clave se reinicia tras cada conversación para liberar
  el micrófono; es normal ver su notificación aparecer y desaparecer.

## Diagnóstico de cierres

Como aquí no hay acceso a `logcat`, la app guarda su propio rastro y lo muestra
en el chat al volver a abrirla:

- `assistant/CrashReporter.kt` captura las excepciones no controladas en
  `last_crash.txt` y los fallos de voz capturados en `last_error.txt`.
- Si la app murió mientras activaba el micrófono sin dejar excepción de Java
  (cierre nativo o del sistema), lo indica con la marca `mic_open`.
- Los callbacks del reconocedor van envueltos en `runCatching`: un fallo ahí se
  anota en vez de cerrar la aplicación.

Si tras instalar esta versión la app se vuelve a cerrar, al abrirla de nuevo el
chat mostrará el motivo (tipo de excepción y dispositivo), que se puede
reenviar para afinarlo.
