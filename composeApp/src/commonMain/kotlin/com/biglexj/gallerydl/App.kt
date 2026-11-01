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
    autoStart: Boolean = false,
    minimizeToTray: Boolean = true,
    themeMode: com.biglexj.gallerydl.theme.ThemeMode = com.biglexj.gallerydl.theme.ThemeMode.System,
    engineVersion: String? = null,
    cookieStatus: String? = null,
    onSetAutoStart: (Boolean) -> Unit = {},
    onSetMinimizeToTray: (Boolean) -> Unit = {},
    onThemeModeSelected: (com.biglexj.gallerydl.theme.ThemeMode) -> Unit = {},
    onReplaceCookie: (suspend () -> String?)? = null,
    onUpdateEngine: (suspend ((String) -> Unit) -> Unit)? = null,
    onChooseDirectory: suspend (String) -> String?,
    onSaveDirectoryStructure: (DirectoryStructure) -> Unit,
    onSaveHistory: (List<DownloadHistoryItem>) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    GalleryDLTheme(mode = themeMode) {
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
                autoStart = autoStart,
                minimizeToTray = minimizeToTray,
                themeMode = themeMode,
                engineVersion = engineVersion,
                cookieStatus = cookieStatus,
                onSetAutoStart = onSetAutoStart,
                onSetMinimizeToTray = onSetMinimizeToTray,
                onThemeModeSelected = onThemeModeSelected,
                onReplaceCookie = onReplaceCookie,
                onUpdateEngine = onUpdateEngine,
                onChooseDirectory = onChooseDirectory,
                onSaveDirectoryStructure = onSaveDirectoryStructure,
                onSaveHistory = onSaveHistory,
                onOpenFolder = onOpenFolder,
                onOpenUrl = onOpenUrl
            )
        }
    }
}
