package com.pocketpal.app.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * RNF-04: la foto se redimensiona y comprime **antes** de subirla (máx. 512 px de
 * lado, JPEG ~80 %). Vive en la capa de datos porque es solo procesamiento de bytes:
 * la pantalla pasa un `Uri` y la ViewModel recibe un `ByteArray` listo para subir.
 *
 * La rotación EXIF se corrige con `androidx.exifinterface` (la clase del framework
 * `android.media.ExifInterface` la desaconseja lint por bugs de seguridad antiguos).
 */
object AvatarImageEncoder {

    private const val MAX_SIDE = 512
    private const val JPEG_QUALITY = 80

    suspend fun encode(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        // Muestrea primero a ~2x el destino y luego escala exacto (ahorra memoria).
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@withContext null

        val rotated = applyExifRotation(resolver, uri, decoded)
        val scaled = scaleToFit(rotated, MAX_SIDE)

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)

        if (scaled !== rotated) scaled.recycle()
        if (rotated !== decoded) rotated.recycle()
        decoded.recycle()
        out.toByteArray()
    }

    private fun applyExifRotation(resolver: android.content.ContentResolver, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = resolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: return bitmap

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToFit(bitmap: Bitmap, maxSide: Int): Bitmap {
        val scale = maxSide.toFloat() / max(bitmap.width, bitmap.height)
        if (scale >= 1f) return bitmap
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
