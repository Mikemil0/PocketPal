package com.pocketpal.app.data.dai

import com.pocketpal.app.data.budget.BudgetRules
import com.pocketpal.app.data.movement.MovementCategory
import com.pocketpal.app.data.movement.MovementKind
import java.time.LocalDate

/**
 * Convierte el TEXTO que leyó el OCR (ML Kit) en los datos de un gasto:
 * comercio, monto total, fecha y una categoría sugerida.
 *
 * ```
 * TIENDA EXPRESS            → comercio
 * NIT 123456789-0
 * 12/04/2025 16:24          → fecha
 * Subtotal      $16.000
 * IVA            $1.900
 * Total         $17.900     → monto
 * ```
 * Son reglas puras (sin Android): se prueban con JUnit en ReceiptParserTest.
 * El resultado NUNCA se guarda solo: el usuario lo revisa y lo aprueba primero.
 */
object ReceiptParser {

    const val MERCHANT_MAX = 60

    /** Palabras que NO son el nombre del comercio (encabezados típicos de una factura). */
    private val notMerchant = setOf(
        "nit", "rut", "factura", "fecha", "hora", "tel", "telefono", "direccion", "dir", "caja", "cajero", "regimen",
        "resolucion", "cliente", "cc", "pos", "ticket", "recibo", "documento", "electronica", "venta",
    )

    /** Palabras de la línea del total, de la más confiable a la menos. */
    private val totalWords = listOf("total a pagar", "valor a pagar", "total pagar", "neto a pagar", "total", "valor total", "a pagar")

    /** Palabra clave en la factura → nombre de categoría que se busca en las del usuario. */
    private val categoryHints: List<Pair<List<String>, List<String>>> = listOf(
        listOf("supermercado", "mercado", "tienda", "carulla", "exito", "d1", "ara", "olimpica", "jumbo", "panaderia",
            "restaurante", "cafe", "comida", "pan", "leche", "galletas", "agua", "frutas", "domicilio", "rappi") to
            listOf("comida", "alimentacion", "alimentos", "mercado", "supermercado"),
        listOf("uber", "didi", "taxi", "transmilenio", "gasolina", "terpel", "primax", "peaje", "parqueadero", "bus") to
            listOf("transporte", "movilidad", "gasolina"),
        listOf("cine", "netflix", "spotify", "teatro", "concierto", "juego", "bar") to
            listOf("entretenimiento", "ocio", "diversion"),
        listOf("farmacia", "drogueria", "medic", "clinica", "eps") to
            listOf("salud", "farmacia"),
        listOf("ropa", "zara", "falabella", "tenis", "almacen", "centro comercial") to
            listOf("compras", "ropa"),
        listOf("arriendo", "energia", "acueducto", "gas natural", "internet", "claro", "movistar", "tigo", "etb") to
            listOf("hogar", "servicios", "vivienda"),
    )

    /**
     * Arma el borrador. Lo que no se encuentre queda vacío o con un valor seguro
     * (la fecha de hoy) para que el usuario lo complete en "Confirmar".
     */
    fun parse(rawText: String, today: LocalDate, categories: List<MovementCategory>): ReceiptDraft {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val merchant = findMerchant(lines)
        val category = suggestCategory(rawText, categories)
        return ReceiptDraft(
            merchant = merchant,
            amount = findTotal(lines),
            date = findDate(rawText, today) ?: today,
            categoryId = category?.id,
            reason = if (merchant.isBlank()) "" else "Compra en $merchant",
            rawText = rawText,
        )
    }

    // ---------------- Monto ----------------

    /**
     * El total de la factura:
     * 1. la línea que dice "TOTAL" (pero no "SUBTOTAL"), tomando el último número;
     * 2. si no hay, el número más grande que parezca dinero.
     */
    fun findTotal(lines: List<String>): Double? {
        for (word in totalWords) {
            val line = lines.lastOrNull { line ->
                val l = DaiBrain.normalize(line)
                l.contains(word) && !l.contains("subtotal") && !l.contains("sub total") && amountsIn(line).isNotEmpty()
            }
            if (line != null) return amountsIn(line).last()
        }
        return lines.flatMap { amountsIn(it) }.filter { it >= MIN_AMOUNT }.maxOrNull()
    }

    /** Todos los montos de una línea ("Agua 1.000 $3.000" → [1000, 3000]). */
    fun amountsIn(line: String): List<Double> =
        Regex("""\$?\s?\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\$?\s?\d+(?:[.,]\d{1,2})?""")
            .findAll(line)
            .mapNotNull { parseAmount(it.value) }
            .toList()

    /**
     * Lee un número con el formato colombiano o el gringo:
     * "$17.900" → 17900 · "17,900.50" → 17900.5 · "17.900,50" → 17900.5 · "17900" → 17900.
     */
    fun parseAmount(token: String): Double? {
        val clean = token.replace("$", "").replace(" ", "").trim()
        if (clean.isEmpty() || clean.none(Char::isDigit)) return null
        val lastDot = clean.lastIndexOf('.')
        val lastComma = clean.lastIndexOf(',')
        val normalized = when {
            lastDot >= 0 && lastComma >= 0 -> {
                // El separador que aparece de último es el de los decimales.
                if (lastDot > lastComma) clean.replace(",", "") else clean.replace(".", "").replace(',', '.')
            }
            lastDot >= 0 || lastComma >= 0 -> {
                val sep = if (lastDot >= 0) '.' else ','
                val count = clean.count { it == sep }
                val decimals = clean.length - clean.lastIndexOf(sep) - 1
                // Una sola vez y con 1 o 2 cifras después = decimales; si no, son miles.
                if (count == 1 && decimals in 1..2) clean.replace(sep, '.') else clean.replace(sep.toString(), "")
            }
            else -> clean
        }
        return normalized.toDoubleOrNull()
    }

