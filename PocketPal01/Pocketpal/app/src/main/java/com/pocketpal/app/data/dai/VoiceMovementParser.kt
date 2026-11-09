package com.pocketpal.app.data.dai

import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import java.text.Normalizer
import java.time.LocalDate

/**
 * Lo que Dai entendió de una frase dicha por voz, por ejemplo
 * *"Gasté 45 mil pesos en mercado, mercado de la semana"*.
 * Lo que no encontró queda vacío para que el usuario lo complete en "Confirmar".
 */
data class VoiceDraft(
    val kind: MovementKind,
    val amount: Double?,
    val date: LocalDate,
    val categoryId: String?,
    val reason: String,
)

/**
 * Módulo 6 · Flujo 5 · "Registro por voz": convierte el texto que reconoció el micrófono en
 * un movimiento (monto, tipo, fecha, categoría y motivo). Es Kotlin puro, sin Android, así
 * que se prueba con JUnit (ver `VoiceMovementParserTest`).
 *
 * - **Tipo**: ingreso si dice "recibí", "me pagaron", "salario"…; si no, gasto.
 * - **Monto**: números ("45000", "45.000", "45 mil", "1,2 millones") o palabras
 *   ("cuarenta y cinco mil"). Si dice varios, toma el más grande ("2 cafés de 5 mil" → 5.000).
 * - **Fecha**: "hoy" (por defecto), "ayer", "antier" o "hace 3 días".
 * - **Categoría**: el nombre de una categoría del usuario o una pista ("taxi" → Transporte).
 * - **Motivo**: lo que va después de la última coma; si no hay coma, la frase sin el verbo,
 *   el monto ni la fecha ("pagué 20 mil de taxi ayer" → "Taxi").
 */
object VoiceMovementParser {

    private val incomeHints = listOf(
        "recibi", "me pagaron", "me pago", "me consignaron", "me transfirieron", "me llego", "me dieron",
        "gane", "ingreso", "salario", "sueldo", "nomina", "quincena", "vendi",
    )

    private val incomeCategoryHints: List<Pair<List<String>, List<String>>> = listOf(
        listOf("salario", "sueldo", "nomina", "quincena", "me pagaron") to listOf("salario", "sueldo", "nomina", "trabajo"),
        listOf("vendi", "venta") to listOf("ventas", "venta", "negocio"),
        listOf("regalo", "me dieron") to listOf("regalos", "regalo"),
    )

    private val mealHints = listOf("almuerzo", "desayuno", "cena", "onces", "comida", "mecato", "cafe")
    private val mealCategories = listOf("comida", "alimentacion", "alimentos", "mercado", "supermercado")

    /** Palabras que no van en el motivo: verbos de gasto/ingreso, "pesos", fechas… */
    private val dropWords = setOf(
        "gaste", "pague", "compre", "gasto", "recibi", "gane", "vendi", "ingrese", "registra", "registrar", "anota",
        "me", "pagaron", "pago", "consignaron", "transfirieron", "llego", "dieron",
        "pesos", "peso", "lucas", "luca", "palos", "palo", "k", "cop",
        "hoy", "ayer", "antier", "anteayer", "dia",
    )

    private val leadingFillers = setOf("en", "de", "del", "para", "por", "el", "la", "los", "las", "un", "una", "y", "a", "al", "que", "fue", "es")

    private val units = mapOf(
        "cero" to 0, "un" to 1, "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12,
        "trece" to 13, "catorce" to 14, "quince" to 15, "dieciseis" to 16, "diecisiete" to 17, "dieciocho" to 18,
        "diecinueve" to 19, "veinte" to 20, "veintiun" to 21, "veintiuno" to 21, "veintidos" to 22, "veintitres" to 23,
        "veinticuatro" to 24, "veinticinco" to 25, "veintiseis" to 26, "veintisiete" to 27, "veintiocho" to 28,
        "veintinueve" to 29, "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50, "sesenta" to 60,
        "setenta" to 70, "ochenta" to 80, "noventa" to 90, "cien" to 100, "ciento" to 100, "doscientos" to 200,
        "trescientos" to 300, "cuatrocientos" to 400, "quinientos" to 500, "seiscientos" to 600,
        "setecientos" to 700, "ochocientos" to 800, "novecientos" to 900,
    )
    private val thousands = setOf("mil", "lucas", "luca", "k")
    private val millions = setOf("millon", "millones", "palos", "palo")

