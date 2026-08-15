package com.biglexj.gallerydl.platform

import com.biglexj.gallerydl.features.DownloadFeature.DownloadHistoryItem
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object InterceptionServer {
    private var server: HttpServer? = null
    private var urlListener: ((String) -> Unit)? = null
    private var cookiesListener: ((String) -> Unit)? = null
    private var imagesListener: ((List<String>, String, String) -> Unit)? = null
    private var historyListener: ((DownloadHistoryItem) -> Unit)? = null

    // HTTP client reutilizable para descargas directas de imagen
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    fun registerUrlListener(listener: (String) -> Unit) {
        urlListener = listener
    }

    fun registerCookiesListener(listener: (String) -> Unit) {
        cookiesListener = listener
    }

    fun registerImagesListener(listener: (List<String>, String, String) -> Unit) {
        imagesListener = listener
    }

    fun registerHistoryListener(listener: (DownloadHistoryItem) -> Unit) {
        historyListener = listener
    }

    fun start(appSettings: AppSettings) {
        if (server != null) return
        Thread {
            try {
                server = HttpServer.create(InetSocketAddress("127.0.0.1", 18274), 0).apply {
                    createContext("/download") { exchange ->
                        if (exchange.requestMethod == "POST") {
                            try {
                                val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).readText()
                                val urlPattern = """\"url\"\s*:\s*\"([^\"]+)\"""".toRegex()
                                val match = urlPattern.find(body)
                                val url = match?.groupValues?.get(1)
                                if (url != null) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        urlListener?.invoke(url)
                                    }
                                    val response = "{\"status\":\"queued\"}"
                                    exchange.responseHeaders.set("Content-Type", "application/json")
                                    exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
                                    exchange.sendResponseHeaders(200, response.length.toLong())
                                    exchange.responseBody.use { it.write(response.toByteArray()) }
                                } else {
                                    exchange.sendResponseHeaders(400, 0)
                                }
                            } catch (e: Exception) {
                                exchange.sendResponseHeaders(500, 0)
                            }
                        } else if (exchange.requestMethod == "OPTIONS") {
                            exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
                            exchange.responseHeaders.set("Access-Control-Allow-Methods", "POST, OPTIONS")
                            exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type")
                            exchange.sendResponseHeaders(204, -1)
                        } else {
                            exchange.sendResponseHeaders(405, 0)
                        }
                    }

                    createContext("/cookies") { exchange ->
                        if (exchange.requestMethod == "POST") {
                            try {
                                val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).readText()
                                val netscapeCookies = convertJsonCookiesToNetscape(body)
                                if (netscapeCookies.isNotBlank()) {
                                    val cookiesDir = File("D:\\Documentos\\cookies")
                                    if (!cookiesDir.exists()) {
                                        cookiesDir.mkdirs()
                                    }
                                    val cookiesFile = File(cookiesDir, "synced_cookies.txt")
                                    cookiesFile.writeText(netscapeCookies, Charsets.UTF_8)
                                    
                                    val absolutePath = cookiesFile.absolutePath
                                    appSettings.defaultBrowserCookies = absolutePath
                                    
                                    CoroutineScope(Dispatchers.Main).launch {
                                        cookiesListener?.invoke(absolutePath)
                                    }

                                    val response = "{\"status\":\"synced\",\"path\":\"${absolutePath.replace("\\", "\\\\")}\"}"
                                    exchange.responseHeaders.set("Content-Type", "application/json")
                                    exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
                                    exchange.sendResponseHeaders(200, response.length.toLong())
                                    exchange.responseBody.use { it.write(response.toByteArray()) }
                                } else {
                                    exchange.sendResponseHeaders(400, 0)
                                }
                            } catch (e: Exception) {
                                exchange.sendResponseHeaders(500, 0)
                            }
                        } else if (exchange.requestMethod == "OPTIONS") {
                            exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
                            exchange.responseHeaders.set("Access-Control-Allow-Methods", "POST, OPTIONS")
                            exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type")
                            exchange.sendResponseHeaders(204, -1)
                        } else {
                            exchange.sendResponseHeaders(405, 0)
                        }
                    }
                    
                    // ── /images: descarga directa de URLs interceptadas desde el navegador ──
                    createContext("/images") { exchange ->
                        val corsHeaders = mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Access-Control-Allow-Methods" to "POST, OPTIONS",
                            "Access-Control-Allow-Headers" to "Content-Type"
                        )
                        corsHeaders.forEach { (k, v) -> exchange.responseHeaders.set(k, v) }

                        if (exchange.requestMethod == "OPTIONS") {
                            exchange.sendResponseHeaders(204, -1)
                            return@createContext
                        }

                        if (exchange.requestMethod != "POST") {
                            exchange.sendResponseHeaders(405, 0)
                            return@createContext
                        }

                        try {
                            val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).readText()

                            // Extraer campos JSON manualmente (sin dependencia extra)
                            val urlsMatch = "\"urls\"\\s*:\\s*\\[([^\\]]+)\\]".toRegex().find(body)
                            val pageTitleMatch = "\"pageTitle\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(body)
                            val pageUrlMatch = "\"pageUrl\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(body)

                            val urlsRaw = urlsMatch?.groupValues?.get(1) ?: ""
                            val pageTitle = pageTitleMatch?.groupValues?.get(1) ?: "Unnamed"
                            val pageUrlStr = pageUrlMatch?.groupValues?.get(1) ?: ""

                            // Parsear el array de URLs (cada una entre comillas)
                            val urls = "\"([^\"]+)\"".toRegex()
                                .findAll(urlsRaw)
                                .map { it.groupValues[1] }
                                .filter { it.startsWith("http") }
                                .toList()

                            if (urls.isEmpty()) {
                                val err = "{\"error\":\"No URLs provided\"}"
                                exchange.responseHeaders.set("Content-Type", "application/json")
                                exchange.sendResponseHeaders(400, err.length.toLong())
                                exchange.responseBody.use { it.write(err.toByteArray()) }
                                return@createContext
                            }

                            // Lanzar descarga directa en coroutine de IO
                            CoroutineScope(Dispatchers.IO).launch {
                                downloadImagesDirectly(urls, pageTitle, pageUrlStr, appSettings)
                            }

                            // También notificar a la UI para logging visual
                            CoroutineScope(Dispatchers.Main).launch {
                                imagesListener?.invoke(urls, pageTitle, pageUrlStr)
                            }

                            val response = "{\"status\":\"queued\",\"queued\":${urls.size}}"
                            exchange.responseHeaders.set("Content-Type", "application/json")
                            exchange.sendResponseHeaders(200, response.length.toLong())
                            exchange.responseBody.use { it.write(response.toByteArray()) }

                        } catch (e: Exception) {
                            println("[Server] Error en /images: ${e.localizedMessage}")
                            exchange.sendResponseHeaders(500, 0)
                        }
                    }

                    executor = null
                    start()
                }
                println("[Server] Servidor HTTP local de intercepción iniciado en el puerto 18274")
            } catch (e: Exception) {
                println("[Server Error] Fallo al iniciar el servidor local: ${e.localizedMessage}")
            }
        }.start()
    }

    fun stop() {
        server?.stop(0)
        server = null
    }

    private fun convertJsonCookiesToNetscape(json: String): String {
        val sb = StringBuilder()
        sb.append("# Netscape HTTP Cookie File\n")
        sb.append("# This file was generated by Gallery-DL GUI Connector\n\n")

        val cookieRegex = """\{([^}]+)\}""".toRegex()
        val nameRegex = """\"name\"\s*:\s*\"([^\"]+)\"""".toRegex()
        val valueRegex = """\"value\"\s*:\s*\"([^\"]+)\"""".toRegex()
        val domainRegex = """\"domain\"\s*:\s*\"([^\"]+)\"""".toRegex()
        val pathRegex = """\"path\"\s*:\s*\"([^\"]+)\"""".toRegex()
        val secureRegex = """\"secure\"\s*:\s*(true|false)""".toRegex()
        val expirationRegex = """\"expirationDate\"\s*:\s*([0-9.]+)""".toRegex()

        var count = 0
        for (match in cookieRegex.findAll(json)) {
            val content = match.value
            val name = nameRegex.find(content)?.groupValues?.get(1) ?: continue
            val value = valueRegex.find(content)?.groupValues?.get(1) ?: continue
            val domain = domainRegex.find(content)?.groupValues?.get(1) ?: continue
            val path = pathRegex.find(content)?.groupValues?.get(1) ?: "/"
            val secure = secureRegex.find(content)?.groupValues?.get(1)?.toBoolean() ?: true
            val expiration = expirationRegex.find(content)?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: (System.currentTimeMillis() / 1000 + 31536000)

            val domainOnly = if (domain.startsWith(".")) domain else ".$domain"
            val includeSubdomains = "TRUE"
            val secureString = if (secure) "TRUE" else "FALSE"

            sb.append("$domainOnly\t$includeSubdomains\t$path\t$secureString\t$expiration\t$name\t$value\n")
            count++
        }
        println("[Server] Convertidas $count cookies de JSON a formato Netscape.")
        return sb.toString()
    }

    /**
     * Descarga directamente una lista de URLs de imagen pre-autenticadas (obtenidas
     * del DOM del navegador). Evita el proceso de re-autenticación de gallery-dl
     * para contenido de edad restringida.
     *
     * Las imágenes se guardan en:
     *   [lastDestination] / [pageTitle sanitizado] / imagen_001.ext
     *
     * Si las cookies sincronizadas existen, se inyectan en el header Cookie de
     * cada petición para mayor compatibilidad con CDNs protegidos.
     */
    private fun downloadImagesDirectly(
        urls: List<String>,
        pageTitle: String,
        pageUrl: String,
        appSettings: AppSettings,
    ) {
        val base = appSettings.lastDestination
            ?: System.getProperty("user.home") + "/Downloads"

        // Sanitizar el título para usarlo como nombre de carpeta
        val safeTitle = pageTitle
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(120)
            .ifBlank { "Gallery_Images" }

        val destDir = File(base, safeTitle)
        if (!destDir.exists()) destDir.mkdirs()

        // Leer cookies sincronizadas para inyectarlas en los headers
        val cookiesHeader = buildCookieHeader(appSettings)

        val downloadedFiles = mutableListOf<String>()
        var failed = 0

        urls.forEachIndexed { index, rawUrl ->
            try {
                val url = rawUrl.trim()
                if (url.isBlank()) return@forEachIndexed

                // Determinar extensión desde la URL
                val extension = url.substringBefore("?")
                    .substringAfterLast(".")
                    .lowercase()
                    .let { ext -> if (ext.length in 2..5 && ext.all { it.isLetter() }) ext else "jpg" }

                val fileName = "image_%04d.%s".format(index + 1, extension)
                val outFile = File(destDir, fileName)

                // Si ya existe y no está vacío, agregarlo a la lista de archivos descargados
                if (outFile.exists() && outFile.length() > 0) {
                    downloadedFiles.add(outFile.absolutePath)
                    return@forEachIndexed
                }

                val requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Referer", "https://www.deviantart.com/")
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")

                if (cookiesHeader.isNotBlank()) {
                    requestBuilder.header("Cookie", cookiesHeader)
                }

                val request = requestBuilder.build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

                if (response.statusCode() in 200..299) {
                    response.body().use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    downloadedFiles.add(outFile.absolutePath)
                    println("[Server] ✅ Imagen ${index + 1}/${urls.size}: $fileName")
                } else {
                    failed++
                    println("[Server] ⚠️ HTTP ${response.statusCode()} para imagen ${index + 1}: $url")
                }

            } catch (e: Exception) {
                failed++
                println("[Server] ❌ Error descargando imagen ${index + 1}: ${e.localizedMessage}")
            }
        }

        println("[Server] Descarga directa completada: ${downloadedFiles.size} OK, $failed errores → ${destDir.absolutePath}")

        // ─── Registrar en el Historial de Descargas ──────────────────────────────
        if (downloadedFiles.isNotEmpty()) {
            val historyItem = DownloadHistoryItem(
                id = "hist_" + System.currentTimeMillis(),
                url = pageUrl.ifBlank { urls.firstOrNull() ?: "" },
                destination = base,
                timestamp = System.currentTimeMillis(),
                status = "Exitoso",
                directoryStructure = "Solo Carpeta de Colección",
                downloadedFiles = downloadedFiles,
                outputDirectory = destDir.absolutePath,
                collectionTitle = safeTitle
            )

            // Persistir en AppSettings
            val existingHistory = appSettings.loadHistory().toMutableList()
            existingHistory.add(0, historyItem)
            appSettings.saveHistory(existingHistory)

            // Notificar a la UI en tiempo real
            CoroutineScope(Dispatchers.Main).launch {
                historyListener?.invoke(historyItem)
            }
        }
    }

    /**
     * Construye un header Cookie desde el archivo Netscape sincronizado
     * para inyectarlo en las peticiones HTTP directas.
     */
    private fun buildCookieHeader(appSettings: AppSettings): String {
        val cookiesPath = appSettings.defaultBrowserCookies
        val cookiesFile = if (!cookiesPath.isNullOrBlank()) File(cookiesPath) else null

        if (cookiesFile == null || !cookiesFile.exists()) return ""

        return cookiesFile.readLines()
            .filter { !it.startsWith("#") && it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size >= 7) "${parts[5]}=${parts[6]}" else null
            }
            .joinToString("; ")
    }
}
