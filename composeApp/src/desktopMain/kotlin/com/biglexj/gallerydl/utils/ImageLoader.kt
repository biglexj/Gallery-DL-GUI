package com.biglexj.gallerydl.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.io.File

actual fun loadLocalImage(path: String): ImageBitmap? {
    return try {
        val file = File(path)
        if (file.exists() && file.isFile) {
            file.inputStream().use { loadImageBitmap(it) }
        } else null
    } catch (e: Exception) {
        null
    }
}
