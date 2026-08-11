package com.biglexj.gallerydl.platform

import com.biglexj.gallerydl.features.DownloadFeature.DirectoryStructure
import com.biglexj.gallerydl.features.DownloadFeature.DownloadService
import com.biglexj.gallerydl.features.DownloadFeature.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference

internal const val DOWNLOADED_FILE_MARKER = "__GALLERY_DL_FILE__"
internal const val OUTPUT_DIRECTORY_MARKER = "__GALLERY_DL_DIRECTORY__"
internal const val COLLECTION_TITLE_MARKER = "__GALLERY_DL_COLLECTION__"

internal fun buildGalleryDlConfig(
    destination: String,
    extractorKey: String,
    directoryStructure: DirectoryStructure
): String {
    val directorySegments = when (directoryStructure) {
        DirectoryStructure.Flat -> emptyList()
        DirectoryStructure.CollectionOnly -> listOf("{album|gallery_title|folder[title]|category}")
        DirectoryStructure.AuthorCollection -> listOf(
            "{user|author[username]|username|folder[owner]}",
            "{album|gallery_title|folder[title]|category}"
        )
        DirectoryStructure.Default -> null
    }

    fun directoryJson() = buildJsonArray {
        directorySegments.orEmpty().forEach { add(JsonPrimitive(it)) }
    }

    val extractorConfig = buildJsonObject {
        put("base-directory", JsonPrimitive(destination.replace('\\', '/')))
        directorySegments?.let { put("directory", directoryJson()) }

        if (extractorKey.isNotBlank() && (directorySegments != null || extractorKey == "deviantart")) {
            put(extractorKey, buildJsonObject {
                directorySegments?.let { put("directory", directoryJson()) }

                if (extractorKey == "deviantart") {
                    // Gallery-DL 1.32+ desactiva las descargas "original" de DeviantArt
                    // por defecto. El ejecutable incluido aún usa el valor antiguo y genera
                    // URLs WixMP que responden 400, así que replicamos el valor actual.
                    put("original", JsonPrimitive(false))
                }
            })
        }
    }

    return buildJsonObject {
        put("extractor", extractorConfig)
    }.toString()
}

internal fun buildDeviantartOverrideOptions(): List<String> = buildList {
    // Gallery-DL aplica las opciones de izquierda a derecha y `--option` tiene
    // prioridad sobre el JSON temporal. Se escriben en el nivel específico y
    // en el nivel raíz porque distintas versiones del extractor consultan
    // rutas de configuración diferentes.
    add("--option")
    add("extractor.deviantart.original=false")
    add("--option")
    add("original=false")
}

internal fun normalizeGalleryDlOutputPath(value: String): String {
    return value.trim()
        .removePrefix("# ")
        .removePrefix("#")
        .trim()
        .removeSurrounding("\"")
}

internal fun resolveDownloadedFiles(candidates: List<String>, destination: String): List<String> {
    return candidates.mapNotNull { candidate ->
        val normalizedCandidate = normalizeGalleryDlOutputPath(candidate)
        val rawFile = File(normalizedCandidate)
        val resolved = if (rawFile.isAbsolute) rawFile else File(destination, normalizedCandidate)
        if (!resolved.exists() || !resolved.isFile) {
            null
        } else {
            runCatching { resolved.canonicalPath }.getOrElse { resolved.absolutePath }
        }
    }.distinct()
}

internal fun resolveOutputDirectory(
    downloadedFiles: List<String>,
    candidates: List<String>,
    destination: String,
    directoryCandidates: List<String> = emptyList()
): String? {
    val completedFileDirectory = downloadedFiles.firstOrNull()?.let { firstFile ->
        runCatching { File(firstFile).canonicalFile.parentFile }
            .getOrNull()
            ?.takeIf { it.exists() && it.isDirectory }
    }
    if (completedFileDirectory != null) return completedFileDirectory.path

    val explicitDirectory = directoryCandidates.firstNotNullOfOrNull { candidate ->
        val normalizedCandidate = normalizeGalleryDlOutputPath(candidate)
        val rawDirectory = File(normalizedCandidate)
        val resolvedDirectory = if (rawDirectory.isAbsolute) rawDirectory else File(destination, normalizedCandidate)
        runCatching { resolvedDirectory.canonicalFile }
            .getOrNull()
            ?.takeIf { it.exists() && it.isDirectory }
            ?.path
    }
    if (explicitDirectory != null) return explicitDirectory

    return candidates.firstNotNullOfOrNull { candidate ->
        val normalizedCandidate = normalizeGalleryDlOutputPath(candidate)
        val rawFile = File(normalizedCandidate)
        val resolvedFile = if (rawFile.isAbsolute) rawFile else File(destination, normalizedCandidate)
        runCatching { resolvedFile.canonicalFile.parentFile }
            .getOrNull()
            ?.takeIf { it.exists() && it.isDirectory }
            ?.path
    }
}

