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
