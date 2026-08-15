package com.biglexj.gallerydl.platform

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/**
 * Garantía de Instancia Única (Single Instance Lock) para Gallery-DL-GUI.
 * Estandarizado según el ecosistema de Luna Fetch y Core-Docs.
 * Previene de forma infalible la duplicación de procesos e iconos duplicados en la bandeja del sistema.
 */
object SingleInstanceLock {
    private const val LOCK_PORT = 18275
    private var serverSocket: ServerSocket? = null

    fun acquireOrBringToFront(): Boolean {
        val isExplicitDev = System.getProperty("gallerydlgui.dev") == "true" ||
                System.getProperty("idea.active") != null

        if (isExplicitDev) {
            return true
        }

        return try {
            serverSocket = ServerSocket(LOCK_PORT, 10, InetAddress.getByName("127.0.0.1"))
            true
        } catch (e: Exception) {
            // El puerto ya está en uso: otra instancia está activa
            notifyExistingInstance()
            false
        }
    }

    private fun notifyExistingInstance() {
        runCatching {
            Socket(InetAddress.getByName("127.0.0.1"), LOCK_PORT).use { socket ->
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