private fun buildFailureMessage(exitCode: Int, diagnostics: List<String>): String {
    val detail = diagnostics
        .asReversed()
        .firstOrNull { it.isNotBlank() }
        ?.removePrefix("[System Error] ")
        ?.take(260)

    return if (detail != null) {
        "Gallery-DL terminó con código $exitCode: $detail"
    } else {
        "Gallery-DL terminó con código de error: $exitCode"
    }
}

class DesktopDownloadService : DownloadService {
    private val activeProcess = AtomicReference<Process?>(null)

    override fun download(
        url: String,
        destination: String,
        directoryStructure: DirectoryStructure
    ): Flow<DownloadStatus> = callbackFlow {
        trySend(DownloadStatus.Preparing)

        val executable = Downloader.getExecutablePath()
        val extractorKey = extractExtractorKey(url)

        val configJson = buildGalleryDlConfig(
            destination = destination,
            extractorKey = extractorKey,
            directoryStructure = directoryStructure
        )

        val configFile = try {
            Files.createTempFile("gallery-dl-gui-", ".json").toFile().apply {
                writeText(configJson, Charsets.UTF_8)
            }
        } catch (error: Exception) {
            trySend(
                DownloadStatus.Failure(
                    "No se pudo preparar la configuración temporal: ${error.localizedMessage ?: "error desconocido"}",
                    emptyList(),
                    null,
                    null
                )
            )
            channel.close()
            return@callbackFlow
        }

        val command = mutableListOf(
            executable,
            "--config-ignore",
            "--config", configFile.absolutePath,
            "--no-input",
            "--no-colors",
            "--Print", "$DOWNLOADED_FILE_MARKER{_path}",
            "--Print", "$OUTPUT_DIRECTORY_MARKER{_directory}",
            "--Print", "$COLLECTION_TITLE_MARKER{folder[title]|collection[title]|album|gallery_title|title}"
        )

        // Se repiten como argumentos de máxima prioridad para evitar que una
        // configuración externa o una diferencia de carga del ejecutable 1.31.7
        // reactive las URLs images-wixmp que fallan con HTTP 400.
        if (extractorKey == "deviantart") {
            command.addAll(buildDeviantartOverrideOptions())
        }

        command.add(url)

        val processBuilder = ProcessBuilder(command)
        processBuilder.environment().putAll(System.getenv())

        val executableFile = File(executable)
        if (executableFile.exists() && executableFile.parentFile != null) {
            processBuilder.directory(executableFile.parentFile)
        }

        val fileCandidates = mutableListOf<String>()
        val directoryCandidates = mutableListOf<String>()
        val collectionTitles = mutableListOf<String>()
        val diagnostics = mutableListOf<String>()

        fun rememberCandidate(path: String) {
            if (path.isBlank()) return
            synchronized(fileCandidates) {
                if (path !in fileCandidates) fileCandidates.add(path)
            }
        }

        fun rememberDirectory(path: String) {
            if (path.isBlank()) return
            synchronized(directoryCandidates) {
                if (path !in directoryCandidates) directoryCandidates.add(path)
            }
        }

        fun rememberCollectionTitle(title: String) {
            val cleanTitle = title.trim()
            if (cleanTitle.isBlank() || cleanTitle.equals("none", ignoreCase = true) ||
                cleanTitle.equals("null", ignoreCase = true)
            ) return
            synchronized(collectionTitles) {
                if (cleanTitle !in collectionTitles) collectionTitles.add(cleanTitle)
            }
        }

        try {
            val process = processBuilder.start()
            activeProcess.set(process)

            val readerThread = Thread {
                process.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val cleanLine = line.trim()
                        when {
                            cleanLine.startsWith(DOWNLOADED_FILE_MARKER) -> {
                                rememberCandidate(cleanLine.removePrefix(DOWNLOADED_FILE_MARKER).trim())
                            }
                            cleanLine.startsWith(OUTPUT_DIRECTORY_MARKER) -> {
                                rememberDirectory(cleanLine.removePrefix(OUTPUT_DIRECTORY_MARKER).trim())
                            }
                            cleanLine.startsWith(COLLECTION_TITLE_MARKER) -> {
                                rememberCollectionTitle(cleanLine.removePrefix(COLLECTION_TITLE_MARKER).trim())
                            }
                            else -> {
                                trySend(DownloadStatus.Log(line))

                                val normalizedPath = normalizeGalleryDlOutputPath(cleanLine)
                                val lower = normalizedPath.lowercase()
                                if (lower.endsWith(".png") || lower.endsWith(".jpg") ||
                                    lower.endsWith(".jpeg") || lower.endsWith(".webp") ||
                                    lower.endsWith(".gif") || lower.endsWith(".mp4")
                                ) {
                                    rememberCandidate(normalizedPath)
                                }
                            }
                        }
                    }
                }
            }

