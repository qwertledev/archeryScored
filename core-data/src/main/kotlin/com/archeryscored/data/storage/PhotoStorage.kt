package com.archeryscored.data.storage

import android.content.Context
import java.io.File

/** App-private photo storage under `filesDir/photos/` — no runtime storage permission needed, never visible outside the app. */
class PhotoStorage(private val context: Context) {

    fun fileForEnd(sessionId: Long, endNumber: Int, timestampMillis: Long): File {
        val dir = File(context.filesDir, "photos/session_$sessionId")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "end_${endNumber}_$timestampMillis.jpg")
    }

    fun resolve(relativePath: String): File = File(context.filesDir, relativePath)

    fun relativePath(file: File): String = file.relativeTo(context.filesDir).path
}
