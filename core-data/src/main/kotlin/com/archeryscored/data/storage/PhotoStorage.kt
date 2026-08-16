package com.archeryscored.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

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
     */
    fun importUploadedPhoto(sourceUri: Uri, dest: File) {
        val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: error("Could not open $sourceUri")
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Could not decode image at $sourceUri")

        val rotationDegrees = ByteArrayInputStream(bytes).use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }
        val upright = if (rotationDegrees == 0) {
            bitmap
        } else {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        FileOutputStream(dest).use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
    }
}
