package com.pocketpal.app.ui.movements

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Flujo 4 · Las dos acciones del detalle que usan Android:
 *
 * - **Compartir**: abre el menú del sistema (WhatsApp, correo, Drive…) con el texto del
 *   comprobante. No hace falta ningún permiso.
 * - **Descargar**: dibuja el comprobante en un PDF con [PdfDocument] (viene con Android,
 *   sin librerías) y lo guarda en Descargas/PocketPal. En Android 10+ se usa MediaStore,
 *   que no pide permiso de almacenamiento; en Android 8-9 se guarda en la carpeta de
 *   descargas propia de la app.
 */
object MovementReceipt {

    fun share(context: Context, content: ReceiptContent, chooserTitle: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, content.heading)
            putExtra(Intent.EXTRA_TEXT, content.shareText())
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }

    /** Crea y guarda el PDF fuera del hilo principal. Devuelve `true` si quedó guardado. */
    suspend fun savePdf(context: Context, content: ReceiptContent): Boolean = withContext(Dispatchers.IO) {
        val document = render(content)
        try {
            openDownloadStream(context, content.fileName())?.use { out -> document.writeTo(out) } != null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        } finally {
            document.close()
        }
    }

    /** Una página A4 (595 x 842 puntos): encabezado coral, título, monto y los datos. */
    private fun render(content: ReceiptContent): PdfDocument {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas
        val heading = Paint().apply { color = CORAL; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val title = Paint().apply { color = INK; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val body = Paint().apply { color = INK; textSize = 12f; isAntiAlias = true }
        val muted = Paint().apply { color = MUTED; textSize = 10f; isAntiAlias = true }

        var y = 72f
        canvas.drawText(content.heading, MARGIN, y, heading)
        y += 16f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, muted)
        y += 32f
        content.pdfLines().forEachIndexed { index, line ->
            canvas.drawText(line, MARGIN, y, if (index < 2) title else body)
            y += if (index < 2) 24f else 20f
        }
        canvas.drawText(content.footer, MARGIN, PAGE_HEIGHT - MARGIN, muted)
        document.finishPage(page)
        return document
    }

    private fun openDownloadStream(context: Context, fileName: String): OutputStream? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PocketPal")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            FileOutputStream(File(dir, fileName))
        }

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val CORAL = 0xFFFF6B6B.toInt()
    private const val INK = 0xFF1F2A44.toInt()
    private const val MUTED = 0xFF6B7690.toInt()
}
