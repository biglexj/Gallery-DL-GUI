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
import com.biglexj.gallerydl.features.DownloadFeature.DownloadHistoryItem
import com.biglexj.gallerydl.platform.AppSettings
import com.biglexj.gallerydl.platform.DesktopDownloadService
import com.biglexj.gallerydl.platform.DirectoryChooser
import com.biglexj.gallerydl.platform.InterceptionServer
import com.biglexj.gallerydl.platform.ModernTrayManager
import com.biglexj.gallerydl.platform.SingleInstanceLock
import java.io.File

fun main(args: Array<String>) {
    // ── Single Instance Lock Guarantee ──────────────────────────────────────────
    if (!SingleInstanceLock.acquireOrBringToFront()) {
        kotlin.system.exitProcess(0)
    }

    val isAutostart = args.contains("--autostart")

    application {
        val appSettings = remember { AppSettings() }
        val downloadService = remember { DesktopDownloadService() }
        var isVisible by remember { mutableStateOf(!isAutostart) }

        // ── Window State Persistence (desktop_app_standards.md Rule 5) ────────
        val initialPlacement = if (appSettings.windowIsMaximized) WindowPlacement.Maximized else WindowPlacement.Floating
        val initialPosition = run {
            val px = appSettings.windowPositionX
            val py = appSettings.windowPositionY
            if (px != null && py != null) {
                val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
                if (px >= 0 && px <= (screenSize.width - 400) && py >= 0 && py <= (screenSize.height - 300)) {
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
            width = appSettings.windowWidth.coerceIn(800, 3840).dp,
            height = appSettings.windowHeight.coerceIn(600, 2160).dp,
        )

        fun saveWindowState() {
            if (windowState.placement == WindowPlacement.Maximized) {
                appSettings.windowIsMaximized = true
            } else if (windowState.placement == WindowPlacement.Floating && !windowState.isMinimized) {
                appSettings.windowIsMaximized = false
                val w = windowState.size.width.value.toInt()
                val h = windowState.size.height.value.toInt()
                if (w in 600..3840 && h in 400..2160) {
                    appSettings.windowWidth = w
                    appSettings.windowHeight = h
                }
                val pos = windowState.position
                if (pos is WindowPosition.Absolute) {
                    val x = pos.x.value.toInt()
                    val y = pos.y.value.toInt()
                    val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
                    if (x in 0..(screenSize.width - 200) && y in 0..(screenSize.height - 200)) {
                        appSettings.windowPositionX = x
                        appSettings.windowPositionY = y
                    }
                }
            }
        }

        // Continuous State Observation
        androidx.compose.runtime.LaunchedEffect(windowState.placement, windowState.size, windowState.position) {
            if (!windowState.isMinimized) {
                saveWindowState()
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
        var externalHistoryItem by remember { mutableStateOf<DownloadHistoryItem?>(null) }

        var autoStartState by remember { mutableStateOf(appSettings.autoStart) }
        var minimizeToTrayState by remember { mutableStateOf(appSettings.minimizeToTray) }

        remember {
            InterceptionServer.registerUrlListener { url ->
                externalUrl = url
                if (windowState.isMinimized) windowState.isMinimized = false
                isVisible = true
            }
            InterceptionServer.registerCookiesListener { cookiesPath ->
                externalCookiesFile = cookiesPath
            }
            InterceptionServer.registerHistoryListener { item ->
                externalHistoryItem = item
            }
            InterceptionServer.start(appSettings)

            SingleInstanceLock.listenForFocusRequests {
                if (windowState.isMinimized) windowState.isMinimized = false
                isVisible = true
            }
        }

        // Logo image loading with classpath & filesystem fallback
        val awtImage = remember {
            try {
                val resourceNames = listOf("Icon.png", "icon.png", "/Icon.png", "/icon.png")
                var loaded: java.awt.image.BufferedImage? = null
                for (name in resourceNames) {
                    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(name)
                        ?: Unit.javaClass.getResourceAsStream(name)
                        ?: ClassLoader.getSystemResourceAsStream(name)
                    if (stream != null) {
                        loaded = stream.use { javax.imageio.ImageIO.read(it) }
                        if (loaded != null) break
                    }
                }
                if (loaded != null) return@remember loaded

                val appResDir = System.getProperty("compose.application.resources.dir") ?: ""
                val userDir = System.getProperty("user.dir") ?: "."
                val paths = listOf(
                    "$appResDir/Icon.png",
                    "$appResDir/icon.png",
                    "icon/Icon.png",
                    "icon/icon.png",
                    "../icon/Icon.png",
                    "composeApp/icon/Icon.png",
                    "$userDir/icon/Icon.png",
                    "$userDir/../icon/Icon.png"
                )
                paths.map { File(it) }.firstOrNull { it.exists() }?.let { file ->
                    javax.imageio.ImageIO.read(file)
                }
            } catch (e: Exception) {
                null
            }
        }

        val logoPainter = remember(awtImage) {
            awtImage?.let { BitmapPainter(it.toComposeImageBitmap()) }
        }

        // Setup System Tray
        val trayIcon = remember(awtImage) {
            val trayImg = awtImage ?: java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            ModernTrayManager.setupTray(
                image = trayImg,
                tooltip = "Gallery-DL GUI",
                onOpenApp = { isVisible = true },
                onOpenDownloadsFolder = {
                    try {
                        val file = File(defaultDest)
                        if (file.exists()) java.awt.Desktop.getDesktop().open(file)
                    } catch (_: Exception) {}
                },
                onQuitApp = {
                    saveWindowState()
                    SingleInstanceLock.release()
                    InterceptionServer.stop()
                    exitApplication()
                }
            )
        }

        androidx.compose.runtime.DisposableEffect(trayIcon) {
            onDispose {
                trayIcon?.let { t -> runCatching { java.awt.SystemTray.getSystemTray().remove(t) } }
            }
        }

        Window(
            onCloseRequest = {
                saveWindowState()
                isVisible = false
            },
            title = "Gallery-DL GUI",
            state = windowState,
            icon = logoPainter,
            visible = isVisible,
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
                    autoStart = autoStartState,
                    minimizeToTray = minimizeToTrayState,
                    onSetAutoStart = { enabled ->
                        autoStartState = enabled
                        appSettings.autoStart = enabled
                    },
                    onSetMinimizeToTray = { enabled ->
                        minimizeToTrayState = enabled
                        appSettings.minimizeToTray = enabled
                    },
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
