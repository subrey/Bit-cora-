# Fase 11 — conocimiento y conversación

## Correcciones

- **Preguntas factuales sin historial.** Cuando la pregunta no está en la base
  local ni en Wikipedia, la IA recibe solo la pregunta actual, sin los turnos
  anteriores, y con temperatura baja. Así no mezcla conceptos parecidos: por
  ejemplo, «agujero negro» ya no se responde con la definición de «lente
  gravitatoria».
- **Base local ampliada.** «Agujero negro» y «lente gravitatoria» tienen su
  propia entrada, con palabras clave distintas. La búsqueda local puntúa las
  palabras clave por longitud, de modo que la más específica gana cuando
  aparecen las dos.
- **Extracción de la pregunta más limpia.** Se eliminan arranques como
  «explícame lo que es…», «dime lo que es…» o «lo que es…» antes de consultar
  la base local o Wikipedia.
- **Menos invención.** La temperatura de las respuestas factuales baja a 0,2 y
  el prompt pide responder solo a la pregunta actual y decir «no lo sé» cuando
  no haya seguridad.

## Pruebas

- `KnowledgeTest` comprueba que «agujero negro» y «lente gravitatoria» se
  resuelven con entradas distintas, y que «explícame lo que es un agujero
  negro» se interpreta como conocimiento.
- `AiPromptTest` comprueba el prompt factual.
- `GeminiJsonTest` comprueba que la temperatura de la petición llega al JSON.

## Fase 11.3 — conversación libre

- **Cualquier frase que no sea una orden local** se envía a la IA cuando hay una
  clave configurada. JARVIS ya no responde «No he entendido esa orden» a una
  frase normal: la trata como conversación.
- **Ventana de conversación de 5 minutos.** Las preguntas de seguimiento
  («¿y por qué?», «cuéntame más») usan los últimos turnos y los recuerdos. Las
  preguntas factuales aisladas siguen yendo sin historial y con temperatura
  baja para no mezclar temas.
- **Las órdenes locales tienen prioridad.** Al ejecutar una orden del teléfono,
  el contexto conversacional se cierra para que el siguiente mensaje no
  arrastre la charla anterior.
- **Reiniciar conversación** limpia también el estado interno del motor, no solo
  los mensajes de la pantalla.
- **Prompt de conversación:** la personalidad de JARVIS pide mantener el hilo,
  usar los turnos anteriores y conversar con naturalidad sin convertir cada
  frase en una orden.

## Fase 11.4 — núcleo más vivo al responder

- **Movimiento más rápido.** El núcleo acelera su respiración, el pulso del
  reactor, el barrido holográfico y la órbita de los anillos. En reposo el
  cambio es contenido; al hablar, la rotación se multiplica para que la
  respuesta se note.
- **Respuesta llamativa.** Mientras JARVIS habla aparecen ondas de choque que
  salen del reactor, anillos de voz que laten con el habla y acentos dorados
  en los arcos. El ecualizador orbital se alarga, se ilumina y añade un núcleo
  blanco en cada barra.
- **Sin pérdida de fluidez.** Los efectos son trazos y degradados simples, sin
  desenfoques ni imágenes, y solo se dibujan en el estado `SPEAKING`.

## Fase 11.5 — búsqueda en internet con resumen

- **Nueva orden:** «busca en internet [tema]», «investiga sobre [tema]» o
  «resúmenme lo más importante de [tema]». El motor extrae el tema, consulta
  Google Search desde Gemini y devuelve un titular, de 4 a 6 puntos clave y
  una frase de cierre, todo en español.
- **Fuentes:** cuando el grounding devuelve enlaces, se añaden al final de la
  respuesta con el dominio de cada uno. La primera fuente suele ser la más
  relevante.
- **Sin clave de IA:** se usa Wikipedia como apoyo y se indica cómo configurar
  Gemini para buscar en toda la web.
- **Si el modelo o la cuota no permiten grounding:** se cae a Wikipedia + el
  propio modelo y se avisa de que no ha sido una consulta en directo.
- **Continuidad:** las preguntas de seguimiento («¿y eso por qué?») siguen la
  conversación como en una respuesta normal; las órdenes locales siguen
  teniendo prioridad y cierran el contexto.

## Fase 11.6 — IA más resistente

- **Reintento con otro modelo.** Si el modelo configurado falla por un problema
  de servicio (no por clave, cuota o bloqueo), se prueba una vez con el otro
  modelo gratuito: `gemini-3.8-flash` o `gemini-3.5-flash-lite`.
- **Errores más claros.** Los mensajes de la API incluyen el código HTTP y el
  detalle devuelto por Google, para saber si es sobrecarga, cuota o permisos.
