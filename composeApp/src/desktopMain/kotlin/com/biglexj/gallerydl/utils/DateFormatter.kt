package com.biglexj.gallerydl.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(date)
}

actual fun getCurrentTimeMillis(): Long {
    return System.currentTimeMillis()
}
