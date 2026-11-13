# Fase 5 · Automatización avanzada y recordatorios inteligentes

Objetivo: dotar a JARVIS de capacidades proactivas, recordatorios basados en voz y notificaciones inteligentes, mejorando la persistencia y la interacción diaria.

## Qué cambia para el usuario

- **Recordatorios por voz.** Posibilidad de pedirle a JARVIS: «Recuérdame comprar leche a las 6 de la tarde» o «Pon un recordatorio para mañana a primera hora».
- **Gestión de tareas y alarmas.** Integración más fluida con el sistema de Android para alarmas y temporizadores avanzados.
- **Memoria persistente mejorada.** Los recuerdos guardados con «memoriza…» se estructuran mejor y se pueden consultar o borrar por categorías.
- **Historial exportable.** Opción para guardar o compartir el historial de chat y notas importantes.

## Cómo estará hecho

```
assistant/src/main/java/com/example/jarvis/
├── data/
│   ├── ReminderEntity.kt     base de datos local Room para recordatorios
│   └── ReminderDao.kt
├── service/
│   └── ReminderReceiver.kt   AlarmManager y BroadcastReceiver para notificaciones
└── domain/
    └── ReminderEngine.kt     procesamiento de intenciones de tiempo y avisos
```

- Uso de `Room` para persistencia robusta de recordatorios y notas.
- `AlarmManager` para programar alertas exactas en el dispositivo.
- Notificaciones enriquecidas con acciones rápidas («Hecho», «Posponer»).

## Pruebas previstas

- `ReminderEngineTest` para verificar la correcta extracción de horas, fechas y descripciones de las órdenes de voz.
- Pruebas de integración de notificaciones y alarmas locales.
