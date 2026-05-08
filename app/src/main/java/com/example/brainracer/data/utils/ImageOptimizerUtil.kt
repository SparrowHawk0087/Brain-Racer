package com.example.brainracer.data.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageOptimizerUtil {

    // Максимальные размеры сторон для обложки и вопросов
    private const val MAX_COVER_PX   = 1024
    private const val MAX_QUESTION_PX = 800
    private const val MAX_FILE_KB    = 300          // целевой потолок размера

    data class OptimizedImage(
        val bytes: ByteArray,
        val mimeType: String,
        val widthPx: Int,
        val heightPx: Int,
        val sizeKb: Int
    )

    /**
     * Основная точка входа.
     * Определяет формат, для JPEG/PNG — сжимает через Bitmap,
     * для GIF — просто уменьшает, если файл велик.
     */
    suspend fun optimize(
        context: Context,
        uri: Uri,
        isCover: Boolean = false
    ): OptimizedImage = withContext(Dispatchers.IO) {
        val maxPx    = if (isCover) MAX_COVER_PX else MAX_QUESTION_PX
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

        val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Не удалось открыть изображение")

        return@withContext if (mimeType == "image/gif") {
            optimizeGif(raw)
        } else {
            optimizeBitmap(context, uri, raw, maxPx, mimeType)
        }
    }

    // Bitmap (JPEG / PNG / WEBP)

    private fun optimizeBitmap(
        context: Context,
        uri: Uri,
        raw: ByteArray,
        maxPx: Int,
        originalMime: String
    ): OptimizedImage {
        // Читаем с inSampleSize, чтобы не грузить RAM гигантскими файлами
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, options)

        val sample = calcSample(options.outWidth, options.outHeight, maxPx)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = BitmapFactory.decodeByteArray(raw, 0, raw.size, decodeOpts)
            ?: throw IllegalArgumentException("Не удалось декодировать изображение")

        // Применяем EXIF-ориентацию
        bmp = fixOrientation(context, uri, bmp)

        // Масштабируем, если всё ещё больше maxPx
        bmp = scaleTo(bmp, maxPx)

        // Кодируем — JPEG для фото, PNG только если исходник был PNG
        val format  = if (originalMime == "image/png") Bitmap.CompressFormat.PNG
        else Bitmap.CompressFormat.JPEG
        val outMime = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"

        // Адаптивно снижаем качество, пока файл не уложится в MAX_FILE_KB
        val out     = ByteArrayOutputStream()
        var quality = 88
        do {
            out.reset()
            bmp.compress(format, quality, out)
            quality -= 8
        } while (out.size() > MAX_FILE_KB * 1024 && quality > 30)

        val bytes = out.toByteArray()
        Log.d("ImageOptimizer", "Compressed: ${raw.size / 1024}KB → ${bytes.size / 1024}KB  ${bmp.width}×${bmp.height}px")

        return OptimizedImage(
            bytes   = bytes,
            mimeType = outMime,
            widthPx  = bmp.width,
            heightPx = bmp.height,
            sizeKb   = bytes.size / 1024
        )
    }

    // GIF
    // Для GIF мы не перекодируем анимацию — просто возвращаем как есть,
    // предупреждая пользователя если файл слишком велик.
    private fun optimizeGif(raw: ByteArray): OptimizedImage {
        val sizeKb = raw.size / 1024
        Log.d("ImageOptimizer", "GIF size: ${sizeKb}KB (no recompression)")
        return OptimizedImage(
            bytes    = raw,
            mimeType = "image/gif",
            widthPx  = 0,
            heightPx = 0,
            sizeKb   = sizeKb
        )
    }

    // Вспомогательные

    private fun calcSample(w: Int, h: Int, maxPx: Int): Int {
        var sample = 1
        val larger = maxOf(w, h)
        while (larger / sample > maxPx * 2) sample *= 2
        return sample
    }

    private fun scaleTo(bmp: Bitmap, maxPx: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        if (w <= maxPx && h <= maxPx) return bmp
        val scale = maxPx.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(bmp, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    private fun fixOrientation(context: Context, uri: Uri, bmp: Bitmap): Bitmap {
        return try {
            val stream: InputStream = context.contentResolver.openInputStream(uri) ?: return bmp
            val exif = ExifInterface(stream)
            stream.close()
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90  -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                else -> return bmp
            }
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        } catch (e: Exception) {
            bmp
        }
    }
}