package org.comon.pdfredactorm

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.comon.pdfredactorm.core.data.initializer.PdfRepositoryInitializer

@HiltAndroidApp
class PdfRedactorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PdfRepositoryInitializer.initialize(this)
    }
}
