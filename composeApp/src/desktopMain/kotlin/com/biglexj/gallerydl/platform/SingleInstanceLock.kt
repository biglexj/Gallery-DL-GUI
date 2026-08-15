package com.biglexj.gallerydl.platform

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/**
 * Garantía de Instancia Única (Single Instance Lock) para Gallery-DL-GUI.
 * Estandarizado según el ecosistema de Luna Fetch y Core-Docs (desktop_app_standards.md Regla 1).
 *
 * En Producción (.exe / .msi / standalone): Bloquea instancias duplicadas y enfoca la ventana activa.
 * En Desarrollo (./gradlew :composeApp:run / IDE): Bypass garantizado para permitir depuración continua sin cierres.
 */
object SingleInstanceLock {
    private const val LOCK_PORT_PROD = 18275
    private const val LOCK_PORT_DEV = 18285
    private var serverSocket: ServerSocket? = null

    private val lockPort get() = if (isDevMode()) LOCK_PORT_DEV else LOCK_PORT_PROD

    fun isDevMode(): Boolean {
        return System.getProperty("gallerydlgui.dev") == "true" ||
                System.getProperty("idea.active") != null ||
                System.getProperty("sun.java.command")?.let {
                    it.contains("MainKt", ignoreCase = true) ||
                    it.contains("Gradle", ignoreCase = true) ||
                    it.contains("idea", ignoreCase = true)
                } == true
    }

    fun acquireOrBringToFront(): Boolean {
        if (isDevMode()) {
            println("[SingleInstanceLock] Modo desarrollo activo (puerto dev: $lockPort). Bypass de bloqueo de instancia única permitido.")
            runCatching {
                serverSocket = ServerSocket(lockPort, 10, InetAddress.getByName("127.0.0.1"))
            }
            return true
        }

        return try {
            serverSocket = ServerSocket(lockPort, 10, InetAddress.getByName("127.0.0.1"))
            true
        } catch (e: Exception) {
            // El puerto ya está en uso: otra instancia está activa en producción
            notifyExistingInstance()
            false
        }
    }

    private fun notifyExistingInstance() {
        runCatching {
            Socket(InetAddress.getByName("127.0.0.1"), lockPort).use { socket ->
                socket.soTimeout = 2500
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
                            SwingUtilities.invokeLater {
                                onFocusRequested()
                            }
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
