package com.biglexj.gallerydl.platform

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Garantía de Instancia Única (Single Instance Lock) para Gallery-DL-GUI.
 * Estandarizado según el ecosistema de Luna Fetch y Core-Docs.
 * Previene la duplicación de procesos y la proliferación de iconos duplicados en la bandeja en producción.
 * Incluye bypass en modo desarrollo para no interferir con ejecuciones de `./gradlew run` o IDEs.
 */
object SingleInstanceLock {
    private const val LOCK_PORT = 18275
    private var serverSocket: ServerSocket? = null

    fun acquireOrBringToFront(): Boolean {
        val execPath = ProcessHandle.current().info().command().orElse("").lowercase()
        val isWindowsInstalled = (execPath.endsWith("gallerydl-gui.exe") || execPath.endsWith("gallery-dl-gui.exe")) &&
                (execPath.contains("appdata") || execPath.contains("program files"))
        val isLinuxInstalled = execPath.contains("/opt/gallerydl") || execPath.contains("/usr/") || System.getenv("APPIMAGE") != null
        val isInstalled = isWindowsInstalled || isLinuxInstalled

        val isDev = !isInstalled ||
                System.getProperty("gallerydlgui.dev") == "true" ||
                System.getProperty("idea.active") != null ||
                System.getProperty("sun.java.command")?.let {
                    it.contains("MainKt", ignoreCase = true) ||
                    it.contains("Gradle", ignoreCase = true) ||
                    it.contains("idea", ignoreCase = true)
                } == true

        if (isDev) {
            return true
        }

        return try {
            serverSocket = ServerSocket(LOCK_PORT, 10, InetAddress.getByName("127.0.0.1"))
            true
        } catch (e: Exception) {
            // El bloqueo pertenece a otra instancia activa en producción
            notifyExistingInstance()
            false
        }
    }

    private fun notifyExistingInstance() {
        runCatching {
            Socket(InetAddress.getByName("127.0.0.1"), LOCK_PORT).use { socket ->
                socket.soTimeout = 2000
                val writer = socket.getOutputStream().bufferedWriter()
                writer.write("FOCUS\n")
                writer.flush()
            }
        }
    }

    fun listenForFocusRequests(onFocusRequested: () -> Unit) {
        val ss = serverSocket ?: return
        thread(isDaemon = true, name = "GalleryDLSingleInstanceLockListener") {
            while (!ss.isClosed) {
                runCatching {
                    val client = ss.accept()
                    client.use { s ->
                        s.soTimeout = 2000
                        val line = s.getInputStream().bufferedReader().readLine()
                        if (line == "FOCUS") {
                            onFocusRequested()
                        }
                    }
                }
            }
        }
    }

    fun release() {
        runCatching {
            serverSocket?.close()
            serverSocket = null
        }
    }
}
