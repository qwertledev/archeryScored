package com.archeryscored.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/** App-private photo storage under `filesDir/photos/` — no runtime storage permission needed, never visible outside the app. */
class PhotoStorage(private val context: Context) {

    fun fileForEnd(sessionId: Long, endNumber: Int, timestampMillis: Long): File {
        val dir = File(context.filesDir, "photos/session_$sessionId")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "end_${endNumber}_$timestampMillis.jpg")
    }

    fun resolve(relativePath: String): File = File(context.filesDir, relativePath)

    fun relativePath(file: File): String = file.relativeTo(context.filesDir).path

    fun deletePhotosForSession(sessionId: Long) {
        File(context.filesDir, "photos/session_$sessionId").deleteRecursively()
    }

    /**
     * Copies an archer-picked gallery image into [dest], decoding and re-encoding it as an upright
     * JPEG. Bitmap/OpenCV don't honor EXIF orientation automatically, and calibration/scoring assume
     * pixel coordinates already match what's displayed, so this normalizes rotation up front rather
     * than have every downstream consumer worry about it.
     *
     * Gallery photos can be far higher-resolution than anything the camera path produces (a modern
     * phone's main camera app can easily hit 48MP+, i.e. a 190MB+ ARGB_8888 bitmap decoded naively) -
     * decoding at full size was crashing the app outright (an OS-level low-memory kill, not something
     * a try/catch can intercept), so this bounds the decode with inSampleSize the standard way.
     */
    fun importUploadedPhoto(sourceUri: Uri, dest: File) {
        val rotationDegrees = openStream(sourceUri) { readExifRotationDegrees(it) }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(sourceUri) { BitmapFactory.decodeStream(it, null, bounds) }
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Could not read image bounds for $sourceUri" }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_IMPORTED_DIMENSION)
        }
        val decoded = openStream(sourceUri) { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: error("Could not decode image at $sourceUri")

        val upright = if (rotationDegrees == 0) {
            decoded
        } else {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it !== decoded) decoded.recycle()
            }
        }

        FileOutputStream(dest).use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        upright.recycle()
    }

    private fun <T> openStream(uri: Uri, block: (InputStream) -> T): T {
        val stream = context.contentResolver.openInputStream(uri) ?: error("Could not open $uri")
        return stream.use(block)
    }

    private fun readExifRotationDegrees(stream: InputStream): Int =
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    /** Smallest power-of-two sample size that brings the longest side under [maxDimension]. */
    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        val longestSide = maxOf(width, height)
        while (longestSide / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    companion object {
        private const val MAX_IMPORTED_DIMENSION = 3000
    }
}
