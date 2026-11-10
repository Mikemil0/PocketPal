package com.pocketpal.app.data.dai

import com.pocketpal.app.data.movement.MovementCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.time.LocalDate

/**
 * Servidor de Dai (proyecto de IA): recibe la foto de la factura y devuelve los 4 campos.
 * Allí corre el modelo que ganó la comparación de OCR (PaddleOCR), las reglas de monto y
 * fecha, la Regresión logística de la categoría y el k-NN del motivo.
 *
 * Si no hay dirección configurada o el servidor no responde, la app sigue funcionando con
 * el OCR del teléfono (ML Kit + [ReceiptParser]).
 */
interface ReceiptAnalyzer {
    /** `null` = no hay servidor o no respondió: se usa la lectura del teléfono. */
    suspend fun analyze(jpeg: ByteArray): ServerReceipt?
}

/** Lo que respondió el servidor para una factura. */
data class ServerReceipt(
    val amount: Double?,
    val date: LocalDate?,
    /** Una de las categorías del dataset de entrenamiento (por ejemplo "Supermercado"). */
    val category: String?,
    val reason: String,
    val merchant: String,
)

/** Cliente HTTP del servidor de Dai (sin librerías extra: `HttpURLConnection`). */
class HttpDaiServer(private val baseUrl: suspend () -> String) : ReceiptAnalyzer {

    override suspend fun analyze(jpeg: ByteArray): ServerReceipt? {
        val base = baseUrl().trim().trimEnd('/')
        if (!base.startsWith("http")) return null
        return withContext(Dispatchers.IO) {
            val boundary = "PocketPalDai${System.currentTimeMillis()}"
            val connection = (URL("$base/analizar").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 120_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
            try {
                connection.outputStream.use { out ->
                    out.write(
                        ("--$boundary\r\n" +
                            "Content-Disposition: form-data; name=\"foto\"; filename=\"factura.jpg\"\r\n" +
                            "Content-Type: image/jpeg\r\n\r\n").toByteArray(),
                    )
                    out.write(jpeg)
                    out.write("\r\n--$boundary--\r\n".toByteArray())
                }
                if (connection.responseCode !in 200..299) return@withContext null
                parse(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        fun parse(json: String): ServerReceipt {
            val j = JSONObject(json)
            return ServerReceipt(
                amount = if (j.isNull("monto")) null else j.getDouble("monto"),
                date = if (j.isNull("fecha")) null else runCatching { LocalDate.parse(j.getString("fecha")) }.getOrNull(),
                category = j.optString("categoria").takeIf { it.isNotBlank() },
                reason = j.optString("motivo"),
                merchant = j.optString("comercio"),
            )
        }
    }
}

/**
 * Convierte la respuesta del servidor en el borrador de "Confirmar". Es puro: se prueba con JUnit.
 *
 * Reglas:
 * - Monto, motivo y categoría del servidor reemplazan a los del teléfono cuando existen.
 * - La fecha del servidor se usa si no es futura (la app no permite gastos con fecha futura,
 *   por ejemplo la fecha límite de un recibo que aún no vence).
 * - El comercio: el que encontró el teléfono y, si no encontró ninguno, el del servidor.
 */
object DaiServerMapping {

    /** Categorías del dataset → palabras con las que se buscan en las categorías del usuario. */
    private val synonyms: Map<String, List<String>> = mapOf(
        "supermercado" to listOf("supermercado", "mercado", "comida", "alimentacion"),
        "restaurantes y domicilios" to listOf("restaurante", "domicilio", "comida", "alimentacion"),
        "transporte" to listOf("transporte", "gasolina", "movilidad"),
        "servicios" to listOf("servicio", "hogar", "casa"),
        "entretenimiento" to listOf("entretenimiento", "ocio", "diversion"),
        "salud" to listOf("salud", "farmacia", "medic"),
        "educacion" to listOf("educacion", "estudio", "universidad"),
        "hogar" to listOf("hogar", "casa"),
        "ropa y accesorios" to listOf("ropa", "vestuario", "compras"),
        "otros" to listOf("otro", "varios"),
    )

    fun merge(
        local: ReceiptDraft,
        server: ServerReceipt,
        categories: List<MovementCategory>,
        today: LocalDate,
    ): ReceiptDraft = local.copy(
        merchant = local.merchant.ifBlank { server.merchant },
        amount = server.amount?.takeIf { it > 0.0 } ?: local.amount,
        date = server.date?.takeIf { !it.isAfter(today) } ?: local.date,
        categoryId = server.category?.let { matchCategory(it, categories) } ?: local.categoryId,
        reason = server.reason.ifBlank { local.reason },
    )

    /** La categoría del usuario que corresponde a la del servidor (o `null` si ninguna). */
    fun matchCategory(serverCategory: String, categories: List<MovementCategory>): String? {
        val key = normalize(serverCategory)
        categories.firstOrNull { normalize(it.name) == key }?.let { return it.id }
        val words = synonyms[key] ?: listOf(key)
        for (word in words) {
            categories.firstOrNull { normalize(it.name).contains(word) }?.let { return it.id }
        }
        return null
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("\\s+"), " ")
}
