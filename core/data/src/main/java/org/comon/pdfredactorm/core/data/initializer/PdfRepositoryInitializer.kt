package org.comon.pdfredactorm.core.data.initializer

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

object PdfRepositoryInitializer {
    fun initialize(context: Context) {
        PDFBoxResourceLoader.init(context)
    }
}
