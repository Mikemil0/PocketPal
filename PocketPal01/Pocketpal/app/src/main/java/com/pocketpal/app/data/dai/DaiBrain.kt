package com.pocketpal.app.data.dai

import com.pocketpal.app.data.budget.BudgetRules
import com.pocketpal.app.data.movement.MovementRules
import com.pocketpal.app.ui.MoneyFormat
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

/** Lo que el usuario quiere, según lo que escribió o dijo. */
enum class DaiIntent { Greeting, Thanks, Help, Spending, Patterns, Goals, Advice, Alerts, Learn, Unknown }

/** Temas que Dai sabe explicar en "Aprende con Dai". */
enum class DaiTopic { CompoundInterest, Budget, Saving, EmergencyFund, Inflation, Credit, Rule503020, AntExpenses }

/**
 * El "cerebro" de Dai: **reglas**, sin Android, sin internet y sin modelos externos.
 *
 * 1. [detectIntent] entiende QUÉ pide el usuario buscando palabras clave en el texto ya
 *    normalizado (sin tildes y en minúsculas).
 * 2. [reply] arma la respuesta con los DATOS REALES del usuario ([FinanceSnapshot]):
 *    movimientos, presupuestos y metas que vienen de Supabase.
 *
 * Por ser funciones puras se prueban con JUnit (DaiBrainTest) sin emulador.
 */
object DaiBrain {

    const val CHIP_SUMMARY = "Ver resumen"
    const val CHIP_ADVICE = "Consejos"
    const val CHIP_PATTERNS = "Ver patrones"
    const val CHIP_GOALS = "Revisar mis metas"
    const val CHIP_LEARN = "Explícame algo de finanzas"

    /** Las 4 sugerencias de "¿En qué puedo ayudarte hoy?" (chat nuevo). */
    val starterPrompts = listOf(
        "¿En qué estoy gastando más?",
        "¿Cómo voy con mi presupuesto?",
        "Dame un consejo para ahorrar",
        CHIP_LEARN,
    )

    /** Máximo de caracteres de un mensaje del usuario (la tabla acepta hasta 2000). */
    const val MAX_MESSAGE = 500

    /** Días que mira la "Alerta de gasto" ("en los últimos 3 días…"). */
    const val ALERT_DAYS = 3

    /** Desde qué % de un presupuesto Dai lo considera "para revisar". */
    const val WARNING_PERCENT = 80

    // ---------------- 1 · Entender ----------------

    /** "¿Cómo VOY con mi PRESUPUESTO?" → "como voy con mi presupuesto". */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")      // quita las tildes
            .replace(Regex("[^a-z0-9%$ ]"), " ")  // quita signos de puntuación
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * Qué pide el usuario. El ORDEN importa: "dame un consejo para ahorrar" tiene
     * "ahorrar" (metas) pero es un consejo; "explícame qué es un presupuesto" tiene
     * "presupuesto" pero es una lección.
     */
    fun detectIntent(text: String): DaiIntent {
        val t = normalize(text)
        if (t.isEmpty()) return DaiIntent.Unknown
        fun has(vararg words: String) = words.any { contains(t, it) }
        return when {
            has("que es", "que son", "que significa", "explica*", "aprend*", "ensena*", "como funciona") ||
                has("interes compuesto", "inflacion", "50 30 20", "gastos hormiga", "fondo de emergencia") -> DaiIntent.Learn
            has("consejo*", "recomienda*", "recomendacion*", "tip*", "que hago", "como ahorro", "ayudame a ahorrar") -> DaiIntent.Advice
            has("alerta*", "deberia saber", "advertencia*", "algo raro", "algo importante") -> DaiIntent.Alerts
            has("patron*", "habito*", "alcanza", "por que gasto", "por que estoy gastando", "en que gasto", "gastando mas", "mayor gasto") -> DaiIntent.Patterns
            has("meta*", "ahorr*") -> DaiIntent.Goals
            has("gast*", "presupuest*", "como voy", "resumen", "movimient*", "moviendo", "dinero", "plata", "cuanto llevo", "limite") ->
                DaiIntent.Spending
            has("gracias", "genial", "perfecto") -> DaiIntent.Thanks
            has("hola", "buenas", "buenos dias", "hey", "que tal") -> DaiIntent.Greeting
            has("ayuda*", "que puedes", "que sabes", "opciones") -> DaiIntent.Help
            else -> DaiIntent.Unknown
        }
    }