    fun parse(text: String, today: LocalDate, categories: List<MovementCategory>): VoiceDraft {
        val plain = " ${normalize(text)} "
        val kind = if (incomeHints.any { plain.contains(" $it ") }) MovementKind.Income else MovementKind.Expense
        return VoiceDraft(
            kind = kind,
            amount = findAmount(text),
            date = findDate(plain, today),
            categoryId = suggestCategory(text, kind, categories)?.id,
            reason = findReason(text),
        )
    }

    /** Minúsculas y sin tildes; deja los dígitos con sus puntos y comas ("45.000"). */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace("$", " ")
            .replace(Regex("([0-9])(k|mil|millones|millon|lucas)\\b"), "$1 $2")   // "45mil" → "45 mil"
            .replace(Regex("[^a-z0-9., ]"), " ")
            .replace(Regex("(?<![0-9])[.,]|[.,](?![0-9])"), " ")   // puntos y comas que no están entre dígitos
            .replace(Regex("\\s+"), " ")
            .trim()

    /** El monto más grande que se dijo, o `null` si no hay ninguno. */
    fun findAmount(text: String): Double? {
        val words = normalize(text).split(" ").filter { it.isNotEmpty() }
        val found = mutableListOf<Double>()
        var i = 0
        while (i < words.size) {
            if (!isNumberWord(words[i])) { i++; continue }
            // Lee toda la frase del número: "cuarenta y cinco mil", "2 millones 500 mil".
            var total = 0.0
            var current = 0.0
            var onlyArticle = true
            while (i < words.size) {
                val w = words[i]
                when {
                    w == "y" && current > 0 && i + 1 < words.size && isNumberWord(words[i + 1]) -> Unit
                    w in thousands && (current > 0 || total == 0.0) -> {
                        total += (if (current == 0.0) 1.0 else current) * 1_000
                        current = 0.0
                        onlyArticle = false
                    }
                    w in millions -> {
                        total = (total + (if (current == 0.0) 1.0 else current)) * 1_000_000
                        current = 0.0
                        onlyArticle = false
                    }
                    w in units -> {
                        current += units.getValue(w)
                        if (w !in setOf("un", "una", "uno")) onlyArticle = false
                    }
                    parseNumeric(w) != null -> {
                        current += parseNumeric(w)!!
                        onlyArticle = false
                    }
                    else -> break
                }
                i++
            }
            val value = total + current
            // "un mercado" no es un monto.
            if (value > 0 && !onlyArticle) found.add(value)
        }
        return found.maxOrNull()
    }

    private fun isNumberWord(w: String) = w in units || w in thousands || w in millions || parseNumeric(w) != null

    /**
     * "45000" → 45000 · "45.000" y "45,000" → 45000 (miles) · "1.200.000" → 1200000 ·
     * "1,5" → 1.5 (decimal: no tiene 3 dígitos después del separador).
     */
    fun parseNumeric(word: String): Double? {
        if (!word.matches(Regex("[0-9]+([.,][0-9]+)*"))) return null
        val groups = word.split('.', ',')
        if (groups.size == 1) return word.toDoubleOrNull()
        return if (groups.drop(1).all { it.length == 3 }) {
            groups.joinToString("").toDoubleOrNull()
        } else {
            word.replace(',', '.').let { w ->
                // Solo el último separador es decimal: "1.234,5" → 1234.5
                val last = w.lastIndexOf('.')
                (w.substring(0, last).replace(".", "") + w.substring(last)).toDoubleOrNull()
            }
        }
    }

