# Fase 9 · Rutinas y automatizaciones

Objetivo: que JARVIS actúe por su cuenta cuando ocurre algo. Ya no solo responde
a órdenes: programa reglas del tipo «cuando pase esto, haz aquello».

## Qué cambia para el usuario

- **Reglas con disparador y acción.** «Cuando suene el temporizador de los
  huevos, apaga la música». «Cuando salte el recordatorio de la reunión, pon
  música».
- **Rutinas horarias.** «Cada mañana a las 8, recuérdame el café». Se repite
  sola cada día.
- **Cinco acciones.** Quitar la música, poner música, avisar (notificación),
  decirlo (con la voz) o crear un recordatorio.
- **Gestión desde la voz.** «Qué rutinas tengo», «borra la rutina de los
  huevos».
- **Gestión desde la pantalla.** Bloque **RUTINAS** en *Tareas*: cada rutina se
  puede activar, desactivar o borrar.

## Disparadores y acciones

| Disparador | Ejemplo | Qué lo activa |
| --- | --- | --- |
| Temporizador | «cuando suene el temporizador de los huevos, apaga la música» | al terminar un temporizador |
| Recordatorio | «cuando salte el recordatorio de la reunión, pon música» | al saltar un recordatorio |
| Hora | «cada mañana a las 8, recuérdame el café» | una alarma diaria |

| Acción | Qué hace |
| --- | --- |
| Quitar la música | detiene la reproducción y devuelve el volumen |
| Poner música | reproduce una pista de la biblioteca |
| Avisar | muestra una notificación |
| Decirlo | lo cuenta con la voz (el micro se pausa mientras habla) |
| Recordar | crea un recordatorio nuevo |

## Cómo está hecho

```
assistant/src/main/java/com/example/jarvis/
├── assistant/
│   ├── RoutineParser.kt     frase → RoutineSpec (puro, testeable)
│   ├── RoutinePlan.kt       qué rutinas aplican y cómo se describen (puro)
│   └── RoutineScheduler.kt  alarma diaria de las rutinas horarias
├── data/
│   ├── Entities.kt          RoutineItem (tabla «routines»)
│   ├── Daos.kt              RoutineDao
│   ├── JarvisDatabase.kt    versión 4 con la migración 3 → 4
│   └── JarvisRepository.kt  alta, consulta, activación y borrado
└── receiver/
    └── JarvisReceiver.kt    ejecuta las rutinas al pasar el disparador
```

- **Dos piezas puras.** `RoutineParser` entiende la frase y `RoutinePlan`
  decide qué rutinas aplican y las describe: ambas se prueban en tests sin
  Android.
- **La etiqueta importa.** Una rutina sin etiqueta («cuando suene cualquier
  temporizador») se aplica a todos; con etiqueta, solo a los que coincidan
  (el texto de la rutina se compara con el del evento, en ambos sentidos, para
  que «huevos» encuentre «para los huevos»).
- **Se ejecuta donde ocurre.** `JarvisReceiver` ya recibía los temporizadores y
  los recordatorios: ahora, después de avisar, busca las rutinas que
  corresponde y las ejecuta. Las rutinas horarias llegan por su propia alarma
  diaria (`setInexactRepeating`).
- **Silenciosa si algo falla.** Una rutina que da error no rompe el aviso que la
  ha provocado.
- **Todo local.** Las rutinas viven en `jarvis.db` y no usan la red.

## Pruebas

- `RoutineParserTest` (6 tests) cubre la rutina de temporizador, la de
  recordatorio, la horaria con hora y con minutos, el caso sin sentido y la
  detección de frases de rutina.
- `RoutinePlanTest` (7 tests) cubre la coincidencia por etiqueta, las rutinas
  sin etiqueta, las desactivadas, las de otro disparador, la selección de las
  que aplican y la descripción legible.
- `IntentParserTest` cubre las órdenes de crear, listar y borrar rutinas.

## Nota

La alarma diaria es inexacta a propósito (`setInexactRepeating`): el sistema
puede retrasarla unos minutos en modo de ahorro de energía, pero así no se
gasta la batería ni hace falta un permiso especial. Para avisos que deban
saltar a un minuto exacto, los recordatorios (fase 5) son la herramienta
adecuada.
