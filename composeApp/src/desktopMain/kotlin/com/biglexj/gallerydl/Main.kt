package com.biglexj.gallerydl

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.biglexj.gallerydl.features.DownloadFeature.DirectoryStructure
import com.biglexj.gallerydl.platform.AppSettings
import com.biglexj.gallerydl.platform.DesktopDownloadService
import com.biglexj.gallerydl.platform.DirectoryChooser
import java.io.File

fun main() {
    application {
        val appSettings = remember { AppSettings() }
        val downloadService = remember { DesktopDownloadService() }

        // Window state restoration
        val initialPlacement = if (appSettings.windowIsMaximized) WindowPlacement.Maximized else WindowPlacement.Floating
        val initialPosition = run {
            val px = appSettings.windowPositionX
            val py = appSettings.windowPositionY
            if (px != null && py != null) {
                // Static boundary check, avoids AWT Toolkit deadlock
                if (px in 0..3840 && py in 0..2160) {
                    WindowPosition(px.dp, py.dp)
                } else {
                    WindowPosition(androidx.compose.ui.Alignment.Center)
                }
            } else {
                WindowPosition(androidx.compose.ui.Alignment.Center)
            }
        }

        val windowState = rememberWindowState(
            placement = initialPlacement,
            position = initialPosition,
            width = appSettings.windowWidth.dp,
            height = appSettings.windowHeight.dp,
        )

        fun saveWindowState() {
            if (windowState.placement == WindowPlacement.Maximized) {
                appSettings.windowIsMaximized = true
            } else {
                appSettings.windowIsMaximized = false
                appSettings.windowWidth = windowState.size.width.value.toInt()
                appSettings.windowHeight = windowState.size.height.value.toInt()
                val pos = windowState.position
                if (pos is WindowPosition.Absolute) {
                    appSettings.windowPositionX = pos.x.value.toInt()
                    appSettings.windowPositionY = pos.y.value.toInt()
                }
            }
        }

        val defaultDest = appSettings.lastDestination ?: run {
            val home = System.getProperty("user.home")
            val downloads = File(home, "Downloads")
            if (downloads.exists()) downloads.absolutePath else home
        }

        val initialDirStruct = run {
            val name = appSettings.defaultDirectoryStructure
            DirectoryStructure.values().firstOrNull { it.name == name } ?: DirectoryStructure.Flat
        }
        val initialHistory = remember { appSettings.loadHistory() }

        // Browser extension interception server state
        var externalUrl by remember { mutableStateOf<String?>(null) }
        var externalCookiesFile by remember { mutableStateOf<String?>(null) }
        var externalHistoryItem by remember { mutableStateOf<com.biglexj.gallerydl.features.DownloadFeature.DownloadHistoryItem?>(null) }

        remember {
            com.biglexj.gallerydl.platform.InterceptionServer.registerUrlListener { url ->
                externalUrl = url
            }
            com.biglexj.gallerydl.platform.InterceptionServer.registerCookiesListener { cookiesPath ->
                externalCookiesFile = cookiesPath
            }
            com.biglexj.gallerydl.platform.InterceptionServer.registerHistoryListener { item ->
                externalHistoryItem = item
            }
            com.biglexj.gallerydl.platform.InterceptionServer.start(appSettings)
        }

        // Logo: load from disk only (no classpath blocking scan)
        val logoPainter = remember {
            try {
                val userDir = System.getProperty("user.dir") ?: "."
                val paths = listOf(
                    "icon/Icon.png",
                    "../icon/Icon.png",
                    "composeApp/icon/Icon.png",
                    "$userDir/icon/Icon.png",
                    "$userDir/../icon/Icon.png"
                )
                val iconFile = paths.map { File(it) }.firstOrNull { it.exists() }
                if (iconFile != null) {
                    val image = javax.imageio.ImageIO.read(iconFile)
                    androidx.compose.ui.graphics.painter.BitmapPainter(image.toComposeImageBitmap())
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        Window(
            onCloseRequest = {
                saveWindowState()
                com.biglexj.gallerydl.platform.InterceptionServer.stop()
                exitApplication()
            },
            title = "Gallery-DL GUI",
            state = windowState,
            icon = logoPainter,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.foundation.LocalContextMenuRepresentation provides androidx.compose.foundation.DarkDefaultContextMenuRepresentation
            ) {
                App(
                    downloadService = downloadService,
                    defaultDestination = defaultDest,
                    initialDirectoryStructure = initialDirStruct,
                    initialHistory = initialHistory,
                    logoPainter = logoPainter,
                    externalUrl = externalUrl,
                    externalCookiesFile = externalCookiesFile,
                    externalHistoryItem = externalHistoryItem,
                    onChooseDirectory = { current ->
                        DirectoryChooser.chooseDirectory(current)?.also { selected ->
                            appSettings.lastDestination = selected
                        }
                    },
                    onSaveDirectoryStructure = { struct ->
                        appSettings.defaultDirectoryStructure = struct.name
                    },
                    onSaveHistory = { history ->
                        appSettings.saveHistory(history)
                    },
                    onOpenFolder = { path ->
                        try {
                            val file = File(path)
                            if (file.exists()) {
                                java.awt.Desktop.getDesktop().open(file)
                            } else {
                                file.parentFile?.let { parent ->
                                    if (parent.exists()) {
                                        java.awt.Desktop.getDesktop().open(parent)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                    },
                    onOpenUrl = { urlString ->
                        try {
                            if (java.awt.Desktop.isDesktopSupported()) {
                                val desktop = java.awt.Desktop.getDesktop()
                                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                                    desktop.browse(java.net.URI(urlString))
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                )
            }
        }
    }
}