- **Búsqueda degradada.** Si la búsqueda con grounding falla, se intenta el
  resumen sin búsqueda con ambos modelos; si tampoco, se entrega el extracto
  de Wikipedia con un aviso. Así la orden siempre da una respuesta útil.

## Fase 11.7 — diagnóstico de red

- **Nueva orden «diagnóstico de red».** Comprueba la red activa (Wi-Fi o datos),
  si el sistema la marca con acceso a internet y hace dos pruebas HTTP reales:
  Wikipedia y el endpoint de Gemini.
- **Errores de red con detalle.** Los fallos de conexión muestran la clase y el
  mensaje de la excepción (`UnknownHostException`, `SSLHandshakeException`,
  etc.), para distinguir DNS, certificados o falta de datos.
- Permite saber si el fallo está en el móvil, en la restricción de datos de la
  app o en el servicio de Google, sin salir de JARVIS.

## Fase 11.8 — permiso de estado de red

- Se añade `ACCESS_NETWORK_STATE` al manifiesto: sin él, el diagnóstico no
  podía consultar si había Wi-Fi o datos activos y fallaba con un
  `SecurityException`. Con este permiso el diagnóstico muestra la red real y
  las dos pruebas HTTP.

## Fase 11.9 — diagnóstico en segundo plano y doble búsqueda

- El diagnóstico hace las dos peticiones HTTP con `Dispatchers.IO`; antes
  corrían en el hilo principal y siempre fallaban con
  `NetworkOnMainThreadException`.
- Si la búsqueda con Google falla en el modelo configurado, se prueba una vez
  con el otro modelo antes de caer al resumen sin búsqueda. Así se aprovecha
  la búsqueda en directo siempre que algún modelo la admita.

## Fase 11.10 — motivo de la búsqueda fallida

- El aviso del modo de respaldo incluye ahora el error real de la búsqueda con
  grounding (código y detalle de Google), para poder distinguir cuota,
  permisos, tiempo de espera o sobrecarga.
- El diagnóstico solo muestra el consejo de Ahorro de datos si Wikipedia
  falla de verdad, no cuando devuelve HTTP 200.

## Fase 11.11 — ahorrar cuota de búsqueda

- El motivo confirmado del fallo era un **429: límite gratuito agotado** por
  las pruebas seguidas. Cada búsqueda llegó a lanzar hasta cuatro peticiones
  a Gemini.
- Cuando la búsqueda devuelve 429, ya **no se repite con el otro modelo**
  (comparten cuota) y se pasa directamente a un único resumen sin búsqueda.
- El mensaje de límite distingue ahora entre «límite de búsqueda en internet»
  y «límite de IA», para saber qué cuota se ha agotado.
- Con el uso normal (una búsqueda cada cierto tiempo) la búsqueda en directo
  vuelve a funcionar al restablecerse la cuota por minuto.

## Fase 11.12 — dos claves con conmutación automática

- **Ajustes → Inteligencia artificial** permite guardar una **clave de
  respaldo** además de la principal, cifrada con el mismo Keystore.
- Si la clave en uso devuelve 429 (cuota), 403 (permisos), error de servicio o
  timeout, `GeminiProvider` **prueba la otra clave en la misma petición** y
  recuerda cuál funcionó para usarla primero la próxima vez.
- Esto no elimina el límite gratuito: para que dos claves tengan cuotas
  independientes deben pertenecer a **proyectos distintos** de Google AI
  Studio. Dos claves del mismo proyecto suelen compartir la cuota.
- El límite que aparece es específicamente de **Google Search grounding**, no
  de las respuestas normales de la IA. Por eso el resumen de respaldo (Wikipedia
  + modelo) sigue funcionando aunque la búsqueda en directo esté agotada.
- Fase 11.13: Ajustes avisa explícitamente que la segunda clave debe ser de
  otro proyecto.
- Fase 11.14: el aviso de búsqueda muestra el error de **cada clave**
  (`[principal]` y `[respaldo]`) y el detalle que devuelve Google en el 429,
  para identificar qué cuota concreta se está agotando.
- Fase 11.15: «diagnóstico de red» muestra también si la clave principal y la
  de respaldo están guardadas, para comprobar que la conmutación tiene dos
  claves reales disponibles.

## Fase 11.16 — proveedor Groq de respaldo

- **Nueva clave opcional de Groq** en Ajustes. Se guarda cifrada igual que las
  de Gemini y se consigue gratis en `console.groq.com/keys`.
- `FallbackAiProvider` prueba Gemini (con sus dos claves) y, si agota la cuota
  o falla, pasa automáticamente a Groq en la misma petición. El aviso indica
  `[Gemini]` o `[Groq]` según quién fallara.
