package com.example.jarvis.domain.model

/**
 * Estado visible del núcleo de JARVIS.
 *
 * Es la única fuente de verdad para la interfaz: el núcleo animado, el texto
 * de estado y el botón de micrófono se pintan a partir de este valor.
 */
enum class AssistantState {
    /** En reposo, esperando una orden. */
    IDLE,

    /** El micrófono está capturando voz. */
    LISTENING,

    /** La orden se está interpretando o la IA está respondiendo. */
    THINKING,

    /** JARVIS está hablando (o va a hacerlo en este turno). */
    SPEAKING,

    /** Algo ha fallado y se avisa al usuario. */
    ERROR,
    ;

    val label: String
        get() = when (this) {
            IDLE -> "EN ESPERA"
            LISTENING -> "ESCUCHANDO"
            THINKING -> "PROCESANDO"
            SPEAKING -> "HABLANDO"
            ERROR -> "ERROR"
        }

    /** true mientras el usuario debe esperar una respuesta. */
    val isBusy: Boolean
        get() = this == THINKING || this == SPEAKING
}
