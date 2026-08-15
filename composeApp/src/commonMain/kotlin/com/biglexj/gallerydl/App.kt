package com.biglexj.gallerydl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.biglexj.gallerydl.features.DownloadFeature.DownloadScreen
import com.biglexj.gallerydl.features.DownloadFeature.DownloadService
import com.biglexj.gallerydl.features.DownloadFeature.DownloadHistoryItem
import com.biglexj.gallerydl.features.DownloadFeature.DirectoryStructure
import com.biglexj.gallerydl.theme.GalleryDLTheme

@Composable
fun App(
    downloadService: DownloadService,
    defaultDestination: String,
    initialDirectoryStructure: DirectoryStructure,
    initialHistory: List<DownloadHistoryItem>,
    logoPainter: Painter? = null,
    externalUrl: String? = null,
    externalCookiesFile: String? = null,
    externalHistoryItem: DownloadHistoryItem? = null,
    onChooseDirectory: suspend (String) -> String?,
    onSaveDirectoryStructure: (DirectoryStructure) -> Unit,
    onSaveHistory: (List<DownloadHistoryItem>) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    GalleryDLTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            DownloadScreen(
                downloadService = downloadService,
                defaultDestination = defaultDestination,
                initialDirectoryStructure = initialDirectoryStructure,
                initialHistory = initialHistory,
                logoPainter = logoPainter,
                externalUrl = externalUrl,
                externalCookiesFile = externalCookiesFile,
                externalHistoryItem = externalHistoryItem,
                onChooseDirectory = onChooseDirectory,
                onSaveDirectoryStructure = onSaveDirectoryStructure,
                onSaveHistory = onSaveHistory,
                onOpenFolder = onOpenFolder,
                onOpenUrl = onOpenUrl
            )
        }
    }
}
