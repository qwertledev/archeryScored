package com.archeryscored.app

import android.app.Application
import com.archeryscored.cv.OpenCvBootstrap
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ArcheryScoredApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Best-effort: if this fails, DetectionPipeline.run() short-circuits to null and every
        // end just falls back to manual calibration - auto-detection is never a hard dependency.
        OpenCvBootstrap.ensureLoaded()
    }
}