    /**
     * ¿El texto tiene esa palabra o frase COMPLETA? "que es" no debe encontrarse dentro de
     * "que estoy". Con `*` al final se acepta cualquier terminación: "gast*" = gasto,
     * gastos, gastando…
     */
    fun contains(normalizedText: String, word: String): Boolean {
        val padded = " $normalizedText "
        return if (word.endsWith("*")) padded.contains(" ${word.dropLast(1)}") else padded.contains(" $word ")
    }

    /** Tema de finanzas mencionado en el texto (para "Aprende con Dai"). */
    fun topicOf(text: String): DaiTopic? {
        val t = normalize(text)
        return when {
            t.contains("interes") -> DaiTopic.CompoundInterest
            t.contains("inflacion") -> DaiTopic.Inflation
            t.contains("emergencia") -> DaiTopic.EmergencyFund
            t.contains("50 30 20") || t.contains("503020") || t.contains("regla") -> DaiTopic.Rule503020
            t.contains("hormiga") -> DaiTopic.AntExpenses
            t.contains("tarjeta") || t.contains("credito") || t.contains("deuda") -> DaiTopic.Credit
            t.contains("presupuesto") -> DaiTopic.Budget
            t.contains("ahorr") -> DaiTopic.Saving
            else -> null
        }
    }

    // ---------------- 2 · Responder ----------------

    fun reply(text: String, data: FinanceSnapshot, zone: ZoneId, currency: String = "COP"): DaiReply =
        when (detectIntent(text)) {
            DaiIntent.Greeting -> greeting(data.userName)
            DaiIntent.Thanks -> DaiReply("¡Con gusto! Aquí estoy si necesitas algo más.", chips = listOf(CHIP_SUMMARY, CHIP_ADVICE))
            DaiIntent.Help -> help()
            DaiIntent.Spending -> spending(data, currency)
            DaiIntent.Patterns -> patterns(data, zone, currency)
            DaiIntent.Goals -> goals(data, currency)
            DaiIntent.Advice -> advice(data, currency)
            DaiIntent.Alerts -> alerts(data, zone, currency)
            DaiIntent.Learn -> lesson(topicOf(text))
            DaiIntent.Unknown -> DaiReply(
                "Disculpa, no te entendí bien. ¿Puedes repetirlo? Puedes preguntarme, por ejemplo, «¿cómo voy con mi presupuesto?».",
                chips = listOf(CHIP_SUMMARY, CHIP_ADVICE, CHIP_GOALS),
            )
        }

    fun greeting(name: String): DaiReply {
        val first = name.trim().substringBefore(" ")
        val hello = if (first.isBlank()) "¡Hola! 👋" else "¡Hola, $first! 👋"
        return DaiReply("$hello ¿En qué puedo ayudarte?", chips = listOf(CHIP_SUMMARY, CHIP_ADVICE, CHIP_GOALS))
    }

    fun help(): DaiReply = DaiReply(
        "Puedo revisar tus gastos y tu presupuesto, seguir tus metas, avisarte si algo se sale de lo normal, " +
            "darte consejos y explicarte temas de finanzas. También leo tus facturas con la cámara 📷.",
        chips = listOf(CHIP_SUMMARY, CHIP_ADVICE, CHIP_GOALS, CHIP_LEARN),
    )

    /** "Todavía no tengo suficientes datos para darte una respuesta precisa." */
    fun missingData(data: FinanceSnapshot): DaiReply = DaiReply(
        "Todavía no tengo suficientes datos para darte una respuesta precisa. " +
            "Para ayudarte mejor, necesito conocer un poco más sobre tus finanzas.",
        card = DaiCard.MissingData(
            needsIncome = data.monthIncome <= 0.0,
            needsExpenses = data.monthExpenses.isEmpty(),
            needsGoals = data.activeGoals.isEmpty(),
        ),
    )