- Para las búsquedas, Groq usa el modelo `openai/gpt-oss-20b` con la
  herramienta integrada **browser_search** (búsqueda real en la web, con
  cuota propia); para conversar usa `llama-3.1-8b-instant`, muy rápido.
- El diagnóstico muestra el estado de Gemini y de Groq.

## Fase 11.17 — escucha permanente de una sola orden

- Al encender el switch de **escucha permanente**, JARVIS abre el micrófono,
  oye **una sola orden** y, en cuanto la recibe, apaga el micrófono, detiene el
  servicio y deja el switch en OFF. No vuelve a escuchar hasta que se encienda
  de nuevo (o se pulse el micrófono de la pantalla).
- Así se evita que el micro siga abierto y se encadenen sesiones después de la
  primera orden.

## Fase 11.18 — conversación continua desde el servicio

- Se revierte el disparo único: con **Escucha permanente** encendida el micro
  sigue abierto y vuelve a escuchar tras cada orden.
- Con **Conversación continua** activa, el servicio abre una ventana de 90
  segundos tras la primera orden: acepta las frases siguientes **sin repetir
  «Jarvis»**. Cada nueva orden renueva la ventana; al expirar vuelve a exigir
  la palabra clave.
- Si la app está en pantalla, la conversación la lleva el micrófono de la app;
  si está en segundo plano, la lleva el servicio de escucha permanente. Así no
  se queda «apagado» después de la primera orden.

## Fase 11.19 — voz masculina

- **Ajustes → Voz de Jarvis** permite elegir **Masculina**, **Femenina** o
  **Predeterminada**. La preferencia se guarda y se aplica al arrancar.
- `Speaker` elige la mejor voz española disponible (prefiriendo `es-ES`) y
  ajusta el tono: más grave para masculina, más agudo para femenina. Android
  no expone el género de las voces TTS, así que el tono es el mecanismo fiable
  en todos los teléfonos.
- Por defecto se usa la voz **masculina**.

## Fase 11.20 — WhatsApp por voz

- **Nueva orden:** «manda un WhatsApp a Juan diciendo llego tarde». Acepta nombres
  (buscando en contactos) o un número directo.
- **Confirmación obligatoria:** antes de abrir nada, JARVIS repite destinatario
  y mensaje y espera un «sí». Con «no» cancela.
- **No envía automáticamente:** abre WhatsApp con el chat y el texto ya
  escritos; el usuario pulsa «enviar». Así se evita enviar por error.
- Si falta el texto, pregunta «¿Qué mensaje le envío a …?». Si no hay permiso
  de contactos, lo pide y reintenta la orden al concederlo.

## Fase 11.21 — WhatsApp fiable

- Se declaran las consultas de **visibilidad de paquetes** para WhatsApp
  (`com.whatsapp` y `com.whatsapp.w4b`) y para los enlaces `https`/`whatsapp`.
  Sin ellas, Android 11+ oculta la app y JARVIS no podía abrirla.
- Se prueban varios métodos en orden: esquema nativo `whatsapp://send`, enlace
  `wa.me` y enlace `api.whatsapp.com`, con y sin paquete fijado.
- El texto se codifica con `Uri.encode` (espacios como `%20`, no `+`).
- Los números nacionales de 9 dígitos reciben el prefijo **34**; los contactos
  se buscan sin distinguir acentos ni mayúsculas.
- El número se resuelve **antes** de la confirmación y se conserva, de modo que
  responder «sí» no depende de volver a leer los contactos.
- Si ningún método abre el chat, el mensaje se copia al portapapeles y se abre
  WhatsApp para pegarlo a mano.
- La confirmación caduca a los dos minutos para no abrir un mensaje antiguo.

## Fase 11.22 — firma estable de las actualizaciones

- **Causa del error al instalar encima:** el flujo de CI intentaba guardar la
  clave de depuración en una caché, pero esa caché nunca se creaba (el archivo
  no existía al terminar el trabajo). Cada compilación generaba una clave nueva
  y Android rechazaba la actualización por firma distinta, obligando a
  desinstalar.
- **Solución:** la clave de depuración ahora está versionada en
  `assistant/debug.keystore` y Gradle firma siempre con ella. Es una clave de
  depuración, no un secreto de producción.
- El flujo de CI verifica la clave antes de compilar e imprime su huella
  SHA-256, para detectar cualquier cambio accidental.
- **Una última desinstalación:** la versión instalada hasta ahora está firmada
  con una clave aleatoria que ya no se puede recuperar. Tras instalar la 11.22,
  todas las actualizaciones siguientes se instalarán encima sin borrar datos.
