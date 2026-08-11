package com.biglexj.gallerydl

import androidx.compose.runtime.remember
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

        // Window state restoration (desktop_app_standards.md Rule 5)
        val initialPlacement = if (appSettings.windowIsMaximized) WindowPlacement.Maximized else WindowPlacement.Floating
        val initialPosition = run {
            val px = appSettings.windowPositionX
            val py = appSettings.windowPositionY
            if (px != null && py != null) {
                val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
                if (px in 0..(screenSize.width - 100) && py in 0..(screenSize.height - 100)) {
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

        // Detect default destination
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

        val findIconFile = {
            val userDir = System.getProperty("user.dir") ?: "."
            val paths = listOf(
                "icon/Icon.png",
                "../icon/Icon.png",
                "composeApp/icon/Icon.png",
                "$userDir/icon/Icon.png",
                "$userDir/../icon/Icon.png"
            )
            paths.map { File(it) }.firstOrNull { it.exists() }
        }

        val logoPainter = remember {
            try {
                val iconFile = findIconFile()
                if (iconFile != null) {
                    val image = javax.imageio.ImageIO.read(iconFile)
                    androidx.compose.ui.graphics.painter.BitmapPainter(image.toComposeImageBitmap())
                } else {
                    null
                }
            } catch (e: java.lang.Exception) {
                null
            }
        }

        Window(
            onCloseRequest = {
                saveWindowState()
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
                                // If direct folder doesn't exist, try its parent
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
