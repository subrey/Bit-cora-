# Fase 6 · Widget de escritorio y acceso directo

Objetivo: tener a JARVIS a mano en la pantalla de inicio, con su estado, su
próximo aviso y dos acciones rápidas sin abrir la app.

## Qué cambia para el usuario

- **Widget de escritorio.** Al mantener pulsado el escritorio se puede añadir el
  widget de JARVIS (3 × 2 celdas, redimensionable).
- **Estado a la vista.** La cabecera muestra si JARVIS está **En espera**,
  **Escuchando** o **Hablando**.
- **Próximo aviso.** Debajo aparece el recordatorio más cercano con su fecha y
  hora («comprar el pan · 14 nov · 18:00») o, si no hay recordatorios, el
  temporizador activo más antiguo. Si no hay nada, dice «Nada programado».
- **Recuento pendiente.** «2 tarea(s) · 1 recordatorio(s)».
- **Botón de micrófono.** Abre la app con la escucha ya preparada: solo hay que
  hablar.
- **Botón de música.** Pone o quita la música directamente desde el escritorio,
  sin abrir la app.

## Cómo está hecho

```
assistant/src/main/java/com/example/jarvis/widget/
├── JarvisWidgetProvider.kt   AppWidgetProvider: estado, próximo aviso y botones
└── WidgetSummary.kt          texto del widget (puro, testeable)

assistant/src/main/res/
├── layout/widget_jarvis.xml  diseño del widget
├── xml/jarvis_widget_info.xml configuración (tamaño, rejilla)
└── drawable/                 fondo, botones e iconos
```

- `WidgetSummary` es una función pura: recibe el estado y los datos y devuelve
  el texto. Se puede probar en tests unitarios sin Android.
- El botón de música envía un broadcast al propio proveedor, que llama a
  `MusicPlayer` desde el proceso de la app: el escaneo de la biblioteca se hace
  en segundo plano (`playRandom`), así que el widget no se congela.
- El botón de micrófono abre `MainActivity` con `EXTRA_START_LISTENING`; la
  actividad llama a `startVoice(delayMillis = 600)` para que el servicio suelte
  el micrófono antes de abrirlo.
- El widget se refresca cuando cambian los datos (tareas, recordatorios,
  temporizadores), cuando JARVIS empieza o deja de hablar y cuando arranca o
  para el servicio de escucha. Además, el sistema lo refresca cada 30 minutos.
- Al saltar un recordatorio, este se marca como completado: deja de contar como
  pendiente. Si el usuario pulsa **Posponer**, vuelve a la lista con la nueva
  hora.

## Pruebas

- `WidgetSummaryTest` cubre el estado según lo que haga el asistente, la
  elección del próximo aviso (recordatorio antes que temporizador), el mensaje
  «Nada programado» y el recuento de pendientes.

## Notas

- El widget no necesita permisos extra: solo lee la base de datos local.
- Si el proceso de la app está muerto (sin servicio de escucha activo), el
  botón de música no tendrá música que poner y no hará nada.