    /** ¿Hay con qué responder sobre gastos? (si no, "Todavía no tengo suficientes datos"). */
    fun hasSpendingData(data: FinanceSnapshot): Boolean = data.monthExpenses.isNotEmpty()

    /**
     * Resumen del mes (Figma "02_Chat_Principal 2"): gasto acumulado contra la suma de los
     * presupuestos, y las categorías principales con su % del total gastado.
     */
    fun spending(data: FinanceSnapshot, currency: String = "COP"): DaiReply {
        if (!hasSpendingData(data)) return missingData(data)
        val spent = data.monthExpenses.sumOf { it.amount }
        val budget = data.budgets.sumOf { it.budget.amountLimit }
        val percent = BudgetRules.percentUsed(spent, budget)
        val card = DaiCard.Spending(spent, budget, percent, categoryShares(data))
        val toReview = budgetToReview(data)
        val text = when {
            budget <= 0.0 ->
                "Este mes llevas ${money(spent, currency)} en gastos. Aún no tienes presupuestos: " +
                    "si creas uno te puedo decir si vas bien."
            percent >= 100 ->
                "Hasta hoy llevas el $percent % de tu presupuesto mensual: ya te pasaste por ${money(spent - budget, currency)}."
            toReview != null ->
                "Hasta hoy llevas el $percent % de tu presupuesto mensual. Vas bien, pero hay una categoría que " +
                    "conviene revisar: ${toReview.budget.name} (${toReview.percent} %)."
            else -> "Hasta hoy llevas el $percent % de tu presupuesto mensual. ¡Vas muy bien!"
        }
        return DaiReply(text, card, listOf(CHIP_PATTERNS, CHIP_ADVICE))
    }

    /** Hasta 4 categorías de gasto (de mayor a menor) y el resto como "Otros". */
    fun categoryShares(data: FinanceSnapshot, max: Int = 4): List<CategoryShare> {
        val expenses = data.monthExpenses
        val total = expenses.sumOf { it.amount }
        if (total <= 0.0) return emptyList()
        val byId = data.categories.associateBy { it.id }
        val totals = MovementRules.categoryTotals(expenses)
        val named = totals.filter { it.categoryId != null && byId[it.categoryId] != null }
        val top = named.take(max - 1)
        val restAmount = total - top.sumOf { it.total }
        val shares = top.map {
            val category = byId.getValue(it.categoryId!!)
            CategoryShare(category.name, category.color, it.total, (it.total / total * 100).roundToInt())
        }
        return if (restAmount > 0.5) {
            shares + CategoryShare(OTHERS, OTHERS_COLOR, restAmount, (restAmount / total * 100).roundToInt())
        } else {
            shares
        }
    }

    /** El presupuesto más "apretado" que pasa del 80 % (o `null` si todos van bien). */
    fun budgetToReview(data: FinanceSnapshot) =
        data.budgets.filter { it.percent >= WARNING_PERCENT }.maxByOrNull { it.percent }

    /** "Ver patrones": la categoría que más pesa, el día de la semana que más gastas y el promedio diario. */
    fun patterns(data: FinanceSnapshot, zone: ZoneId, currency: String = "COP"): DaiReply {
        if (!hasSpendingData(data)) return missingData(data)
        val shares = categoryShares(data)
        val parts = mutableListOf<String>()
        shares.firstOrNull { it.name != OTHERS }?.let {
            parts += "${it.name} es tu mayor categoría: se lleva el ${it.percent} % de tus gastos del mes."
        }
        topWeekday(data, zone)?.let { parts += "Los ${weekdayName(it)} es cuando más gastas." }
        val day = data.today.dayOfMonth
        val avg = data.monthExpenses.sumOf { it.amount } / day
        parts += "En promedio gastas ${money(avg, currency)} por día."
        compareWithLastMonth(data, zone)?.let { change ->
            parts += when {
                change > 0 -> "Vas $change % por encima de lo que llevabas el mes pasado a esta altura."
                change < 0 -> "Vas ${-change} % por debajo de lo que llevabas el mes pasado a esta altura. 👏"
                else -> "Vas igual que el mes pasado a esta altura."
            }
        }
        val spent = data.monthExpenses.sumOf { it.amount }
        val budget = data.budgets.sumOf { it.budget.amountLimit }
        val card = DaiCard.Spending(spent, budget, BudgetRules.percentUsed(spent, budget), shares)
        return DaiReply(parts.joinToString(" "), card, listOf(CHIP_ADVICE, CHIP_SUMMARY))
    }

