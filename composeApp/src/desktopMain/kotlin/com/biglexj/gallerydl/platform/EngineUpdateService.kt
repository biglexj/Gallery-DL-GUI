package com.biglexj.gallerydl.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration

@Serializable
data class CodebergRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<CodebergAsset> = emptyList()
)

@Serializable
data class CodebergAsset(
    val name: String,
    val size: Long = 0L,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)

sealed interface EngineUpdateCheckResult {
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val downloadUrl: String,
        val assetSize: Long
    ) : EngineUpdateCheckResult

    data class UpToDate(val currentVersion: String) : EngineUpdateCheckResult
    data class Error(val message: String) : EngineUpdateCheckResult
}

object EngineUpdateService {
    private const val CODEBERG_API_URL = "https://codeberg.org/api/v1/repos/mikf/gallery-dl/releases/latest"

    private val httpClient: HttpClient by lazy {
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(15))
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        val curParts = current.removePrefix("v").trim().split(".").mapNotNull { it.toIntOrNull() }
        val latParts = latest.removePrefix("v").trim().split(".").mapNotNull { it.toIntOrNull() }
        if (curParts.isEmpty() && latParts.isNotEmpty()) return true
        val maxLen = maxOf(curParts.size, latParts.size)
        for (i in 0 until maxLen) {
            val c = curParts.getOrElse(i) { 0 }
            val l = latParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    suspend fun checkUpdate(): EngineUpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val currentVersion = Downloader.getActiveEngineVersion() ?: "Desconocida"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(CODEBERG_API_URL))
                .header("User-Agent", "GalleryDL-GUI")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                return@withContext EngineUpdateCheckResult.Error("HTTP ${response.statusCode()} al consultar Codeberg")
            }

            val release = json.decodeFromString<CodebergRelease>(response.body())
            val latestVersion = release.tagName.removePrefix("v").trim()
            val exeName = Downloader.getExecutableName()

            val matchingAsset = release.assets.firstOrNull { it.name.equals(exeName, ignoreCase = true) }
                ?: release.assets.firstOrNull { it.name.endsWith(".exe", ignoreCase = true) && !it.name.contains("x86") }

            val downloadUrl = matchingAsset?.browserDownloadUrl
                ?: "https://codeberg.org/mikf/gallery-dl/releases/download/${release.tagName}/$exeName"
            val size = matchingAsset?.size ?: 0L

            if (isNewerVersion(currentVersion, latestVersion)) {
                EngineUpdateCheckResult.UpdateAvailable(
                    currentVersion = currentVersion,
                    latestVersion = latestVersion,
                    downloadUrl = downloadUrl,
                    assetSize = size
                )
            } else {
                EngineUpdateCheckResult.UpToDate(currentVersion)
            }
        }.getOrElse { error ->
            EngineUpdateCheckResult.Error(error.message ?: "Error al comprobar actualización del motor")
        }
    }

    suspend fun downloadAndUpdate(
        downloadUrl: String,
        expectedVersion: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", "GalleryDL-GUI")
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() !in 200..299) {
                throw IllegalStateException("HTTP ${response.statusCode()} al descargar ejecutable desde $downloadUrl")
            }

            val totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1L)
            val targetDir = Downloader.getUserEngineDirectory()
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val targetFile = Downloader.getUserEngineExecutable()
            val tempFile = File(targetDir, "${targetFile.name}.part")

            response.body().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                        onProgress(progress, totalRead, totalBytes)
                    }
                    output.flush()
                }
            }

            try {
                Files.move(
                    tempFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: Exception) {
                Files.move(
                    tempFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }

            targetFile.setExecutable(true, false)

            val verifiedVersion = Downloader.getActiveEngineVersion()
            verifiedVersion ?: expectedVersion
        }
    }
}
