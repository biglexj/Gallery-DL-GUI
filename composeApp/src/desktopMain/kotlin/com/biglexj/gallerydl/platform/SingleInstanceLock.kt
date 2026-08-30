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
        // En producción (.exe distribuido / jpackage), System.getProperty("jpackage.app-path") está presente
        if (System.getProperty("jpackage.app-path") != null) return false

        // Si el ejecutable nativo no es java/javaw, es la aplicación empaquetada en producción
        val command = ProcessHandle.current().info().command().orElse("")
        if (command.endsWith(".exe", ignoreCase = true) &&
            !command.endsWith("java.exe", ignoreCase = true) &&
            !command.endsWith("javaw.exe", ignoreCase = true)
        ) {
            return false
        }

        return System.getProperty("gallerydlgui.dev") == "true" ||
                System.getProperty("idea.active") != null ||
                System.getProperty("sun.java.command")?.let {
                    it.contains("Gradle", ignoreCase = true) ||
                    it.contains("idea", ignoreCase = true)
                } == true
    }

    fun acquireOrTransfer(args: Array<String> = emptyArray()): Boolean {
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
            // El puerto ya está en uso: otra instancia está activa en producción -> transferir orden
            val payload = extractPayload(args)
            notifyExistingInstance(payload)
            false
        }
    }

    // Compatibilidad retroactiva
    fun acquireOrBringToFront(): Boolean = acquireOrTransfer(emptyArray())

    private fun extractPayload(args: Array<String>): String {
        val targetArg = args.firstOrNull { arg ->
            arg.startsWith("http://", ignoreCase = true) ||
            arg.startsWith("https://", ignoreCase = true) ||
            arg.startsWith("gallerydl://", ignoreCase = true) ||
            arg.startsWith("aurora-synapse://", ignoreCase = true) ||
            (arg.startsWith("{") && arg.endsWith("}"))
        }
        return targetArg ?: "FOCUS"
    }

    private fun notifyExistingInstance(payload: String) {
        runCatching {
            Socket(InetAddress.getByName("127.0.0.1"), lockPort).use { socket ->
                socket.soTimeout = 2500
                val writer = socket.getOutputStream().bufferedWriter()
                writer.write(payload.trim() + "\n")
                writer.flush()
            }
        }
    }

    fun listenForRequests(onRequestReceived: (com.biglexj.gallerydl.platform.synapse.SynapseAction) -> Unit) {
        val ss = serverSocket ?: return
        thread(isDaemon = true, name = "GalleryDLSingleInstanceLockListener") {
            while (!ss.isClosed) {
                runCatching {
                    val client = ss.accept()
                    client.use { s ->
                        s.soTimeout = 2500
                        val line = s.getInputStream().bufferedReader().readLine()
                        if (!line.isNullOrBlank()) {
                            val action = com.biglexj.gallerydl.platform.synapse.SynapseUriParser.parse(line)
                                ?: com.biglexj.gallerydl.platform.synapse.SynapseAction.Focus
                            SwingUtilities.invokeLater {
                                onRequestReceived(action)
                            }
                        }
                    }
                }
            }
        }
    }

    // Compatibilidad retroactiva
    fun listenForFocusRequests(onFocusRequested: () -> Unit) {
        listenForRequests { action ->
            if (action is com.biglexj.gallerydl.platform.synapse.SynapseAction.Focus) {
                onFocusRequested()
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
