package com.biglexj.gallerydl.platform

import java.io.File

object Downloader {
    fun getExecutablePath(): String {
        val osName = System.getProperty("os.name").lowercase()
        val isWindows = osName.contains("win")
        val exeName = if (isWindows) "gallery-dl.exe" else "gallery-dl"

        val candidates = buildList {
            System.getProperty("compose.application.resources.dir")
                ?.takeIf { it.isNotBlank() }
                ?.let { resourcesDir -> add(File(resourcesDir, "bin/$exeName")) }

            // Rutas de desarrollo y ejecución desde el repositorio.
            add(File("bin", exeName))
            add(File("../bin", exeName))
            add(File(System.getProperty("user.dir"), "bin/$exeName"))
            add(File(System.getProperty("user.dir"), "../bin/$exeName"))
        }

        for (candidate in candidates) {
            if (candidate.exists()) {
                return candidate.absolutePath
            }
        }

        return exeName // Fallback to system PATH
    }
}
