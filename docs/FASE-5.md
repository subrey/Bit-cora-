# Fase 5 · Recordatorios inteligentes y automatización

Objetivo: que JARVIS sea capaz de recordar cosas más adelante, avisar en el
momento exacto y gestionar esos avisos desde la voz o desde la pantalla.

## Qué cambia para el usuario

- **Recordatorios por voz.** «Recuérdame comprar el pan a las 6 de la tarde»,
  «recuérdame llamar al médico mañana a las 10» o «ponme un recordatorio en
  20 minutos».
- **Listar y cancelar.** «Qué recordatorios tengo» y «cancela el recordatorio de
  comprar pan».
- **Aviso a la hora exacta.** Cuando salta, JARVIS lo dice con la voz y además
  muestra una notificación con dos acciones rápidas: **Hecho** y **Posponer**.
- **Pantalla de recordatorios.** En la pestaña **Tareas** hay un bloque
  **RECORDATORIOS** con la fecha y la hora de cada aviso pendiente. Se puede
  marcar como hecho o borrar a mano.
- **Historial exportable.** «Exporta el historial» genera un archivo de texto con
  toda la conversación y abre el menú de compartir. También hay un botón
  **Exportar** en la pantalla de historial.

## Órdenes entendidas

| Orden | Resultado |
| --- | --- |
| «recuérdame comprar pan a las 5 de la tarde» | Recordatorio a las 17:00 de hoy |
| «recuérdame sacar la basura en 30 minutos» | Recordatorio dentro de 30 minutos |
| «llamar al doctor a las 8» | Si las 8 ya han pasado, mañana a las 8:00 |
| «reunión mañana a las 9» | Mañana a las 09:00 |
| «qué recordatorios tengo» | Lista los pendientes con su fecha |
| «cancela el recordatorio de comprar pan» | Lo borra y cancela su alarma |
| «exporta el historial» | Guarda la conversación y abre el menú de compartir |

## Cómo está hecho

```
assistant/src/main/java/com/example/jarvis/
├── assistant/
│   ├── ReminderParser.kt      frases naturales → (mensaje, fecha y hora exactas)
│   ├── ReminderScheduler.kt   AlarmManager con RTC_WAKEUP y hora absoluta
│   └── ChatExporter.kt        historial a texto + menú de compartir
├── data/
│   ├── Entities.kt            ReminderItem (tabla «reminders»)
│   ├── Daos.kt                ReminderDao
│   ├── JarvisDatabase.kt      versión 3 con la migración 2 → 3
│   └── JarvisRepository.kt    alta, consulta, marcado y borrado
├── receiver/
│   └── JarvisReceiver.kt      ACTION_REMINDER, …_DONE y …_SNOOZE
└── ui/
    ├── JarvisViewModel.kt     flujo de recordatorios y acciones
    ├── TasksScreen.kt         bloque RECORDATORIOS
    └── TerminalScreen.kt      botón Exportar
```

- `ReminderParser` es una función pura (sin Android) para poder probarla en
  tests unitarios. Interpreta horas relativas («en 30 minutos»), horas fijas
  («a las 6 de la tarde»), días relativos («mañana», «pasado mañana», «hoy») y
  devuelve un `triggerAt` en milisegundos desde epoch.
- `ReminderScheduler` usa `setExactAndAllowWhileIdle` cuando el sistema permite
  alarmas exactas, y `setAndAllowWhileIdle` si no. El recordatorio sobrevive a
  que el teléfono esté en reposo.
- La base de datos pasa a la **versión 3** con la migración `2 → 3`, que crea
  la tabla `reminders`. Las instalaciones anteriores se actualizan sin perder
  tareas, memorias ni conversaciones.
- Al saltar, `JarvisReceiver` muestra la notificación y habla el aviso. Las
  acciones **Hecho** y **Posponer** vuelven al mismo receptor: «Hecho» marca el
  recordatorio como completado y «Posponer» lo retrasa 10 minutos.
- Los recordatorios se guardan en su propia tabla, no como temporizadores: así
  tienen una fecha absoluta y se pueden listar y cancelar por separado.
- El historial exportado se escribe en la caché de la app y se comparte con un
  `FileProvider` (`res/xml/file_paths.xml`), sin permisos ni archivos públicos.

## Pruebas

- `ReminderParserTest` cubre las horas relativas, las horas fijas con «de la
  tarde», el día siguiente, el salto al día siguiente cuando la hora ya pasó y
  la limpieza del mensaje.
- `IntentParserTest` cubre las órdenes «recuérdame…», «qué recordatorios
  tengo», «cancela el recordatorio…» y «exporta el historial».
- `ChatExporterTest` comprueba que el archivo de texto lleva la fecha, la hora
  y el autor de cada mensaje.

## Nota

Los recordatorios se programan con `AlarmManager`, así que no necesitan que la
app esté abierta. Lo que sí necesita el teléfono es permiso paraExact alarms
en Ajustes del sistema (Android 12 en adelante) para que el aviso salte a la
hora exacta; sin ese permiso el aviso salta en el momento más cercano que
permita el sistema.
