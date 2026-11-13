# Fase 2 · Interfaz holográfica

Objetivo: sustituir la estética Matrix por una interfaz futurista propia,
manteniendo toda la funcionalidad que ya existía.

## Entregado

### Identidad visual

- Paleta nueva: azul eléctrico (`#3BA7FF`) y cian (`#22E3F5`) sobre azul casi
  negro (`#04070F`), con textos en hielo y grises azulados.
- Tipografía: sans para lectura y monoespaciada para etiquetas y datos.
- Fondo holográfico (`HoloBackground`): degradado profundo, halo cian y
  rejilla tenue. Es estático para no gastar batería.
- Icono de lanzador tipo reactor, tema oscuro azul y splash a juego.

### Núcleo animado (`JarvisCore`)

Dibujado con un único `Canvas` (sin imágenes ni desenfoques):

| Estado | Animación |
|---|---|
| En espera | latido suave y giro lento de los arcos |
| Escuchando | ondas que se expanden desde el núcleo |
| Procesando | barrido de radar y puntos en órbita |
| Hablando | ecualizador circular que late con la voz |
| Error | núcleo rojo |

Los anillos exteriores, las marcas del dial y los arcos giran en sentidos
opuestos; el color del núcleo cambia según el estado.

### Pantalla principal (`HomeScreen`)

- Núcleo central (también se puede tocar para hablar).
- Estado del asistente siempre visible (píldora + barra superior).
- Botón de micrófono grande y campo de texto con envío.
- Resumen de los últimos mensajes.
- Accesos rápidos a historial y ajustes.

### Navegación

- Barra inferior con cinco secciones: **Inicio, Historial, Música, Tareas y
  Ajustes**, con icono y estado seleccionado.
- Transiciones de fundido entre secciones y estado conservado por pestaña.
- El terminal Matrix pasa a ser el **historial** de conversación.

## Comprobación

```bash
./gradlew :assistant:assembleDebug :assistant:testDebugUnitTest
```

GitHub Actions compila y publica `jarvis-debug-apk` en cada push.

> Limitación del entorno de desarrollo: aquí no hay emulador ni pantalla, así
> que la comprobación automática es de compilación y tests. La revisión visual
> final se hace instalando el APK en el teléfono.