    // ---------------- Fecha ----------------

    /** Primera fecha válida que no sea futura: 12/04/2025, 12-04-25, 12.04.2025 o 2025-04-12. */
    fun findDate(text: String, today: LocalDate): LocalDate? {
        val candidates = mutableListOf<LocalDate>()
        Regex("""\b(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\b""").findAll(text).forEach { m ->
            safeDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())?.let(candidates::add)
        }
        Regex("""\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})\b""").findAll(text).forEach { m ->
            var year = m.groupValues[3].toInt()
            if (year < 100) year += 2000
            // En Colombia la factura dice día/mes/año.
            safeDate(year, m.groupValues[2].toInt(), m.groupValues[1].toInt())?.let(candidates::add)
        }
        return candidates.firstOrNull { !it.isAfter(today) && it.isAfter(today.minusYears(2)) }
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? = try {
        LocalDate.of(year, month, day)
    } catch (e: Exception) {
        null
    }

    // ---------------- Comercio ----------------

    /** La primera línea de arriba que tenga letras y no sea un encabezado ("NIT…", "FACTURA…"). */
    fun findMerchant(lines: List<String>): String {
        val line = lines.take(6).firstOrNull { line ->
            val words = DaiBrain.normalize(line).split(" ")
            words.sumOf { w -> w.count(Char::isLetter) } >= 3 &&
                words.none { it in notMerchant } &&
                amountsIn(line).none { it >= MIN_AMOUNT }   // "D1" sí puede ser un nombre; "$17.900" no
        } ?: return ""
        return line.lowercase().split(" ").filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar(Char::titlecase) }
            .take(MERCHANT_MAX)
    }

    // ---------------- Categoría ----------------

    /**
     * Categoría de GASTOS del usuario que mejor encaja:
     * 1. si el nombre de una de sus categorías aparece en la factura, esa;
     * 2. si no, por palabras clave ("Carulla" → Comida, "Uber" → Transporte).
     */
    fun suggestCategory(text: String, categories: List<MovementCategory>): MovementCategory? {
        val t = " ${DaiBrain.normalize(text)} "
        val expenses = categories.filter { it.kind == MovementKind.Expense.wire }
        expenses.firstOrNull { t.contains(" ${DaiBrain.normalize(it.name)} ") }?.let { return it }
        for ((hints, names) in categoryHints) {
            if (hints.any { t.contains(" $it") }) {
                expenses.firstOrNull { c -> DaiBrain.normalize(c.name) in names }?.let { return it }
            }
        }
        return null
    }

    // ---------------- Aprobación ----------------

    /** Primer problema del borrador antes de guardar (`null` = se puede guardar). */
    fun validate(draft: ReceiptDraft, today: LocalDate): ReceiptProblem? = when {
        draft.amount == null || draft.amount <= 0.0 || draft.amount > BudgetRules.MAX_AMOUNT -> ReceiptProblem.Amount
        draft.date.isAfter(today) -> ReceiptProblem.FutureDate
        draft.merchant.isBlank() -> ReceiptProblem.Merchant
        else -> null
    }

    /**
     * "¿Dónde se guardará?": la categoría y cómo queda su presupuesto (si tiene) con
     * este gasto. Solo cuenta si la fecha cae en el período actual del presupuesto.
     */
    fun impact(draft: ReceiptDraft, data: FinanceSnapshot, weekStart: Int): ReceiptImpact {
        val category = data.categories.firstOrNull { it.id == draft.categoryId }
        val status = data.budgets.firstOrNull { it.budget.categoryId == draft.categoryId }
        if (status == null) {
            return ReceiptImpact(category?.name, null, null, null, exceedsBudget = false)
        }
        val inPeriod = draft.date in BudgetRules.periodRange(status.budget.budgetPeriod, data.today, weekStart)
        val after = status.spent + if (inPeriod) (draft.amount ?: 0.0) else 0.0
        val afterPercent = BudgetRules.percentUsed(after, status.budget.amountLimit)
        return ReceiptImpact(
            categoryName = category?.name,
            budgetName = status.budget.name,
            budgetPercentBefore = status.percent,
            budgetPercentAfter = afterPercent,
            exceedsBudget = after > status.budget.amountLimit,
        )
    }

    /** Lo que dice Dai en el chat después de guardar la factura aprobada. */
    fun savedReply(draft: ReceiptDraft, impact: ReceiptImpact, currency: String = "COP"): DaiReply {
        val amount = draft.amount ?: 0.0
        val where = impact.categoryName?.let { " en $it" } ?: " sin categoría"
        val budget = when {
            impact.budgetPercentAfter == null -> ""
            impact.exceedsBudget -> " Ojo: con este gasto tu presupuesto de ${impact.budgetName} queda en el ${impact.budgetPercentAfter} %, ya pasaste el límite."
            else -> " Con este gasto tu presupuesto de ${impact.budgetName} queda en el ${impact.budgetPercentAfter} %."
        }
        return DaiReply(
            "¡Listo! Registré ${DaiBrain.money(amount, currency)} de ${draft.merchant}$where.$budget",
            DaiCard.ReceiptSaved(draft.merchant, amount, impact.categoryName, draft.date.toString(), impact.budgetPercentAfter),
            listOf(DaiBrain.CHIP_SUMMARY),
        )
    }

    /** Montos más pequeños que esto no se toman como total (son cantidades o códigos). */
    private const val MIN_AMOUNT = 100.0
}

/** Por qué no se puede guardar todavía la factura. */
enum class ReceiptProblem { Amount, FutureDate, Merchant }
