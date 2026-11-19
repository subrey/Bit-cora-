# Fase 11.2 — respuestas de conocimiento

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