    /** Día de la semana con más gasto acumulado en el mes. */
    fun topWeekday(data: FinanceSnapshot, zone: ZoneId): DayOfWeek? =
        data.monthExpenses
            .groupBy { it.localDate(zone)?.dayOfWeek }
            .filterKeys { it != null }
            .maxByOrNull { (_, list) -> list.sumOf { it.amount } }
            ?.key

    /**
     * % de cambio frente al mes pasado **hasta el mismo día** (comparar el mes completo
     * pasado con el mes en curso siempre daría "gastas menos").
     */
    fun compareWithLastMonth(data: FinanceSnapshot, zone: ZoneId): Int? {
        val day = data.today.dayOfMonth
        val previous = data.previousMonthMovements
            .filter { it.isExpense }
            .filter { (it.localDate(zone)?.dayOfMonth ?: Int.MAX_VALUE) <= day }
            .sumOf { it.amount }
        return MovementRules.percentChange(data.monthExpenses.sumOf { it.amount }, previous)
    }

    /** Metas activas (Figma "05_Metas_Seguimiento"). */
    fun goals(data: FinanceSnapshot, currency: String = "COP"): DaiReply {
        val active = data.activeGoals
        if (active.isEmpty()) {
            return DaiReply(
                "Aún no tienes metas activas. Crear una meta me ayuda a acompañarte en tu ahorro.",
                chips = listOf(CHIP_SUMMARY, CHIP_ADVICE),
            )
        }
        val shares = goalShares(data)
        val best = shares.maxByOrNull { it.percent }!!
        val missing = (best.target - best.saved).coerceAtLeast(0.0)
        val text = if (best.percent >= 50) {
            "¡Vas muy bien! Tu meta ${best.name} ya va en ${best.percent} %. Te faltan ${money(missing, currency)}."
        } else {
            "Tu meta ${best.name} va en ${best.percent} %. Te faltan ${money(missing, currency)}: " +
                "un aporte pequeño cada semana hace la diferencia."
        }
        return DaiReply(text, DaiCard.Goals(shares), listOf(CHIP_ADVICE, CHIP_SUMMARY))
    }

    fun goalShares(data: FinanceSnapshot): List<GoalShare> = data.activeGoals.map {
        GoalShare(it.name, it.savedAmount, it.targetAmount, BudgetRules.percentUsed(it.savedAmount, it.targetAmount).coerceAtMost(100))
    }

    /** "Dame un consejo": primero lo urgente (presupuestos pasados), luego lo importante. */
    fun advice(data: FinanceSnapshot, currency: String = "COP"): DaiReply {
        if (!hasSpendingData(data)) return missingData(data)
        val exceeded = data.budgets.filter { it.exceeded }.maxByOrNull { it.over }
        val near = budgetToReview(data)
        val top = categoryShares(data).firstOrNull { it.name != OTHERS }
        val daysLeft = YearMonth.from(data.today).lengthOfMonth() - data.today.dayOfMonth + 1
        val text = when {
            exceeded != null ->
                "Te pasaste ${money(exceeded.over, currency)} en ${exceeded.budget.name}. Intenta pausar los gastos " +
                    "de esa categoría lo que queda del mes y revisa si alguno se puede cambiar por una opción más barata."
            near != null ->
                "Tu presupuesto de ${near.budget.name} va en ${near.percent} %. Te quedan ${money(near.remaining, currency)} " +
                    "para $daysLeft días: unos ${money(near.remaining / daysLeft, currency)} por día."
            top != null && top.percent >= 40 ->
                "${top.name} se lleva el ${top.percent} % de tus gastos. Si la bajas un 10 % ahorrarías " +
                    "${money(top.amount * 0.1, currency)} este mes."
            else ->
                "Separa una parte de tu ingreso apenas te llegue, antes de gastar. Una guía sencilla es la regla " +
                    "50/30/20: 50 % necesidades, 30 % gustos y 20 % ahorro."
        }
        val goalTip = data.activeGoals.firstOrNull()?.let { " Y si te sobra algo, súmaselo a tu meta ${it.name}." }.orEmpty()
        return DaiReply(text + goalTip, chips = listOf(CHIP_SUMMARY, CHIP_GOALS))
    }

