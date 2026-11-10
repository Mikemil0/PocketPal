package com.pocketpal.app.ui.dai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Prepara la foto de la factura para el servidor de Dai: la endereza y la reduce a 1280 px
 * de lado (sube y se lee más rápido).
 * Se llama fuera del hilo principal.
 */
object ReceiptPhoto {
    /** 1280 px: los números de la factura se siguen leyendo y sube y se lee más rápido. */
    private const val MAX_SIDE = 1280

    /** [jpeg] = los bytes que entrega CameraX; [rotationDegrees] = lo que hay que girarla. */
    fun fromJpeg(jpeg: ByteArray, rotationDegrees: Int): ByteArray? {
        return try {
            val bitmap = decodeScaled(jpeg) ?: return null
            compress(rotate(bitmap, rotationDegrees))
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /** Una foto de la galería: el giro viene en sus datos EXIF. */
    fun fromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val rotation = try {
                ExifInterface(ByteArrayInputStream(bytes)).rotationDegrees
            } catch (e: Exception) {
                0
            }
            fromJpeg(bytes, rotation)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val scale = MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
        return if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun compress(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.toByteArray()
    }
}
