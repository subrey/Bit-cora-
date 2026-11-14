# Fase 7 · Meteorología real

Objetivo: que JARVIS sepa el tiempo que hace de verdad, sin inventarse nada y
sin pedirle datos ni permisos al usuario.

## Qué cambia para el usuario

- **El tiempo de ahora.** «Qué tiempo hace» responde con la temperatura, la
  sensación térmica, el estado del cielo, la lluvia caída y el viento.
- **La previsión.** «Qué tiempo hace mañana» da el máximo, el mínimo y la
  probabilidad de lluvia. La respuesta de «qué tiempo hace» ya incluye el
  resumen de mañana.
- **Un consejo útil.** «¿Debo llevar paraguas?», «hace frío», «voy a salir»
  devuelven una recomendación corta según el tiempo que hace.
- **Ciudad configurable.** En *Ajustes → Meteorología* se escribe la ciudad.
  No hace falta permiso de ubicación: solo se envía el nombre de la ciudad.

Antes de esta fase, JARVIS respondía que no tenía conexión con los servicios
meteorológicos. Ahora usa **Open-Meteo**, que es gratuito, no necesita clave y
no guarda datos personales.

## Órdenes entendidas

| Orden | Resultado |
| --- | --- |
| «qué tiempo hace» | Estado actual + mañana + consejo |
| «qué tiempo hace mañana» | Previsión de mañana |
| «¿debo llevar paraguas?» | Consejo breve |
| «hace frío», «voy a salir» | Consejo breve |

## Cómo está hecho

```
assistant/src/main/java/com/example/jarvis/weather/
├── WeatherModels.kt    Place, CurrentWeather, DailyWeather, WeatherReport
├── WeatherJson.kt      lectura del JSON de Open-Meteo (puro, testeable)
├── WeatherText.kt      el informe traducido a texto hablable (puro, testeable)
├── WeatherProvider.kt  interfaz y resultado (éxito o fallo)
└── OpenMeteoProvider.kt  cliente OkHttp: ciudad + previsión
```

- **Dos llamadas**: la geocodificación (`geocoding-api.open-meteo.com`) busca
  las coordenadas de la ciudad; la previsión (`api.open-meteo.com`) pide el
  estado actual y 7 días de previsión.
- **Sin clave y sin cuenta**: Open-Meteo es abierto. Lo único que sale del
  teléfono es el nombre de la ciudad.
- **Caché en memoria**: el informe se reutiliza 15 minutos, así que preguntar
  dos veces seguidas no hace dos peticiones.
- **Códigos WMO**: `WeatherText.describe()` traduce los códigos de tiempo de
  Open-Meteo a español: despejado, nublado, con niebla, con lluvia ligera, con
  chubascos, con nieve, con tormenta…
- **Sin permiso de ubicación**: la ciudad se escribe a mano en Ajustes. Si no
  se pone ninguna, se usa Madrid.
- Si el servicio no responde, JARVIS lo avisa con un mensaje claro y sigue
  funcionando con el resto de órdenes.

## Pruebas

- `WeatherTextTest` (9 tests) cubre la traducción de los códigos WMO, la
  descripción del estado actual, la sensación térmica, la previsión de un día y
  los tres consejos (paraguas, abrigo, aviso de lluvia al día siguiente).
- `WeatherJsonTest` (4 tests) cubre la lectura del JSON de ciudad y de previsión,
  incluidos los casos sin resultados o sin estado actual.
- `IntentParserTest` cubre las órdenes de tiempo, de mañana y de consejo.

## Nota

La meteorología es la primera función de JARVIS que usa la red sin clave de
Google. Sigue siendo opcional: si no hay cobertura, JARVIS lo dice y el resto
del asistente funciona igual.
