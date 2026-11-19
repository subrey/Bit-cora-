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
