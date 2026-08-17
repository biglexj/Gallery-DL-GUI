package com.biglexj.gallerydl.features.DownloadFeature

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biglexj.gallerydl.utils.formatTimestamp
import com.biglexj.gallerydl.utils.getCurrentTimeMillis
import com.biglexj.gallerydl.utils.loadLocalImage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun DownloadScreen(
    downloadService: DownloadService,
    defaultDestination: String,
    initialDirectoryStructure: DirectoryStructure,
    initialHistory: List<DownloadHistoryItem>,
    onChooseDirectory: suspend (String) -> String?,
    onSaveDirectoryStructure: (DirectoryStructure) -> Unit,
    onSaveHistory: (List<DownloadHistoryItem>) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    logoPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    externalUrl: String? = null,
    externalCookiesFile: String? = null,
    externalHistoryItem: DownloadHistoryItem? = null,
    autoStart: Boolean = false,
    minimizeToTray: Boolean = true,
    onSetAutoStart: (Boolean) -> Unit = {},
    onSetMinimizeToTray: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf(defaultDestination) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // Directory structure settings
    val structureOptions = DirectoryStructure.values().toList()
    var selectedStructure by remember { mutableStateOf(initialDirectoryStructure) }

    val logs = remember { mutableStateListOf<String>() }
    var isDownloading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Listo") }
    var downloadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // History state
    val history = remember { mutableStateListOf<DownloadHistoryItem>().apply { addAll(initialHistory) } }

    LaunchedEffect(externalUrl) {
        if (!externalUrl.isNullOrBlank()) {
            url = externalUrl
        }
    }

    LaunchedEffect(externalCookiesFile) {
        if (!externalCookiesFile.isNullOrBlank()) {
            logs.add("[System] Cookies de sesión sincronizadas desde el navegador.")
        }
    }

    LaunchedEffect(externalHistoryItem) {
        if (externalHistoryItem != null) {
            if (!history.any { it.id == externalHistoryItem.id }) {
                history.add(0, externalHistoryItem)
                onSaveHistory(history.toList())
                logs.add("[System] ✅ Descarga directa completada: ${externalHistoryItem.downloadedFiles.size} imágenes guardadas en ${externalHistoryItem.outputDirectory}")
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val logsListState = rememberLazyListState()
    val historyListState = rememberLazyListState()

    // Auto-scroll logs
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logsListState.animateScrollToItem(logs.size - 1)
        }
    }

    // Unified download execution handler
    val onStartDownload: () -> Unit = {
        if (isDownloading) {
            downloadJob?.cancel()
            isDownloading = false
            statusText = "Descarga cancelada por el usuario"
            logs.add("[System] Descarga cancelada.")
        } else {
            if (url.isBlank()) {
                statusText = "El enlace no puede estar vacío"
            } else {
                logs.clear()
                isDownloading = true
                statusText = "Iniciando descarga..."
                downloadJob = coroutineScope.launch {
                    downloadService.download(url.trim(), destination.trim(), selectedStructure).collect { status ->
                        when (status) {
                            is DownloadStatus.Idle -> {}
                            is DownloadStatus.Preparing -> {
                                statusText = "Iniciando motor..."
                            }
                            is DownloadStatus.Log -> {
                                logs.add(status.message)
                            }
                            is DownloadStatus.Success -> {
                                statusText = status.message
                                isDownloading = false
                                
                                val newItem = DownloadHistoryItem(
                                    id = "hist_" + getCurrentTimeMillis(),
                                    url = url.trim(),
                                    destination = destination.trim(),
                                    timestamp = getCurrentTimeMillis(),
                                    status = "Exitoso",
                                    directoryStructure = selectedStructure.name,
                                    downloadedFiles = status.downloadedFiles,
                                    outputDirectory = status.outputDirectory,
                                    collectionTitle = status.collectionTitle
                                )
                                history.add(0, newItem)
                                onSaveHistory(history.toList())
                            }
                            is DownloadStatus.Failure -> {
                                statusText = status.error
                                isDownloading = false

                                val newItem = DownloadHistoryItem(
                                    id = "hist_" + getCurrentTimeMillis(),
                                    url = url.trim(),
                                    destination = destination.trim(),
                                    timestamp = getCurrentTimeMillis(),
                                    status = "Fallido",
                                    directoryStructure = selectedStructure.name,
                                    downloadedFiles = status.downloadedFiles,
                                    outputDirectory = status.outputDirectory,
                                    collectionTitle = status.collectionTitle
                                )
                                history.add(0, newItem)
                                onSaveHistory(history.toList())
                            }
                        }
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(16.dp)) {
        val isNarrow = maxWidth < 640.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    logoPainter?.let { painter ->
                        Image(
                            painter = painter,
                            contentDescription = "App Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                    Column {
                        Text(
                            text = "Gallery-DL GUI",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Descargador de galerías de imágenes rápido y flexible",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showSettingsDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("⚙️ Ajustes")
                }
            }

            if (showSettingsDialog) {
                AppSettingsDialog(
                    autoStart = autoStart,
                    minimizeToTray = minimizeToTray,
                    onSetAutoStart = onSetAutoStart,
                    onSetMinimizeToTray = onSetMinimizeToTray,
                    onOpenCookiesFolder = { onOpenFolder("D:\\Documentos\\cookies") },
                    onOpenUrl = onOpenUrl,
                    onDismiss = { showSettingsDialog = false }
                )
            }

            if (isNarrow) {
                // Compact Single Column Layout for narrow screens
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsCard(
                        url = url,
                        onUrlChange = { url = it },
                        destination = destination,
                        onChooseDirectory = {
                            coroutineScope.launch {
                                onChooseDirectory(destination)?.let { destination = it }
                            }
                        },
                        structureOptions = structureOptions,
                        selectedStructure = selectedStructure,
                        onStructureChange = {
                            selectedStructure = it
                            onSaveDirectoryStructure(it)
                        },
                        isDownloading = isDownloading,
                        onDownloadClick = onStartDownload,
                        statusText = statusText
                    )

                    LogsCard(
                        logs = logs,
                        listState = logsListState,
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                    )

                    HistoryCard(
                        history = history,
                        onClearHistory = {
                            history.clear()
                            onSaveHistory(emptyList())
                        },
                        onRemoveItem = { item ->
                            history.remove(item)
                            onSaveHistory(history.toList())
                        },
                        onOpenFolder = onOpenFolder,
                        onOpenUrl = onOpenUrl,
                        listState = historyListState,
                        modifier = Modifier.fillMaxWidth().height(320.dp)
                    )
                }
            } else {
                // Two Column Layout (Left: Settings & Logs Console; Right: Download History)
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column (Settings on top & Console below)
                    Column(
                        modifier = Modifier.weight(1.05f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsCard(
                            url = url,
                            onUrlChange = { url = it },
                            destination = destination,
                            onChooseDirectory = {
                                coroutineScope.launch {
                                    onChooseDirectory(destination)?.let { destination = it }
                                }
                            },
                            structureOptions = structureOptions,
                            selectedStructure = selectedStructure,
                            onStructureChange = {
                                selectedStructure = it
                                onSaveDirectoryStructure(it)
                            },
                            isDownloading = isDownloading,
                            onDownloadClick = onStartDownload,
                            statusText = statusText
                        )

                        LogsCard(
                            logs = logs,
                            listState = logsListState,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }

                    // Right Column (Download History taking full column height)
                    Column(
                        modifier = Modifier.weight(0.95f).fillMaxHeight()
                    ) {
                        HistoryCard(
                            history = history,
                            onClearHistory = {
                                history.clear()
                                onSaveHistory(emptyList())
                            },
                            onRemoveItem = { item ->
                                history.remove(item)
                                onSaveHistory(history.toList())
                            },
                            onOpenFolder = onOpenFolder,
                            onOpenUrl = onOpenUrl,
                            listState = historyListState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

// ── Reusable Component Cards ──────────────────────────────────────────

@Composable
fun SettingsCard(
    url: String,
    onUrlChange: (String) -> Unit,
    destination: String,
    onChooseDirectory: () -> Unit,
    structureOptions: List<DirectoryStructure>,
    selectedStructure: DirectoryStructure,
    onStructureChange: (DirectoryStructure) -> Unit,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    statusText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Configuración de Descarga",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // URL Input
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("Enlace / URL de la Galería") },
                placeholder = { Text("Pega aquí el enlace de la imagen o galería...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = !isDownloading,
                singleLine = true
            )

            // Destination Folder Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Styled read-only container for Destination path to avoid text selection cursor and inconsistent context menus
                Surface(
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = if (isDownloading) 0.38f else 1f)
                    ),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "Carpeta de Guardado",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDownloading) 0.38f else 1f)
                                )
                            )
                            Text(
                                text = destination,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDownloading) 0.38f else 1f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Button(
                    onClick = onChooseDirectory,
                    enabled = !isDownloading,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("📁 Buscar")
                }
            }

            SimpleDropdownField(
                label = "Estructura de Directorio",
                selectedValue = selectedStructure.label,
                options = structureOptions.map { it.label },
                onOptionSelected = { label ->
                    structureOptions.firstOrNull { it.label == label }?.let(onStructureChange)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDownloading
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDownloadClick,
                    enabled = isDownloading || url.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = if (isDownloading) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isDownloading) "Detener" else "Descargar",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Status Info
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Estado: $statusText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isDownloading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    }
}

@Composable
fun HistoryCard(
    history: List<DownloadHistoryItem>,
    onClearHistory: () -> Unit,
    onRemoveItem: (DownloadHistoryItem) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historial de Descargas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text("Limpiar todo", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "El historial está vacío.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { item ->
                        HistoryItemRow(item, onOpenFolder, onOpenUrl, onRemoveItem)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(
    item: DownloadHistoryItem,
    onOpenFolder: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onRemoveItem: (DownloadHistoryItem) -> Unit
) {
    fun openItemFolder() {
        val firstFile = item.downloadedFiles.firstOrNull()
        val actualFolder = if (!item.outputDirectory.isNullOrBlank()) {
            item.outputDirectory
        } else if (firstFile != null) {
            try {
                java.io.File(firstFile).canonicalFile.parent ?: item.destination
            } catch (e: Exception) {
                item.destination
            }
        } else {
            item.destination
        }
        onOpenFolder(actualFolder)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Load and display local thumbnail if files exist
            val firstImage = item.downloadedFiles.firstOrNull()
            val bitmap = remember(firstImage) {
                if (firstImage != null) {
                    loadLocalImage(firstImage)
                } else null
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Thumbnail",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openItemFolder() },
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openItemFolder() },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (item.status == "Exitoso") "🖼️" else "⚠️",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolveHistoryTitle(item),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = resolveHistoryOutputPath(item),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                        maxLines = 1
                    )
                }
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Web button (Luna style)
                Button(
                    onClick = { onOpenUrl(item.url) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("🌐 Web", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                // Open Folder button
                Button(
                    onClick = { openItemFolder() },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("📁 Abrir", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { onRemoveItem(item) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("🗑️", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun LogsCard(
    logs: List<String>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "Consola de salida...",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("[System Error]") || log.contains("[Error]") || log.contains("[downloader.http][warning]")) {
                                Color(0xFFFF5252)
                            } else if (log.startsWith("[System]")) {
                                Color(0xFF64B5F6)
                            } else {
                                Color(0xFFE0E0E0)
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { if (enabled) expanded = true }) {
                    Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.bodySmall)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun extractLastPathSegment(url: String): String {
    return try {
        val withoutQuery = url.substringBefore('?')
        val clean = withoutQuery.trim().removeSuffix("/")
        val segment = clean.substringAfterLast('/')
        if (segment.isNotBlank()) segment else url
    } catch (e: Exception) {
        url
    }
}

internal fun resolveHistoryOutputPath(item: DownloadHistoryItem): String {
    item.outputDirectory?.takeIf { it.isNotBlank() }?.let { return it }

    item.downloadedFiles.firstOrNull()?.let { firstFile ->
        runCatching { java.io.File(firstFile).canonicalFile.parentFile?.path }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
    }

    return item.destination
}

internal fun resolveHistoryTitle(item: DownloadHistoryItem): String {
    item.collectionTitle?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

    val outputDirectory = resolveHistoryOutputPath(item)
    val outputName = runCatching { java.io.File(outputDirectory).canonicalFile.name }
        .getOrElse { java.io.File(outputDirectory).name }
    val destinationName = runCatching { java.io.File(item.destination).canonicalFile.name }
        .getOrElse { java.io.File(item.destination).name }

    if (outputName.isNotBlank() && !outputName.equals(destinationName, ignoreCase = true)) {
        return outputName
    }

    return extractLastPathSegment(item.url)
        .replace('-', ' ')
        .replace('_', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { character ->
                if (character.isLowerCase()) character.titlecase() else character.toString()
            }
        }
        .ifBlank { extractLastPathSegment(item.url) }
}