    /**
     * "Alerta de gasto" (Figma "04_Alertas_Recomendaciones"): la categoría con más gasto en
     * los últimos [ALERT_DAYS] días, qué % de su presupuesto es y cómo terminaría el mes
     * si sigue al mismo ritmo.
     */
    fun alertCard(data: FinanceSnapshot, zone: ZoneId): DaiCard.Alert? {
        val from = data.today.minusDays((ALERT_DAYS - 1).toLong())
        val recent = data.monthMovements.filter { m ->
            m.isExpense && m.categoryId != null && (m.localDate(zone)?.let { !it.isBefore(from) && !it.isAfter(data.today) } ?: false)
        }
        val (categoryId, list) = recent.groupBy { it.categoryId!! }.maxByOrNull { (_, l) -> l.sumOf { it.amount } } ?: return null
        val amount = list.sumOf { it.amount }
        val name = data.categories.firstOrNull { it.id == categoryId }?.name ?: OTHERS
        val status = data.budgets.firstOrNull { it.budget.categoryId == categoryId }
        val percentOfBudget = status?.let { BudgetRules.percentUsed(amount, it.budget.amountLimit) }
        val projectedOver = status?.let {
            val month = YearMonth.from(data.today)
            val projected = it.spent / data.today.dayOfMonth * month.lengthOfMonth()
            (projected - it.budget.amountLimit).takeIf { over -> over > 0.0 }
        }
        return DaiCard.Alert(name, amount, ALERT_DAYS, percentOfBudget, projectedOver)
    }

    fun alerts(data: FinanceSnapshot, zone: ZoneId, currency: String = "COP"): DaiReply {
        if (!hasSpendingData(data)) return missingData(data)
        val card = alertCard(data, zone)
            ?: return DaiReply("No veo nada fuera de lo normal en tus últimos días. ¡Sigue así! 💪", chips = listOf(CHIP_SUMMARY, CHIP_ADVICE))
        val budgetPart = card.percentOfBudget?.let { " Eso representa el $it % de tu presupuesto de esta categoría." }.orEmpty()
        val impact = card.projectedOver?.let { " Si sigues a este ritmo podrías terminar el mes ${money(it, currency)} por encima del presupuesto." }.orEmpty()
        return DaiReply(
            "Noté que en los últimos ${card.days} días has gastado ${money(card.amount, currency)} en ${card.category}.$budgetPart$impact",
            card,
            listOf(CHIP_ADVICE, CHIP_SUMMARY),
        )
    }

