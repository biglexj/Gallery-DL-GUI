package com.biglexj.gallerydl.features.DownloadFeature

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

enum class DirectoryStructure(val label: String) {
    Flat("Sin Subcarpetas"),
    CollectionOnly("Solo Carpeta de Colección"),
    AuthorCollection("Autor / Colección"),
    Default("Estructura Completa")
}

@Serializable
data class DownloadHistoryItem(
    val id: String,
    val url: String,
    val destination: String,
    val timestamp: Long,
    val status: String, // "Exitoso" o "Fallido"
    val directoryStructure: String = "Default",
    val downloadedFiles: List<String> = emptyList(),
    val outputDirectory: String? = null,
    val collectionTitle: String? = null
)

interface DownloadService {
    fun download(
        url: String,
        destination: String,
        directoryStructure: DirectoryStructure
    ): Flow<DownloadStatus>
    fun cancel()
}

sealed interface DownloadStatus {
    object Idle : DownloadStatus
    object Preparing : DownloadStatus
    data class Log(val message: String) : DownloadStatus
    data class Success(
        val message: String,
        val downloadedFiles: List<String>,
        val outputDirectory: String?,
        val collectionTitle: String?
    ) : DownloadStatus
    data class Failure(
        val error: String,
        val downloadedFiles: List<String>,
        val outputDirectory: String?,
        val collectionTitle: String?
    ) : DownloadStatus
}
