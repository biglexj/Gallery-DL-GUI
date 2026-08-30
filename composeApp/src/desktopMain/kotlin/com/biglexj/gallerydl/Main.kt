package com.biglexj.gallerydl

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
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
import com.biglexj.gallerydl.platform.synapse.SynapseAction
import com.biglexj.gallerydl.platform.synapse.SynapseOutboundClient
import com.biglexj.gallerydl.platform.synapse.SynapseUriParser
import com.biglexj.gallerydl.platform.synapse.WindowsProtocolRegistrar
import com.biglexj.gallerydl.theme.ThemeMode
import java.io.File

fun main(args: Array<String>) {
    // ── Single Instance Lock & Aurora Synapse Dispatch Guarantee ───────────
    if (!SingleInstanceLock.acquireOrTransfer(args)) {
        kotlin.system.exitProcess(0)
    }

    // Registrar protocolo gallerydl:// en Windows
    WindowsProtocolRegistrar.registerProtocolIfWindows()

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
                if (px in 0..(screenSize.width - 200) && py in 0..(screenSize.height - 200)) {
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

        // Browser extension interception server & CLI/Synapse args state
        val initialSynapseAction = args.firstNotNullOfOrNull { SynapseUriParser.parse(it) }
        val cliUrl = when (initialSynapseAction) {
            is SynapseAction.Download -> initialSynapseAction.url
            is SynapseAction.Enqueue -> initialSynapseAction.url
            else -> args.firstOrNull { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
        }
        var externalUrl by remember { mutableStateOf<String?>(cliUrl) }
        var externalCookiesFile by remember { mutableStateOf<String?>(null) }
        var externalHistoryItem by remember { mutableStateOf<DownloadHistoryItem?>(null) }

        var autoStartState by remember { mutableStateOf(appSettings.autoStart) }
        var minimizeToTrayState by remember { mutableStateOf(appSettings.minimizeToTray) }
        var themeModeState by remember {
            val saved = appSettings.themeMode
            mutableStateOf(ThemeMode.values().firstOrNull { it.name.equals(saved, ignoreCase = true) } ?: ThemeMode.System)
        }
        var focusTrigger by remember { mutableStateOf(0) }

        // Listen for Synapse actions and focus requests from duplicate launches
        remember {
            com.biglexj.gallerydl.platform.SingleInstanceLock.listenForRequests { action ->
                when (action) {
                    is SynapseAction.Focus -> {
                        if (windowState.isMinimized) windowState.isMinimized = false
                        isVisible = true
                        focusTrigger++
                    }
                    is SynapseAction.Download -> {
                        externalUrl = action.url
                        if (windowState.isMinimized) windowState.isMinimized = false
                        isVisible = true
                        focusTrigger++
                    }
                    is SynapseAction.Enqueue -> {
                        externalUrl = action.url
                        if (windowState.isMinimized) windowState.isMinimized = false
                        isVisible = true
                        focusTrigger++
                    }
                    is SynapseAction.OpenFolder -> {
                        if (windowState.isMinimized) windowState.isMinimized = false
                        isVisible = true
                        focusTrigger++
                    }
                }
            }
        }

        remember {
            InterceptionServer.registerUrlListener { url ->
                externalUrl = url
                if (windowState.isMinimized) windowState.isMinimized = false
                isVisible = true
                focusTrigger++
            }
            InterceptionServer.registerCookiesListener { cookiesPath ->
                externalCookiesFile = cookiesPath
            }
            InterceptionServer.registerHistoryListener { item ->
                externalHistoryItem = item
            }
            InterceptionServer.start(appSettings)
        }

        // Logo image loading
        val awtImage = remember {
            try {
                val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("Icon.png")
                    ?: ClassLoader.getSystemResourceAsStream("Icon.png")
                    ?: File("assets/branding/icons/icon-transparent.png").takeIf { it.exists() }?.inputStream()
                stream?.use { javax.imageio.ImageIO.read(it) }
            } catch (e: Exception) {
                null
            }
        }

        val logoPainter = remember(awtImage) {
            awtImage?.let { BitmapPainter(it.toComposeImageBitmap()) }
        }

        // Setup System Tray asynchronously to prevent AWT rendering lifecycle lock
        LaunchedEffect(awtImage) {
            javax.swing.SwingUtilities.invokeLater {
                val trayImg = awtImage ?: java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB).apply {
                    val g = createGraphics()
                    g.color = java.awt.Color(0x10, 0xB9, 0x81)
                    g.fillOval(2, 2, 12, 12)
                    g.dispose()
                }
                ModernTrayManager.setupTray(
                    image = trayImg,
                    tooltip = "Gallery-DL GUI",
                    onOpenApp = {
                        isVisible = true
                        windowState.isMinimized = false
                    },
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
                        ModernTrayManager.removeTray()
                        exitApplication()
                    }
                )
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                ModernTrayManager.removeTray()
            }
        }

        Window(
            onCloseRequest = {
                saveWindowState()
                if (appSettings.minimizeToTray) {
                    isVisible = false
                } else {
                    SingleInstanceLock.release()
                    InterceptionServer.stop()
                    ModernTrayManager.removeTray()
                    exitApplication()
                }
            },
            title = if (SingleInstanceLock.isDevMode()) "Gallery-DL GUI [Dev]" else "Gallery-DL GUI",
            state = windowState,
            icon = logoPainter,
            visible = isVisible,
        ) {
            LaunchedEffect(focusTrigger) {
                java.awt.EventQueue.invokeLater {
                    window.isMinimized = false
                    window.toFront()
                    window.requestFocus()
                }
            }

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
                    themeMode = themeModeState,
                    onSetAutoStart = { enabled ->
                        autoStartState = enabled
                        appSettings.autoStart = enabled
                    },
                    onSetMinimizeToTray = { enabled ->
                        minimizeToTrayState = enabled
                        appSettings.minimizeToTray = enabled
                    },
                    onThemeModeSelected = { mode ->
                        themeModeState = mode
                        appSettings.themeMode = mode.name
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
