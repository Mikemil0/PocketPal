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
    const val DEFAULT_TITLE = "Dai"

    /** Palabras que NO son el nombre del comercio (encabezados típicos de una factura). */
    private val notMerchant = setOf(
        "nit", "rut", "factura", "fecha", "hora", "tel", "telefono", "direccion", "dir", "caja", "cajero", "regimen",
        "resolucion", "cliente", "cc", "pos", "ticket", "recibo", "documento", "electronica", "venta",
    )

    /** Palabras de la línea del total, de la más confiable a la menos. */
    private val totalWords = listOf("total a pagar", "valor a pagar", "total pagar", "neto a pagar", "total", "valor total", "a pagar")

    /** Palabra clave en la factura → nombre de categoría que se busca en las del usuario. */
    /**
     * Palabras clave → nombres de categoría del usuario. Se comparan **palabras completas**
     * (antes "medic" coincidía con "medición" y "medidor" de los recibos y los mandaba a Salud).
     * Los recibos de servicios van primero: traen palabras como "agua" o "barrio" que no
     * deben llevarlos a Comida ni a Entretenimiento.
     */
    private val categoryHints: List<Pair<List<String>, List<String>>> by lazy { listOf(
        serviceWords to listOf("servicios", "servicios publicos", "hogar", "vivienda", "casa"),
        listOf("supermercado", "mercado", "tienda", "carulla", "exito", "d1", "ara", "olimpica", "jumbo", "panaderia",
            "restaurante", "cafe", "comida", "pan", "leche", "galletas", "frutas", "domicilio", "rappi") to
            listOf("comida", "alimentacion", "alimentos", "mercado", "supermercado"),
        listOf("uber", "didi", "taxi", "transmilenio", "gasolina", "terpel", "primax", "peaje", "parqueadero", "bus") to
            listOf("transporte", "movilidad", "gasolina"),
        listOf("cine", "netflix", "spotify", "teatro", "concierto", "juego", "bar") to
            listOf("entretenimiento", "ocio", "diversion"),
        listOf("farmacia", "drogueria", "medicamento", "medicamentos", "medico", "clinica", "eps", "odontologia", "laboratorio") to
            listOf("salud", "farmacia"),
        listOf("ropa", "zara", "falabella", "tenis", "almacen", "centro comercial") to
            listOf("compras", "ropa"),
        listOf("arriendo") to listOf("hogar", "vivienda", "casa"),
    ) }

    /** Palabras que solo salen en recibos de servicios públicos, celular o internet. */
    private val serviceWords
        get() = serviceKinds.flatMap { it.first }

    /** Tipo de recibo de servicios → motivo que se sugiere. */
    private val serviceKinds: List<Pair<List<String>, String>> = listOf(
        listOf("claro", "movistar", "tigo", "wom", "comcel", "servicios moviles") to "Pago de celular",
        listOf("internet", "etb", "fibra", "banda ancha") to "Pago de internet",
        listOf("energia", "enel", "codensa", "kwh", "electrificadora", "air e", "celsia") to "Pago de luz",
        listOf("acueducto", "alcantarillado", "m3") to "Pago de agua",
        listOf("gas natural", "vanti", "gases") to "Pago de gas",
        listOf("epm", "servicios publicos") to "Pago de servicios públicos",
    )

    /** Empresas de servicios: si aparece una, ese es el comercio (la primera línea suele ser basura). */
    private val providers = listOf(
        "claro", "movistar", "tigo", "wom", "etb", "enel", "codensa", "vanti", "epm", "celsia", "air e", "acueducto",
    )

    /**
     * Arma el borrador. Lo que no se encuentre queda vacío o con un valor seguro
     * (la fecha de hoy) para que el usuario lo complete en "Confirmar".
     */
    fun parse(rawText: String, today: LocalDate, categories: List<MovementCategory>): ReceiptDraft {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val service = serviceReason(rawText)
        val merchant = provider(rawText) ?: findMerchant(lines)
        val category = suggestCategory(rawText, categories)
        return ReceiptDraft(
            merchant = merchant,
            amount = findTotal(lines),
            date = findDate(rawText, today) ?: today,
            categoryId = category?.id,
            reason = service ?: if (merchant.isBlank()) "" else "Compra en $merchant",
            rawText = rawText,
        )
    }

    // ---------------- Monto ----------------

    /** Etiquetas del total, de la más confiable a la menos (puntaje 100). */
    private val strongTotal = listOf(
        "total a pagar", "valor a pagar", "total pagar", "neto a pagar", "total factura", "valor total",
        "gran total", "total venta", "total compra", "importe total", "total general",
    )

    /**
     * Palabras de líneas que tienen números pero NO son el total: subtotal, impuestos,
     * descuentos, lo que el cliente pagó y su cambio, y datos del documento (NIT, teléfono…).
     */
    private val notTotalWords = setOf(
        "subtotal", "iva", "impuesto", "impuestos", "impoconsumo", "inc", "base", "descuento", "descuentos", "dcto",
        "ahorro", "ahorraste", "efectivo", "recibido", "cambio", "vueltas", "devuelta", "vuelto", "propina",
        "nit", "tel", "cel", "telefono", "celular", "whatsapp", "cc", "resolucion", "autorizacion", "cufe",
        "cajero", "caja", "item", "items", "articulo", "articulos", "unidades", "und", "cant", "cantidad",
        "codigo", "cod", "ref", "plu", "puntos", "saldo", "tarjeta", "pago", "paga", "no",
    )
    private val cashWords = setOf("efectivo", "recibido")
    private val changeWords = setOf("cambio", "vueltas", "devuelta", "vuelto")

    /**
     * El total de la factura. El OCR se equivoca, así que se buscan **candidatos** y se
     * escoge el más confiable:
     * 1. una línea con etiqueta de total ("TOTAL A PAGAR" vale más que "TOTAL"), tomando su
     *    último monto; si la etiqueta quedó sin número, el número de la fila de abajo;
     * 2. si no hay etiqueta: lo que el cliente pagó menos el cambio ("Efectivo − Cambio");
     * 3. si tampoco: el monto más grande **con formato de dinero** ("$", puntos de miles),
     *    sin contar las líneas de NIT, teléfono, fechas, efectivo, cambio, etc.
     */
    fun findTotal(lines: List<String>): Double? {
        val clean = lines.map(::fixOcr)

        // 1. Etiqueta de total.
        var best: Pair<Int, Double>? = null
        clean.forEachIndexed { i, line ->
            val score = totalScore(line) ?: return@forEachIndexed
            val amount = moneyIn(line, strict = false).lastOrNull { it >= MIN_AMOUNT }
                ?: clean.drop(i + 1).take(2)
                    .firstOrNull { next -> totalScore(next) == null && !isNotTotal(next) && moneyIn(next, strict = false).isNotEmpty() }
                    ?.let { next -> moneyIn(next, strict = false).last() }
                ?: return@forEachIndexed
            // Con el mismo puntaje gana el de más abajo (el total va al final de la factura).
            if (amount >= MIN_AMOUNT && (best == null || score >= best!!.first)) best = score to amount
        }
        best?.let { return it.second }

        // 2. Efectivo − Cambio.
        val cash = labeledAmount(clean, cashWords)
        val change = labeledAmount(clean, changeWords)
        if (cash != null && change != null && cash > change) return cash - change

        // 3. El monto más grande con formato de dinero.
        val usable = clean.filterNot(::isNotTotal)
        val money = usable.flatMap { moneyIn(it, strict = true) }.filter { it >= MIN_AMOUNT }
        if (money.isNotEmpty()) return money.max()
        return usable.flatMap { moneyIn(it, strict = false) }.filter { it in MIN_AMOUNT..MAX_PLAIN }.maxOrNull()
    }

    /** 100 = "TOTAL A PAGAR"…, 60 = "TOTAL" solo; `null` = no es la línea del total. */
    private fun totalScore(line: String): Int? {
        val l = DaiBrain.normalize(line)
        if (l.contains("subtotal") || l.contains("sub total")) return null
        if (strongTotal.any { l.contains(it) }) return 100
        val words = l.split(" ")
        if ("total" !in words) return null
        return if (words.any { it in notTotalWords }) null else 60
    }

    private fun isNotTotal(line: String): Boolean =
        DaiBrain.normalize(line).split(" ").any { it in notTotalWords }

    /** El último monto de la primera línea que tenga una de [words] ("Efectivo $20.000"). */
    private fun labeledAmount(lines: List<String>, words: Set<String>): Double? =
        lines.firstOrNull { line -> DaiBrain.normalize(line).split(" ").any { it in words } }
            ?.let { moneyIn(it, strict = false).lastOrNull() }

    /**
     * Corrige confusiones típicas del OCR dentro de los números: "$17.9OO" → "$17.900",
     * "S17.900" → "$17.900" (la S era el signo $), "17. 900" → "17.900".
     */
    fun fixOcr(line: String): String {
        var t = line
        // "S17.900" → "$17.900": una S suelta pegada a un número era el signo $.
        t = Regex("""(?<![A-Za-z])S(?=\s?\d)""").replace(t) { "$" }
        // Letras que el OCR confunde con dígitos, solo DENTRO de un número: "17.9OO", "1l.500".
        repeat(3) {
            t = Regex("""(?<=[\d.,$])[Oo](?=[\d.,Oo]|\b)""").replace(t) { "0" }
            t = Regex("""(?<=[\d.,$])[lI|](?=[\d.,])""").replace(t) { "1" }
        }
        // "TOTAL17.900" → "TOTAL 17.900" y "17. 900" → "17.900".
        t = Regex("""([A-Za-z]{3,})(?=[$]|\d)""").replace(t) { it.value + " " }
        return Regex("""(\d)([.,])\s(\d{3})(?!\d)""").replace(t) { m ->
            m.groupValues[1] + m.groupValues[2] + m.groupValues[3]
        }
    }

    /** Todos los montos de una línea ("Agua 1.000 $3.000" → [1000, 3000]). */
    fun amountsIn(line: String): List<Double> = moneyIn(line, strict = false)

    /**
     * Los montos de la línea, sin contar partes de fechas ("12/04/2026"), horas ("16:24"),
     * códigos pegados a letras ("A123") ni el NIT ("900123456-7").
     * Con [strict] solo cuenta lo que tiene **formato de dinero**: "$", puntos de miles o
     * centavos ("$8.500", "17.900", "45,50"), no un número suelto como "2026".
     */
    private fun moneyIn(line: String, strict: Boolean): List<Double> =
        AMOUNT.findAll(line).filter { m ->
            // El patrón puede empezar con un espacio (" 17.900"): se mira el carácter antes del número.
            val start = m.range.first + m.value.indexOfFirst { !it.isWhitespace() }
            val before = line.getOrNull(start - 1)
            val after = line.getOrNull(m.range.last + 1)
            val afterNext = line.getOrNull(m.range.last + 2)
            val glued = (before != null && (before.isLetter() || before in "-/:#")) ||
                (after != null && (after.isLetter() || after in "/:%")) ||
                (after == '-' && afterNext?.isDigit() == true)
            !glued && (!strict || looksLikeMoney(m.value))
        }.mapNotNull { parseAmount(it.value) }.toList()

    private fun looksLikeMoney(token: String): Boolean =
        token.contains('$') || Regex("""\d{1,3}(?:[.,]\d{3})+""").containsMatchIn(token) || Regex("""[.,]\d{2}$""").containsMatchIn(token)

    private val AMOUNT = Regex("""\$?\s?\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\$?\s?\d+(?:[.,]\d{1,2})?""")

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

    /** Líneas con fechas que NO son la de la compra (la resolución de la DIAN y su vigencia). */
    private val notPurchaseDate = setOf(
        "vigencia", "resolucion", "res", "autorizacion", "autoriza", "habilita", "habilitacion", "rango",
        "desde", "hasta", "vence", "vencimiento", "validez", "prefijo",
    )

    /** Líneas que dicen de forma explícita que es la fecha de la factura. */
    private val purchaseDate = setOf("fecha", "creacion", "emision", "expedicion", "generacion", "compra", "venta")

    /** Recibos de servicios (luz, agua, celular…): la fecha que cuenta es la límite de pago. */
    private val dueDate = setOf("limite", "oportuno")

    /** Meses escritos con letras ("26-Ago-2026", "8 oct, 2026", "OCT/02/2026"). */
    private val months = mapOf(
        "ene" to 1, "jan" to 1, "feb" to 2, "mar" to 3, "abr" to 4, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "ago" to 8, "aug" to 8, "sep" to 9, "set" to 9, "oct" to 10, "nov" to 11,
        "dic" to 12, "dec" to 12,
    )

    /**
     * La fecha de la factura, con la misma regla del proyecto de IA:
     * - **recibos de servicios:** la **fecha límite de pago** ("Fecha límite de pago",
     *   "Pago oportuno"), si no es futura;
     * - **las demás facturas:** la fecha de emisión. Una factura electrónica trae varias
     *   fechas (la de la compra, la de la resolución de la DIAN y su vigencia), así que:
     *   1. se descartan las líneas de resolución, vigencia, "desde/hasta" y vencimiento;
     *   2. se prefieren las líneas que dicen "Fecha", "Fecha creación", "Emisión"…;
     *   3. entre las que quedan, la **más reciente que no sea futura** (y de los últimos 2 años).
     *
     * Si la etiqueta quedó sola en su fila ("FECHA LÍMITE DE PAGO:"), la fecha se busca en la
     * fila de abajo, igual que con el total. Se aceptan 12/04/2026, 12-04-26, 2026-04-12,
     * 26-Ago-2026, 26 de agosto de 2026 y OCT/02/2026.
     */
    fun findDate(text: String, today: LocalDate): LocalDate? {
        data class Candidate(val date: LocalDate, val labeled: Boolean, val due: Boolean)
        val candidates = mutableListOf<Candidate>()
        val lines = text.lines()
        for ((i, line) in lines.withIndex()) {
            val words = DaiBrain.normalize(line).split(" ")
            val due = words.any { it in dueDate }
            if (!due && words.any { it in notPurchaseDate }) continue
            val labeled = due || words.any { it in purchaseDate }
            var dates = datesIn(line)
            if (dates.isEmpty() && labeled && i + 1 < lines.size) dates = datesIn(lines[i + 1])
            dates.forEach { candidates.add(Candidate(it, labeled, due)) }
        }
        val valid = candidates.filter { !it.date.isAfter(today) && it.date.isAfter(today.minusYears(2)) }
        valid.filter { it.due }.maxOfOrNull { it.date }?.let { return it }
        return (valid.filter { it.labeled }.ifEmpty { valid }).maxOfOrNull { it.date }
    }

    /** Todas las fechas de una línea, en cualquiera de los formatos aceptados. */
    private fun datesIn(line: String): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        Regex("""\b(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\b""").findAll(line).forEach { m ->
            safeDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())?.let(dates::add)
        }
        Regex("""\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})\b""").findAll(line).forEach { m ->
            var year = m.groupValues[3].toInt()
            if (year < 100) year += 2000
            // En Colombia la factura dice día/mes/año.
            safeDate(year, m.groupValues[2].toInt(), m.groupValues[1].toInt())?.let(dates::add)
        }
        // Mes con letras: se busca sin tildes y en minúscula.
        val plain = java.text.Normalizer.normalize(line.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        // 26-Ago-2026 · 26 ago 2026 · 26 de agosto de 2026 · 8 oct, 2026
        Regex("""(?<!\d)(\d{1,2})[\s./-]*(?:de\s+)?([a-z]{3,10})\.?,?[\s./-]*(?:del?\s+)?(\d{4})(?!\d)""")
            .findAll(plain).forEach { m ->
                val month = months[m.groupValues[2].take(3)] ?: return@forEach
                safeDate(m.groupValues[3].toInt(), month, m.groupValues[1].toInt())?.let(dates::add)
            }
        // OCT/02/2026 · ago 26, 2026
        Regex("""(?<![a-z])([a-z]{3,10})\.?[\s/-]*(\d{1,2}),?[\s/-]+(\d{4})(?!\d)""")
            .findAll(plain).forEach { m ->
                val month = months[m.groupValues[1].take(3)] ?: return@forEach
                safeDate(m.groupValues[3].toInt(), month, m.groupValues[2].toInt())?.let(dates::add)
            }
        return dates
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
            if (hints.any { t.contains(" $it ") }) {
                expenses.firstOrNull { c -> DaiBrain.normalize(c.name) in names }?.let { return it }
            }
        }
        return null
    }

    /** "Pago de luz", "Pago de celular"… si es un recibo de servicios; `null` si no. */
    fun serviceReason(text: String): String? {
        val t = " ${DaiBrain.normalize(text)} "
        return serviceKinds.firstOrNull { (words, _) -> words.any { t.contains(" $it ") } }?.second
    }

    /** La empresa de servicios del recibo ("Claro", "Enel"…), o `null`. */
    private fun provider(text: String): String? {
        val t = " ${DaiBrain.normalize(text)} "
        val name = providers.firstOrNull { t.contains(" $it ") } ?: return null
        return name.split(" ").joinToString(" ") { w -> if (w.length <= 3 && w != "air") w.uppercase() else w.replaceFirstChar(Char::titlecase) }
    }

    // ---------------- Aprobación ----------------

    /** Primer problema del borrador antes de guardar (`null` = se puede guardar). */
    /**
     * El título con el que se guarda el gasto. "Confirmar" ya no muestra el comercio
     * (solo monto, fecha, categoría y motivo), así que se usa el comercio leído; si no hay,
     * el motivo; si no, la categoría; y si tampoco, "Dai".
     */
    fun titleFor(draft: ReceiptDraft, categoryName: String?): String =
        listOf(draft.merchant, draft.reason, categoryName.orEmpty(), DEFAULT_TITLE)
            .first { it.isNotBlank() }
            .trim()
            .take(MERCHANT_MAX)

    fun validate(draft: ReceiptDraft, today: LocalDate): ReceiptProblem? = when {
        draft.amount == null || draft.amount <= 0.0 || draft.amount > BudgetRules.MAX_AMOUNT -> ReceiptProblem.Amount
        draft.date.isAfter(today) -> ReceiptProblem.FutureDate
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

    /** Un número suelto (sin "$" ni puntos) más grande que esto es un código, no dinero. */
    private const val MAX_PLAIN = 9_999_999.0
}

/** Por qué no se puede guardar todavía la factura. */
enum class ReceiptProblem { Amount, FutureDate }
