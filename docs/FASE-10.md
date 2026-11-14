# Fase 10 · La agenda del día

Objetivo: que JARVIS sepa qué hay que hacer hoy y lo diga de un vistazo, sin
tener que preguntarle tres veces.

## Qué cambia para el usuario

- **Tareas con fecha.** «Añade tarea llamar al banco mañana a las 10» crea la
  tarea con su día y hora. En la lista, la tarea muestra «Para 15 nov · 10:00».
- **La agenda del día.** «Qué tengo hoy» (o «mi agenda de hoy») responde con
  las tareas de hoy, los recordatorios de hoy, el tiempo y la batería, todo en
  una frase.
- **El widget mira el día.** El recuento ahora dice «2 tareas hoy · 1
  recordatorio(s)» en lugar del total de tareas.
- **El resumen automático.** Una rutina puede pedir el resumen del día:
  «cada mañana a las 8, avísame el resumen del día» muestra en la notificación
  lo que tiene pendiente ese día.

## Órdenes entendidas

| Orden | Resultado |
| --- | --- |
| «añade tarea llamar al banco mañana a las 10» | tarea con fecha |
| «qué tengo hoy» | agenda del día |
| «mi agenda de hoy», «qué hay hoy», «resumen del día» | agenda del día |
| «cada mañana a las 8, avísame el resumen del día» | rutina con el resumen diario |

## Cómo está hecho

```
assistant/src/main/java/com/example/jarvis/
├── assistant/
│   └── Agenda.kt     qué hay hoy y cómo se dice (puro, testeable)
├── data/
│   ├── Entities.kt   TodoItem gana la columna dueAt
│   ├── Daos.kt       consulta de las tareas con fecha
│   ├── JarvisDatabase.kt   versión 5 con la migración 4 → 5
│   └── JarvisRepository.kt tareas que vencen hoy
├── ui/
│   └── TasksScreen.kt    la tarea muestra su fecha
└── widget/
    └── WidgetSummary.kt  el recuento pasa a ser «tareas hoy»
```

- **Una sola columna nueva.** La migración `4 → 5` añade `dueAt` (nullable) a
  la tabla `todos`: las tareas antiguas se quedan sin fecha y siguen siendo
  válidas.
- **El parser se reaprovecha.** Las tareas usan `ReminderParser.parseStrict()`:
  el mismo que entiende «mañana a las 10» en los recordatorios. La diferencia
  es que la versión estricta devuelve null si la frase no trae fecha, así que
  «añade tarea comprar leche» sigue siendo una tarea sin día.
- **`Agenda` es puro.** `todosForToday`, `remindersForToday` y `summary` solo
  necesitan las listas de tareas y avisos y la fecha de hoy: se prueban sin
  Android.
- **El resumen se reutiliza en dos sitios**: en la respuesta de «qué tengo hoy»
  (con el tiempo y la batería) y en la notificación de la rutina horaria (solo
  con las tareas y los avisos, para que el resumen sea rápido y no dependa de
  la red).
- **Prioridad de intenciones.** «mi agenda de hoy» se comprueba antes que
  «añade tarea», porque la palabra «agenda» también servía para las tareas.

## Pruebas

- `AgendaTest` (6 tests) cubre las tareas y avisos de hoy, el resumen con y
  sin pendientes, el añadido del tiempo y la batería, y las etiquetas relativas
  («hoy», «mañana», fecha concreta).
- `IntentParserTest` cubre la agenda del día.
- `WidgetSummaryTest` cubre el nuevo recuento.

## Nota

El resumen de la rutina se genera sin el tiempo ni la batería a propósito: la
notificación se calcula en un receptor del sistema, donde una llamada de red
podría tardar. Para el resumen completo (con el tiempo) está la orden de voz.
