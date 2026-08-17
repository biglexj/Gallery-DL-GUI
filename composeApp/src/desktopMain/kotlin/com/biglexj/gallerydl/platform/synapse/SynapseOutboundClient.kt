package com.biglexj.gallerydl.platform.synapse

import com.biglexj.gallerydl.features.synapse.SynapseEnvelope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.awt.Desktop
import java.io.File
import java.net.InetAddress
import java.net.Socket
import java.net.URI
import java.net.URLEncoder

/**
 * Cliente Emisor de Acciones Synapse (Aurora Synapse Outbound Client).
 * Permite a Gallery-DL-GUI invocar a Prisma u otras aplicaciones del ecosistema.
 */
object SynapseOutboundClient {

    const val PRISMA_PORT = 49280

    /**
     * Abre y reproduce/visualiza el medio o directorio descargado en Prisma.
     * 1. Intenta contactar la instancia activa de Prisma vía IPC localhost:49280.
     * 2. Si no responde por socket, despacha el esquema URI nativo `prisma://open?path=...&autoplay=true`.
     * 3. Como respaldo si Prisma no está presente, abre el archivo o carpeta con el visor/reproductor del sistema.
     */
    fun openMediaInPrisma(
        targetPath: String,
        mediaCategory: String = "visual",
        autoPlay: Boolean = true
    ): Boolean {
        val file = File(targetPath)
        if (!file.exists()) return false
        val canonicalPath = file.canonicalPath

        // 1. Enviar payload estructurado por socket IPC si Prisma ya está corriendo
        val envelope = SynapseEnvelope(
            synapseVersion = "1.0",
            sourceApp = "gallerydl",
            targetApp = "prisma",
            action = "open_media",
            timestampUtc = java.time.Instant.now().toString(),
            payload = buildJsonObject {
                put("file_path", canonicalPath)
                put("media_category", mediaCategory)
                put("auto_play", autoPlay)
            }
        )
        val jsonPayload = SynapseEnvelope.toJson(envelope)

        val ipcSuccess = runCatching {
            Socket(InetAddress.getByName("127.0.0.1"), PRISMA_PORT).use { socket ->
                socket.soTimeout = 1500
                val writer = socket.getOutputStream().bufferedWriter()
                writer.write(jsonPayload + "\n")
                writer.flush()
                true
            }
        }.getOrDefault(false)

        if (ipcSuccess) return true

        // 2. Intentar deep link URI ante el sistema operativo
        val uriSuccess = runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                val encodedPath = URLEncoder.encode(canonicalPath, "UTF-8").replace("+", "%20")
                val uri = URI("prisma://open?path=$encodedPath&autoplay=$autoPlay")
                Desktop.getDesktop().browse(uri)
                true
            } else {
                false
            }
        }.getOrDefault(false)

        if (uriSuccess) return true

        // 3. Respaldo directo: abrir con la aplicación predeterminada del sistema
        return runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file)
                true
            } else {
                val os = System.getProperty("os.name", "").lowercase()
                if (os.contains("win")) {
                    ProcessBuilder("cmd.exe", "/c", "start", "", canonicalPath).start()
                } else if (os.contains("mac")) {
                    ProcessBuilder("open", canonicalPath).start()
                } else {
                    ProcessBuilder("xdg-open", canonicalPath).start()
                }
                true
            }
        }.getOrDefault(false)
    }
}
