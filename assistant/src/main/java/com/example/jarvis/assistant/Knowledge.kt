package com.example.jarvis.assistant

/**
 * Base de conocimiento local: temas que Jarvis conoce sin conexión.
 * Texto en español, pensado para leerse en voz alta.
 */
object Knowledge {

    data class Entry(
        val title: String,
        val text: String,
        val keywords: List<String>,
    )

    private val entries = listOf(
        Entry(
            title = "Revolución Mexicana",
            text = "La Revolución Mexicana fue un movimiento armado y político que comenzó el 20 de noviembre de 1910 " +
                "contra el gobierno de Porfirio Díaz y terminó oficialmente en 1920. Entre sus causas estuvieron la " +
                "dependencia económica del país hacia el extranjero, las malas condiciones del campo y el " +
                "cuestionamiento del sistema de haciendas. En sus filas estuvieron Francisco «Pancho» Villa, " +
                "«Pancho» Villa, Emiliano Zapata, Venustiano Carranza y Felipe de la Torre. El Plan de San Luis, " +                "firmado por Francisco I. Madero en 1910, dio origen al conflicto, y la Constitución de 1917 " +
                "consolidó muchas de sus demandas: tierra, trabajo y educación.",
            keywords = listOf(
                "revolucion mexicana", "revolucion", "mexicana", "madero", "villa",
                "zapata", "porfirio diaz", "plan de san luis", "constitucion de 1917",
                "guerra civil mexicana",
            ),
        ),
        Entry(
            title = "Miguel Hidalgo y Costilla",
            text = "Miguel Hidalgo y Costilla fue un sacerdote mexicano que en 1810 dio el Grito de Dolores, el " +
                "llamado que inició la Guerra de Independencia de México contra el dominio español. Murió en 1811 y " +
                "es considerado el Padre de la Patria.",
            keywords = listOf("hidalgo", "grito de dolores", "padre de la patria", "independencia de mexico"),
        ),
        Entry(
            title = "Albert Einstein",
            text = "Albert Einstein, que nació en 1879 y murió en 1955, fue un físico alemán que desarrolló la teoría " +
                "de la relatividad y recibió el Nobel de Física en 1921. Su famosa ecuación E = mc2 relaciona la " +
                "masa con la energía.",
            keywords = listOf("einstein", "relatividad", "teoria de la relatividad", "nobel de fisica", "einstein teorie"),
        ),
        Entry(
            title = "Napoleón Bonaparte",
            text = "Napoleón Bonaparte (1769-1821) fue un general y soberano francés. Conquistó buena parte de Europa, " +
                "promulgó el Código napoleónico y fue coronado emperador en 1804. Su derrota en la batalla de Waterloo " +
                "en 1815 puso fin a su imperio.",
            keywords = listOf("napoleon", "bonaparte", "codigo napoleonico", "waterloo", "emperador"),
        ),
        Entry(
            title = "El alunizaje",
            text = "El alunizaje fue la llegada a la Luna de la tripulación del Apolo 11 el 20 de julio de 1969. " +
                "Neil Armstrong y Buzz Aldrin fueron los primeros humanos en caminar por la superficie lunar, " +
                "mientras Michael Collins orbitaba en el módulo de comando.",
            keywords = listOf("alunizaje", "apollo 11", "luna", "armstrong", "aldrin", "collin", "nasa"),
        ),
        Entry(
            title = "Antiguo Egitto",
            text = "El Antiguo Egitto fue una civilización del noreste de África que duró desde unos 3000 a.C. hasta " +
                "el 30 a.C. Sus pirámides de Guiza, la escritura jeroglífica y la momificación son sus rasgos más " +
                "conocidos.",
            keywords = listOf("egipto", "piramides", "faraon", "tutankhamon", "nilo", "jeroglificos"),
        ),
        Entry(
            title = "Guerra Fría",
            text = "La Guerra Fría fue el conflicto ideológico entre Estados Unidos y la Unión Soviética que duró desde " +
                "1945 hasta 1991, sin combate directo. Se caracterizó por la carrera armamentística, la guerra " +
                "espacial y la división de Europa en dos bloques.",
            keywords = listOf("guerra fria", "mauer", "truman", "union sovietica", "cortina de hierro"),
        ),
        Entry(
            title = "Ada Lovelace",
            text = "Ada Lovelace (1815-1852) fue una matemática británica que escribió el primer algoritmo " +
                "destinado a ser ejecutado por una máquina y es considerada la primera programadora de la historia.",
            keywords = listOf("ada lovelace", "lovelace", "primer algoritmo", "programadora"),
        ),
        Entry(
            title = "Charles Babbage",
            text = "Charles Babbage (1791-1871) fue un matemático y mecánico británico que diseñó la máquina analítica, " +
                "la primera máquina de calcular programable. En ella Ada Lovelace escribió el primer algoritmo.",
            keywords = listOf("babbage", "maquina analitica", "diferencias", "maquina de calcular"),
        ),
        Entry(
            title = "El sistema solar",
            text = "El sistema solar está formado por el Sol y los ocho planetas: Mercurio, Venus, Tierra, Marte, " +
                "Júpiter, Saturno, Urano y Neptuno, además de sus satélites y los cuerpos menores como los asteroides " +
                "y los cometas.",
            keywords = listOf("sistema solar", "planetas", "mercurio", "neptuno", "cometa", "jupiter"),
        ),
    )

    /** Busca el tema que mejor encaja con el asunto consultado. */
    fun search(subject: String): Entry? {
        val normalized = IntentParser.normalize(subject)
        if (normalized.isBlank()) return null
        var best: Entry? = null
        var bestScore = 0
        entries.forEach { entry ->
            val score = entry.keywords.count { keyword ->
                keyword.length >= 4 && normalized.contains(keyword)
            }
            if (score > bestScore) {
                bestScore = score
                best = entry
            }
        }
        return best
    }

    fun titles(): List<String> = entries.map { it.title }
}