            val errorThread = Thread {
                process.errorStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val logLine = "[System Error] $line"
                        synchronized(diagnostics) { diagnostics.add(logLine) }
                        trySend(DownloadStatus.Log(logLine))
                    }
                }
            }

            readerThread.start()
            errorThread.start()

            val exitCode = process.waitFor()
            readerThread.join()
            errorThread.join()
            activeProcess.compareAndSet(process, null)

            val candidatesSnapshot = synchronized(fileCandidates) { fileCandidates.toList() }
            val directoriesSnapshot = synchronized(directoryCandidates) { directoryCandidates.toList() }
            val collectionTitle = synchronized(collectionTitles) { collectionTitles.firstOrNull() }
            val downloadedFiles = resolveDownloadedFiles(candidatesSnapshot, destination)
            val outputDirectory = resolveOutputDirectory(
                downloadedFiles,
                candidatesSnapshot,
                destination,
                directoriesSnapshot
            )

            if (exitCode == 0) {
                trySend(
                    DownloadStatus.Success(
                        "Descarga finalizada exitosamente.",
                        downloadedFiles,
                        outputDirectory,
                        collectionTitle
                    )
                )
            } else {
                val diagnosticSnapshot = synchronized(diagnostics) { diagnostics.toList() }
                trySend(
                    DownloadStatus.Failure(
                        buildFailureMessage(exitCode, diagnosticSnapshot),
                        downloadedFiles,
                        outputDirectory,
                        collectionTitle
                    )
                )
            }
            channel.close()
        } catch (error: Exception) {
            val candidatesSnapshot = synchronized(fileCandidates) { fileCandidates.toList() }
            val directoriesSnapshot = synchronized(directoryCandidates) { directoryCandidates.toList() }
            val collectionTitle = synchronized(collectionTitles) { collectionTitles.firstOrNull() }
            val downloadedFiles = resolveDownloadedFiles(candidatesSnapshot, destination)
            trySend(
                DownloadStatus.Failure(
                    "Fallo al iniciar Gallery-DL: ${error.localizedMessage ?: "error desconocido"}",
                    downloadedFiles,
                    resolveOutputDirectory(downloadedFiles, candidatesSnapshot, destination, directoriesSnapshot),
                    collectionTitle
                )
            )
            channel.close()
        }

        awaitClose {
            cancel()
            runCatching { Files.deleteIfExists(configFile.toPath()) }
        }
    }.flowOn(Dispatchers.IO)

    override fun cancel() {
        activeProcess.getAndSet(null)?.let { process ->
            runCatching { process.destroyForcibly() }
        }
    }

    private fun extractExtractorKey(url: String): String {
        return try {
            val cleanUrl = url.trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("www.")
            val host = cleanUrl.substringBefore('/')
            val parts = host.split('.')
            val domain = if (parts.size >= 2) {
                val mainPart = parts[parts.size - 2]
                if (mainPart == "co" || mainPart == "com" || mainPart == "net" || mainPart == "org") {
                    if (parts.size >= 3) parts[parts.size - 3] else mainPart
                } else {
                    mainPart
                }
            } else {
                host
            }

            when (domain.lowercase()) {
                "x" -> "twitter"
                else -> domain.lowercase()
            }
        } catch (_: Exception) {
            ""
        }
    }
}