    /** "Aprende con Dai" (Figma "08_Educacion_Integrada"). */
    fun lesson(topic: DaiTopic?): DaiReply {
        if (topic == null) {
            return DaiReply(
                "¡Claro! ¿Qué te gustaría aprender? Puedo explicarte el interés compuesto, cómo hacer un presupuesto, " +
                    "el fondo de emergencia, la inflación, la regla 50/30/20 o los gastos hormiga.",
                chips = listOf("¿Qué es el interés compuesto?", "¿Qué es un fondo de emergencia?", "¿Qué es la regla 50/30/20?"),
            )
        }
        val (text, card) = when (topic) {
            DaiTopic.CompoundInterest -> "Es cuando tus ganancias también empiezan a generar ganancias con el tiempo. " +
                "Si ahorras y reinviertes lo que ganas, cada mes ganas sobre una base más grande." to
                DaiCard.Lesson("Interés compuesto", "El poder del interés compuesto", 4, "interés compuesto explicado fácil")
            DaiTopic.Budget -> "Un presupuesto es un plan de cuánto vas a gastar en cada cosa durante el mes. " +
                "Te ayuda a decidir antes de gastar, no después." to
                DaiCard.Lesson("Presupuesto", "Cómo hacer un presupuesto personal", 6, "cómo hacer un presupuesto personal")
            DaiTopic.Saving -> "Ahorrar es apartar una parte de lo que recibes para el futuro. El truco es hacerlo " +
                "apenas te llega el dinero, como si fuera un gasto fijo." to
                DaiCard.Lesson("Ahorro", "Hábitos para ahorrar cada mes", 5, "hábitos para ahorrar dinero")
            DaiTopic.EmergencyFund -> "Es un ahorro guardado solo para imprevistos (una enfermedad, quedarte sin trabajo). " +
                "Lo ideal es que cubra entre 3 y 6 meses de tus gastos." to
                DaiCard.Lesson("Fondo de emergencia", "Por qué necesitas un fondo de emergencia", 5, "fondo de emergencia finanzas personales")
            DaiTopic.Inflation -> "La inflación es la subida general de los precios: con el mismo dinero compras menos " +
                "cosas que antes. Por eso el dinero quieto pierde valor." to
                DaiCard.Lesson("Inflación", "La inflación explicada fácil", 4, "inflación explicada fácil")
            DaiTopic.Credit -> "Una tarjeta de crédito es dinero prestado: si no pagas el total a tiempo, pagas intereses. " +
                "Úsala solo para lo que ya tienes con qué pagar." to
                DaiCard.Lesson("Crédito", "Cómo usar bien la tarjeta de crédito", 6, "cómo usar bien la tarjeta de crédito")
            DaiTopic.Rule503020 -> "Es una forma fácil de repartir tu ingreso: 50 % para necesidades, 30 % para gustos " +
                "y 20 % para ahorro o deudas." to
                DaiCard.Lesson("Regla 50/30/20", "La regla 50/30/20", 3, "regla 50 30 20 finanzas")
            DaiTopic.AntExpenses -> "Son los gastos pequeños de todos los días (un café, un snack, un domicilio) que " +
                "parecen nada pero juntos suman mucho al mes." to
                DaiCard.Lesson("Gastos hormiga", "Cómo detectar los gastos hormiga", 4, "gastos hormiga cómo evitarlos")
        }
        return DaiReply("Claro. $text", card, listOf(CHIP_GOALS, CHIP_ADVICE))
    }

    /** Respuesta rápida asociada a cada tarjeta (al volver a abrir el historial). */
    fun chipsFor(card: DaiCard?): List<String> = when (card) {
        is DaiCard.Spending -> listOf(CHIP_PATTERNS, CHIP_ADVICE)
        is DaiCard.Goals, is DaiCard.Lesson -> listOf(CHIP_ADVICE, CHIP_SUMMARY)
        is DaiCard.Alert -> listOf(CHIP_ADVICE, CHIP_SUMMARY)
        is DaiCard.ReceiptSaved -> listOf(CHIP_SUMMARY)
        is DaiCard.MissingData, null -> emptyList()
    }

    // ---------------- Apoyo ----------------

    const val OTHERS = "Otros"
    const val OTHERS_COLOR = "#A0AEC0"

    fun money(amount: Double, currency: String = "COP"): String = MoneyFormat.format(amount, currency)

    fun weekdayName(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "lunes"
        DayOfWeek.TUESDAY -> "martes"
        DayOfWeek.WEDNESDAY -> "miércoles"
        DayOfWeek.THURSDAY -> "jueves"
        DayOfWeek.FRIDAY -> "viernes"
        DayOfWeek.SATURDAY -> "sábados"
        DayOfWeek.SUNDAY -> "domingos"
    }
}
