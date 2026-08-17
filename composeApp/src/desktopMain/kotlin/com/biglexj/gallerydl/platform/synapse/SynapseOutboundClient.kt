package com.biglexj.gallerydl.platform.synapse

import java.awt.Desktop
import java.io.File
import java.net.InetAddress
import java.net.Socket
import java.net.URLEncoder

/**
 * Cliente Emisor de Acciones Synapse (Aurora Synapse Outbound Client).
 * Permite a Gallery-DL-GUI invocar a Prisma u otras aplicaciones del ecosistema.
 */
object SynapseOutboundClient {

    // Puertos oficiales de Prisma Synapse en el ecosistema (Servidor HTTP/IPC 49288 y puerto de respaldo 49280)
    private val PRISMA_PORTS = listOf(49288, 49280)

    /**
     * Abre y reproduce/visualiza el medio o directorio descargado en Prisma.
     * 1. Intenta contactar la instancia activa de Prisma vía IPC localhost en puertos 49288 / 49280.
     *    Prisma procesa directamente peticiones URI: `prisma://open?path=...&autoplay=true\n`
     * 2. Como respaldo directo si Prisma no está presente, abre el archivo con el reproductor/visor
     *    predeterminado configurado en el sistema operativo (Windows, Linux, macOS) SIN abrir el navegador web.
     */
    fun openMediaInPrisma(
        targetPath: String,
        mediaCategory: String = "visual",
        autoPlay: Boolean = true
    ): Boolean {
        val file = File(targetPath)
        if (!file.exists()) return false
        val canonicalPath = file.canonicalPath
        val encodedPath = URLEncoder.encode(canonicalPath, "UTF-8").replace("+", "%20")
        val prismaUriString = "prisma://open?path=$encodedPath&autoplay=$autoPlay\n"

        // 1. Intentar contactar la instancia viva de Prisma por socket IPC directo
        for (port in PRISMA_PORTS) {
            val success = runCatching {
                Socket(InetAddress.getByName("127.0.0.1"), port).use { socket ->
                    socket.soTimeout = 1500
                    val writer = socket.getOutputStream().bufferedWriter(Charsets.UTF_8)
                    writer.write(prismaUriString)
                    writer.flush()

                    // Leer confirmación de Prisma (responde "OK\r\n")
                    val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                    val response = reader.readLine()
                    response != null && (response.contains("OK", ignoreCase = true) || response.contains("200") || response.contains("success", ignoreCase = true))
                }
            }.getOrDefault(false)

            if (success) return true
        }

        // 2. Fallback Infalible: Abrir directamente con el visor/reproductor nativo del SO (CERO NAVEGADOR)
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
