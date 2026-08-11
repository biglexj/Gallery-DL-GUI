package com.biglexj.gallerydl.platform

import com.biglexj.gallerydl.features.DownloadFeature.DownloadHistoryItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.prefs.Preferences

class AppSettings(
    private val historyFile: Path = defaultHistoryFile()
) {
    private val prefs = Preferences.userRoot().node("com/biglexj/gallerydl")
    private val historySerializer = ListSerializer(DownloadHistoryItem.serializer())
    private val historyJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    var windowWidth: Int
        get() = prefs.getInt("window_width", 1024) // Slightly wider by default as requested
        set(value) = prefs.putInt("window_width", value)

    var windowHeight: Int
        get() = prefs.getInt("window_height", 768)
        set(value) = prefs.putInt("window_height", value)

    var windowPositionX: Int?
        get() = prefs.getInt("window_x", -1).takeIf { it != -1 }
        set(value) = if (value != null) prefs.putInt("window_x", value) else prefs.remove("window_x")

    var windowPositionY: Int?
        get() = prefs.getInt("window_y", -1).takeIf { it != -1 }
        set(value) = if (value != null) prefs.putInt("window_y", value) else prefs.remove("window_y")

    var windowIsMaximized: Boolean
        get() = prefs.getBoolean("window_maximized", false)
        set(value) = prefs.putBoolean("window_maximized", value)

    var lastDestination: String?
        get() = prefs.get("last_destination", null)
        set(value) = if (value != null) prefs.put("last_destination", value) else prefs.remove("last_destination")

    var defaultDirectoryStructure: String
        get() = prefs.get("default_directory_structure", "Flat")
        set(value) = prefs.put("default_directory_structure", value)

    fun loadHistory(): List<DownloadHistoryItem> {
        loadHistoryFile()?.let { return it }

        // Migración desde versiones anteriores, que guardaban todo el historial
        // dentro de Preferences y podían superar su límite de tamaño en Windows.
        val legacyJson = prefs.get(HISTORY_PREFERENCE_KEY, null) ?: return emptyList()
        val legacyHistory = decodeHistory(legacyJson) ?: return emptyList()
        if (writeHistoryFile(legacyHistory)) {
            prefs.remove(HISTORY_PREFERENCE_KEY)
        }
        return legacyHistory
    }

    fun saveHistory(history: List<DownloadHistoryItem>) {
        if (writeHistoryFile(history)) {
            // El archivo JSON sustituye definitivamente al valor limitado de Preferences.
            prefs.remove(HISTORY_PREFERENCE_KEY)
        }
    }

    private fun loadHistoryFile(): List<DownloadHistoryItem>? {
        if (!Files.isRegularFile(historyFile)) return null

        return runCatching {
            decodeHistory(Files.readString(historyFile))
        }.onFailure { error ->
            System.err.println("No se pudo leer el historial de descargas: ${error.message}")
        }.getOrNull()
    }

    private fun decodeHistory(json: String): List<DownloadHistoryItem>? {
        if (json.isBlank()) return emptyList()
        return runCatching {
            historyJson.decodeFromString(historySerializer, json)
        }.onFailure { error ->
            System.err.println("No se pudo interpretar el historial de descargas: ${error.message}")
        }.getOrNull()
    }

    private fun writeHistoryFile(history: List<DownloadHistoryItem>): Boolean {
        return runCatching {
            val parent = historyFile.parent
            if (parent != null) Files.createDirectories(parent)

            val json = historyJson.encodeToString(historySerializer, history)
            val temporaryFile = historyFile.resolveSibling("${historyFile.fileName}.tmp")
            Files.writeString(temporaryFile, json)

            try {
                Files.move(
                    temporaryFile,
                    historyFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: Exception) {
                Files.move(
                    temporaryFile,
                    historyFile,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }.onFailure { error ->
            System.err.println("No se pudo guardar el historial de descargas: ${error.message}")
        }.isSuccess
    }

    companion object {
        private const val HISTORY_PREFERENCE_KEY = "download_history"

        private fun defaultHistoryFile(): Path {
            val configRoot = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
                ?: System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
                ?: Paths.get(System.getProperty("user.home"), ".config").toString()

            return Paths.get(configRoot, "Gallery-DL-GUI", "download-history.json")
        }
    }
}
