package com.biglexj.gallerydl.platform

import java.io.File
import java.util.concurrent.TimeUnit

object Downloader {
    fun getUserEngineDirectory(): File {
        val custom = System.getProperty("gallerydl.user.engine.dir")
        if (!custom.isNullOrBlank()) {
            return File(custom)
        }
        val configRoot = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
            ?: System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
            ?: (System.getProperty("user.home") + "/.config")
        return File(configRoot, "Gallery-DL-GUI/bin")
    }

    fun getExecutableName(): String {
        val osName = System.getProperty("os.name").lowercase()
        val isWindows = osName.contains("win")
        return if (isWindows) "gallery-dl.exe" else "gallery-dl"
    }

    fun getUserEngineExecutable(): File {
        return File(getUserEngineDirectory(), getExecutableName())
    }

    fun getExecutablePath(): String {
        val exeName = getExecutableName()

        val candidates = buildList {
            // 1. Binario actualizado por el usuario en AppData (máxima prioridad)
            add(getUserEngineExecutable())

            // 2. Recursos empaquetados en tiempo de ejecución (jpackage / Inno Setup)
            System.getProperty("compose.application.resources.dir")
                ?.takeIf { it.isNotBlank() }
                ?.let { resourcesDir -> add(File(resourcesDir, "bin/$exeName")) }

            // 3. Rutas de desarrollo y ejecución desde el repositorio.
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

    fun getActiveEngineVersion(): String? {
        return runCatching {
            val exePath = getExecutablePath()
            val process = ProcessBuilder(exePath, "--version")
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(5, TimeUnit.SECONDS)
            if (finished && process.exitValue() == 0) {
                process.inputStream.bufferedReader().readLine()?.trim()
            } else {
                process.destroyForcibly()
                null
            }
        }.getOrNull()
    }
}
