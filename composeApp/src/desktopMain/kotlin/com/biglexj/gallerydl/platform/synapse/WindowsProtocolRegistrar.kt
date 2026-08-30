package com.biglexj.gallerydl.platform.synapse

import java.io.File

/**
 * Registrador nativo del esquema de protocolo `gallerydl://` en el Registro de Windows (HKCU).
 * Permite que enlaces `gallerydl://download?url=...` en el navegador, Discord, Lienzo Gallery o web
 * invoquen automáticamente Gallery-DL GUI.
 */
object WindowsProtocolRegistrar {

    fun registerProtocolIfWindows() {
        val os = System.getProperty("os.name", "").lowercase()
        if (!os.contains("win")) return

        val appPath = System.getProperty("jpackage.app-path")
            ?: ProcessHandle.current().info().command().orElse(null)
            ?: return

        val exeFile = File(appPath)
        if (!exeFile.exists() || !exeFile.name.endsWith(".exe", ignoreCase = true)) return

        // Evitar registrar si se está ejecutando desde java.exe / javaw.exe directo
        if (exeFile.name.contains("java", ignoreCase = true)) return

        val exePath = exeFile.canonicalPath
        val escapedExe = "\\\"$exePath\\\" \\\"%1\\\""

        Thread {
            runCatching {
                val regCommands = listOf(
                    "reg add \"HKCU\\Software\\Classes\\gallerydl\" /ve /d \"URL:Gallery-DL Protocol\" /f",
                    "reg add \"HKCU\\Software\\Classes\\gallerydl\" /v \"URL Protocol\" /d \"\" /f",
                    "reg add \"HKCU\\Software\\Classes\\gallerydl\\DefaultIcon\" /ve /d \"\\\"$exePath\\\",0\" /f",
                    "reg add \"HKCU\\Software\\Classes\\gallerydl\\shell\\open\\command\" /ve /d \"$escapedExe\" /f"
                )

                for (cmd in regCommands) {
                    val process = ProcessBuilder("cmd.exe", "/c", cmd)
                        .redirectErrorStream(true)
                        .start()
                    process.waitFor()
                }
            }
        }.start()
    }
}
