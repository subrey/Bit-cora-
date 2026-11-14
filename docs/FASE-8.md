# Fase 8 · Música inteligente

Objetivo: dejar de pedir «pon música» a ciegas y poder pedir exactamente lo que
suena: un género, un artista, una época o un estado de ánimo.

## Qué cambia para el usuario

- **Por género.** «Pon algo de rock», «música de salsa», «algo de jazz».
- **Por artista o título.** «Pon algo de Coldplay», «la canción Billie Jean»,
  «pon la pista Bohemian Rhapsody» (también entre comillas).
- **Por época.** «Pon algo de los 90» (décadas de 1970 a 2000).
- **Por estado de ánimo.** «Pon música para estudiar», «algo tranquilo»,
  «música para el gimnasio», «algo triste», «para una cena romántica».
- **Si no hay coincidencia, decide la IA.** Con clave de Gemini configurada,
  JARVIS le pasa a Gemini la lista de artistas de la biblioteca y elige el que
  mejor encaja con lo pedido.

Antes de esta fase, «pon música» reproducía una pista cualquiera de la
biblioteca y no había forma de afinar.

## Órdenes entendidas

| Orden | Búsqueda |
| --- | --- |
| «pon algo de rock» | género |
| «pon música para estudiar» | estado de ánimo |
| «pon algo de Coldplay» | artista |
| «pon la canción Billie Jean» | título |
| «pon algo de los 90» | década |
| «pon algo de jazz» | género |

## Cómo está hecho

```
assistant/src/main/java/com/example/jarvis/assistant/
├── MusicRequestParser.kt   interpreta la frase → MusicRequest (puro, testeable)
├── MusicSearch.kt          puntúa las pistas de la biblioteca (puro, testeable)
└── MusicPlayer.kt          añade álbum, género y año a cada pista
```

- **La biblioteca manda.** `MusicPlayer` ahora lee también el álbum, el año y
  el género de cada pista. El género solo existe en MediaStore desde Android 11
  (`Build.VERSION_CODES.R`); en versiones anteriores se deja vacío y la búsqueda
  se apoya en el título, el artista y el álbum.
- **Puntuación.** Cada pista recibe una nota según el tipo de búsqueda:
  el género conocido del sistema pesa más (100) que una coincidencia en el
  título o el álbum (45); el artista y el título coinciden con 90 y 50; la
  década con 100. Un género que no encaja resta puntos, así que no se cuela
  música ajena.
- **Diccionarios.** `MusicRequestParser` trae 29 géneros con sus sinónimos en
  español e inglés (rock, reggaeton, electrónica, Flamenco, cumbia…) y 7 estados
  de ánimo con los géneros que suelen encajar (tranquilo, estudiar, energía,
  alegre, triste, romántico, fiesta).
- **Sin metadatos no se rinde.** Si el sistema no dice el género y no hay
  coincidencia local, la IA elige de la lista de artistas disponibles.
- **Todo en local.** La búsqueda no usa la red: solo la biblioteca del
  teléfono. La IA es el plan B y solo si hay clave configurada.

## Pruebas

- `MusicRequestParserTest` (8 tests) cubre género en español e inglés, estado
  de ánimo, década, artista, artista entre comillas, normalización de acentos
  y los casos sin pistas de búsqueda.
- `MusicSearchTest` (7 tests) cubre la búsqueda por género, artista, título y
  década, el acierto por estado de ánimo, el caso sin coincidencias y la lista
  de artistas para la IA.
- `IntentParserTest` cubre las órdenes de búsqueda musical.

## Nota

La calidad de la búsqueda depende de los metadatos que tenga el teléfono. Con
la biblioteca bien etiquetada funciona de maravilla; sin etiquetas de género,
JARVIS cae en la búsqueda por nombre y, si tiene clave, en la elección por IA.
