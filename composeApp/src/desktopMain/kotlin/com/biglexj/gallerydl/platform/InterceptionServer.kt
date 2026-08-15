package com.biglexj.gallerydl.platform

import com.biglexj.gallerydl.features.DownloadFeature.DownloadHistoryItem
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
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

    private fun handleCors(exchange: HttpExchange): Boolean {
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
        exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization, *")
        exchange.responseHeaders.set("Access-Control-Max-Age", "86400")

        if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
            return true
        }
        return false
    }

    fun start(appSettings: AppSettings) {
        if (server != null) return
        Thread {
            try {
                val s = HttpServer.create(InetSocketAddress("127.0.0.1", 18274), 50)
                s.executor = Executors.newCachedThreadPool()

                // ── /status: Verificación de salud y conectividad de la extensión ──────
                s.createContext("/status") { exchange ->
                    try {
                        if (handleCors(exchange)) return@createContext
                        val response = "{\"status\":\"online\",\"version\":\"0.0.2\"}"
                        val bytes = response.toByteArray(Charsets.UTF_8)
                        exchange.responseHeaders.set("Content-Type", "application/json")
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.use { it.write(bytes) }
                    } catch (e: Exception) {
                        exchange.sendResponseHeaders(500, 0)
                    } finally {
                        exchange.close()
                    }
                }

                // ── /download: Encolar URL de galería completa ─────────────────────────
                s.createContext("/download") { exchange ->
                    try {
                        if (handleCors(exchange)) return@createContext
                        if (!exchange.requestMethod.equals("POST", ignoreCase = true)) {
                            exchange.sendResponseHeaders(405, 0)
                            return@createContext
                        }

                        val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).readText()
                        val urlPattern = """\"url\"\s*:\s*\"([^\"]+)\"""".toRegex()
                        val match = urlPattern.find(body)
                        val url = match?.groupValues?.get(1)

                        if (url != null) {
                            CoroutineScope(Dispatchers.Main).launch {
                                urlListener?.invoke(url)
                            }
                            val response = "{\"status\":\"queued\",\"url\":\"$url\"}"
                            val bytes = response.toByteArray(Charsets.UTF_8)
                            exchange.responseHeaders.set("Content-Type", "application/json")
                            exchange.sendResponseHeaders(200, bytes.size.toLong())
                            exchange.responseBody.use { it.write(bytes) }
                        } else {
                            val err = "{\"error\":\"Missing url parameter\"}"
                            val bytes = err.toByteArray(Charsets.UTF_8)
                            exchange.responseHeaders.set("Content-Type", "application/json")
                            exchange.sendResponseHeaders(400, bytes.size.toLong())
                            exchange.responseBody.use { it.write(bytes) }
                        }
                    } catch (e: Exception) {
                        exchange.sendResponseHeaders(500, 0)
                    } finally {
                        exchange.close()
                    }
                }

                // ── /cookies: Sincronización de cookies Netscape ───────────────────────
                s.createContext("/cookies") { exchange ->
                    try {
                        if (handleCors(exchange)) return@createContext
                        if (!exchange.requestMethod.equals("POST", ignoreCase = true)) {
                            exchange.sendResponseHeaders(405, 0)
                            return@createContext
                        }

                        val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).readText()
                        val netscapeCookies = convertJsonCookiesToNetscape(body)

                        if (netscapeCookies.isNotBlank()) {
                            val cookiesDir = File("D:\\Documentos\\cookies")
                            if (!cookiesDir.exists()) cookiesDir.mkdirs()

                            val cookiesFile = File(cookiesDir, "synced_cookies.txt")
                            cookiesFile.writeText(netscapeCookies, Charsets.UTF_8)

                            val absolutePath = cookiesFile.absolutePath
                            appSettings.defaultBrowserCookies = absolutePath

                            CoroutineScope(Dispatchers.Main).launch {
                                cookiesListener?.invoke(absolutePath)
                            }

                            val response = "{\"status\":\"synced\",\"path\":\"${absolutePath.replace("\\", "\\\\")}\"}"
                            val bytes = response.toByteArray(Charsets.UTF_8)
                            exchange.responseHeaders.set("Content-Type", "application/json")
                            exchange.sendResponseHeaders(200, bytes.size.toLong())
                            exchange.responseBody.use { it.write(bytes) }
                        } else {
                            val err = "{\"error\":\"No cookies parsed\"}"
                            val bytes = err.toByteArray(Charsets.UTF_8)
                            exchange.responseHeaders.set("Content-Type", "application/json")
                            exchange.sendResponseHeaders(400, bytes.size.toLong())
                            exchange.responseBody.use { it.write(bytes) }
                        }
                    } catch (e: Exception) {
                        exchange.sendResponseHeaders(500, 0)
                    } finally {
                        exchange.close()
                    }
                }

                // ── /images: Descarga directa de URLs interceptadas del DOM ───────────
                s.createContext("/images") { exchange ->
                    try {
                        if (handleCors(exchange)) return@createContext
                        if (!exchange.requestMethod.equals("POST", ignoreCase = true)) {
                            exchange.sendResponseHeaders(405, 0)
                            return@createContext
                        }

                        val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).readText()

                        val urlsMatch = "\"urls\"\\s*:\\s*\\[([^\\]]+)\\]".toRegex().find(body)
                        val pageTitleMatch = "\"pageTitle\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(body)
                        val pageUrlMatch = "\"pageUrl\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(body)

                        val urlsRaw = urlsMatch?.groupValues?.get(1) ?: ""
                        val pageTitle = pageTitleMatch?.groupValues?.get(1) ?: "Unnamed"
                        val pageUrlStr = pageUrlMatch?.groupValues?.get(1) ?: ""

                        val urls = "\"([^\"]+)\"".toRegex()
                            .findAll(urlsRaw)
                            .map { it.groupValues[1] }
                            .filter { it.startsWith("http") }
                            .toList()

                        if (urls.isEmpty()) {
                            val err = "{\"error\":\"No URLs provided\"}"
                            val bytes = err.toByteArray(Charsets.UTF_8)
                            exchange.responseHeaders.set("Content-Type", "application/json")
                            exchange.sendResponseHeaders(400, bytes.size.toLong())
                            exchange.responseBody.use { it.write(bytes) }
                            return@createContext
                        }

                        // Lanzar descarga directa en coroutine de IO
                        CoroutineScope(Dispatchers.IO).launch {
                            downloadImagesDirectly(urls, pageTitle, pageUrlStr, appSettings)
                        }

                        // Notificar a la UI
                        CoroutineScope(Dispatchers.Main).launch {
                            imagesListener?.invoke(urls, pageTitle, pageUrlStr)
                        }

                        val response = "{\"status\":\"queued\",\"queued\":${urls.size}}"
                        val bytes = response.toByteArray(Charsets.UTF_8)
                        exchange.responseHeaders.set("Content-Type", "application/json")
                        exchange.sendResponseHeaders(200, bytes.size.toLong())
                        exchange.responseBody.use { it.write(bytes) }

                    } catch (e: Exception) {
                        println("[Server Error] Excepción en /images: ${e.localizedMessage}")
                        exchange.sendResponseHeaders(500, 0)
                    } finally {
                        exchange.close()
                    }
                }

                s.start()
                server = s
                println("[Server] Servidor HTTP local de intercepción iniciado en el puerto 18274")
            } catch (e: Exception) {
                println("[Server Error] Fallo al iniciar el servidor local en 18274: ${e.localizedMessage}")
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
     * Descarga directamente una lista de URLs de imagen pre-autenticadas.
     */
    private fun downloadImagesDirectly(
        urls: List<String>,
        pageTitle: String,
        pageUrl: String,
        appSettings: AppSettings,
    ) {
        val base = appSettings.lastDestination
            ?: System.getProperty("user.home") + "/Downloads"

        val safeTitle = pageTitle
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(120)
            .ifBlank { "Gallery_Images" }

        val destDir = File(base, safeTitle)
        if (!destDir.exists()) destDir.mkdirs()

        val cookiesHeader = buildCookieHeader(appSettings)
        val downloadedFiles = mutableListOf<String>()
        var failed = 0

        urls.forEachIndexed { index, rawUrl ->
            try {
                val url = rawUrl.trim()
                if (url.isBlank()) return@forEachIndexed

                val extension = url.substringBefore("?")
                    .substringAfterLast(".")
                    .lowercase()
                    .let { ext -> if (ext.length in 2..5 && ext.all { it.isLetter() }) ext else "jpg" }

                val fileName = "image_%04d.%s".format(index + 1, extension)
                val outFile = File(destDir, fileName)

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

        // Registrar en el Historial de Descargas
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

            val existingHistory = appSettings.loadHistory().toMutableList()
            existingHistory.add(0, historyItem)
            appSettings.saveHistory(existingHistory)

            CoroutineScope(Dispatchers.Main).launch {
                historyListener?.invoke(historyItem)
            }
        }
    }

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