    /** "ayer", "antier", "hace 3 días"; si no dice nada, hoy. Nunca una fecha futura. */
    fun findDate(plain: String, today: LocalDate): LocalDate {
        Regex(" hace ([0-9]+) dias? ").find(plain)?.let { m ->
            return today.minusDays(m.groupValues[1].toLong().coerceIn(0, 365))
        }
        return when {
            plain.contains(" antier ") || plain.contains(" anteayer ") || plain.contains(" antes de ayer ") -> today.minusDays(2)
            plain.contains(" ayer ") -> today.minusDays(1)
            else -> today
        }
    }

    /** Una categoría del mismo tipo (gasto o ingreso) que se nombre o se insinúe. */
    fun suggestCategory(text: String, kind: MovementKind, categories: List<MovementCategory>): MovementCategory? {
        if (kind == MovementKind.Expense) {
            ReceiptParser.suggestCategory(text, categories)?.let { return it }
            // Palabras que se dicen hablando y casi nunca salen en una factura.
            val spoken = " ${normalize(text)} "
            if (mealHints.any { spoken.contains(" $it") }) {
                return categories.firstOrNull { it.kind == MovementKind.Expense.wire && normalize(it.name) in mealCategories }
            }
            return null
        }
        val plain = " ${normalize(text)} "
        val incomes = categories.filter { it.kind == MovementKind.Income.wire }
        incomes.firstOrNull { plain.contains(" ${normalize(it.name)} ") }?.let { return it }
        for ((hints, names) in incomeCategoryHints) {
            if (hints.any { plain.contains(" $it ") }) {
                incomes.firstOrNull { normalize(it.name) in names }?.let { return it }
            }
        }
        return null
    }

    /**
     * El motivo, con la primera letra en mayúscula ("Mercado de la semana"):
     * 1. si la persona dice "motivo" o "porque", lo que viene después;
     * 2. si no, lo que va después de la última coma;
     * 3. si no, la frase sin el verbo, el monto, la fecha ni "la categoría X".
     */
    fun findReason(text: String): String {
        var part = text
        val said = Regex("""(?i)\b(?:por\s+)?motivo\b[:,]?|\bporque\b|\bpor\s+qu[eé]\b""").findAll(part).lastOrNull()
        part = when {
            said != null && part.substring(said.range.last + 1).isNotBlank() -> part.substring(said.range.last + 1)
            part.contains(',') -> part.substringAfterLast(',')
            else -> part
        }
        part = part
            .replace(Regex("""(?i)\b(?:de|en|a)?\s*(?:la\s+)?categor[ií]a\s+\S+"""), " ")   // "de la categoría gustos"
            .replace(Regex("""(?i)hace\s+\S+\s+d[ií]as?"""), " ")                          // "hace 3 días"
            .replace(Regex("""([0-9])([a-zA-Z])"""), "$1 $2")                                // "45mil" → "45 mil"
        val words = part.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val norm = words.map(::normalize)
        val kept = words.filterIndexed { i, _ ->
            val n = norm[i]
            val next = norm.getOrNull(i + 1)
            when {
                n.isEmpty() || n.matches(Regex("[0-9.,]+")) -> false
                // "un", "una": son número solo si sigue "mil"/"millón" ("un millón"), no en "un gustico".
                n in setOf("un", "una", "uno") -> !(next != null && (next in thousands || next in millions))
                // "me" solo sobra en "me pagaron", "me dieron"…; en "me antojé" se queda.
                n == "me" -> next !in setOf("pagaron", "pago", "consignaron", "transfirieron", "llego", "dieron")
                n in dropWords || isNumberWord(n) -> false
                else -> true
            }
        }.toMutableList()
        // Quita rellenos al principio y al final ("y que", "en", "de").
        while (kept.isNotEmpty() && normalize(kept.first()) in leadingFillers) kept.removeAt(0)
        while (kept.isNotEmpty() && normalize(kept.last()) in leadingFillers) kept.removeAt(kept.lastIndex)
        val reason = kept.joinToString(" ").trim().trimEnd('.', '!', '?').take(REASON_MAX)
        return reason.replaceFirstChar { it.uppercase() }
    }

    const val REASON_MAX = 200
}
