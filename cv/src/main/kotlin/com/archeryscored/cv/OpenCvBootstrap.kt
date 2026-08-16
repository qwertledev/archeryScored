package com.archeryscored.cv

import org.opencv.android.OpenCVLoader

/** Call once (e.g. from Application.onCreate) before any detector runs. */
object OpenCvBootstrap {
    @Volatile
    private var loaded = false

    fun ensureLoaded(): Boolean {
        if (!loaded) {
            loaded = OpenCVLoader.initLocal()
        }
        return loaded
    }
}
