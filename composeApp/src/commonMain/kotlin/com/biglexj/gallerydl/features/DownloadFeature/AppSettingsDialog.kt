package com.biglexj.gallerydl.features.DownloadFeature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun AppSettingsDialog(
    autoStart: Boolean,
    minimizeToTray: Boolean,
    onSetAutoStart: (Boolean) -> Unit,
    onSetMinimizeToTray: (Boolean) -> Unit,
    onOpenCookiesFolder: () -> Unit,
    onOpenUrl: (String) -> Unit,
    appVersion: String = "0.0.2",
    onDismiss: () -> Unit
) {
    var extensionEnabled by remember { mutableStateOf(true) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints {
        val isMobile = maxWidth < 520.dp
        val dialogModifier = if (isMobile) {
            Modifier.widthIn(max = 440.dp).fillMaxWidth(0.85f)
        } else {
            Modifier.widthIn(max = 500.dp).fillMaxWidth(0.92f)
        }
        val mainSpacing = if (isMobile) 10.dp else 8.dp
        val dividerPadding = if (isMobile) 4.dp else 2.dp

        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = dialogModifier,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "v$appVersion",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(mainSpacing)
                ) {
                    // 1. Iniciar con Windows
                    SettingsRow(
                        title = "Iniciar con Windows",
                        subtitle = "Abre Gallery-DL GUI automáticamente al encender el equipo.",
                        checked = autoStart,
                        onCheckedChange = onSetAutoStart
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = dividerPadding),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )

                    // 2. Minimizar en lugar de cerrar
                    SettingsRow(
                        title = "Minimizar en lugar de cerrar",
                        subtitle = "Al pulsar ✕, la app se oculta en la bandeja del sistema.",
                        checked = minimizeToTray,
                        onCheckedChange = onSetMinimizeToTray
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = dividerPadding),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )

                    // 3. Extensión de navegador
                    SettingsRow(
                        title = "Extensión de navegador",
                        subtitle = if (extensionEnabled)
                            "Host registrado en Windows. Abre la página para agregar la extensión a Chrome/Edge."
                        else
                            "Activa la conexión para sincronizar cookies y descargar galerías con un clic.",
                        checked = extensionEnabled,
                        onCheckedChange = { isChecked ->
                            extensionEnabled = isChecked
                            if (isChecked) {
                                onOpenUrl("https://github.com/biglexj/Gallery-DL-GUI/tree/main/extension#readme")
                            }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = dividerPadding),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )

                    // 4. Controladores del motor / Cookies
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Controladores del motor",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Componentes de extracción y cookies sincronizadas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenCookiesFolder,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("📁 Cookies", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = dividerPadding),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )

                    // 5. Sección Acerca de Gallery-DL GUI
                    Column(verticalArrangement = Arrangement.spacedBy(if (isMobile) 8.dp else 6.dp)) {
                        Text(
                            text = "Acerca de Gallery-DL GUI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Una herramienta moderna y gratuita para descargar galerías de imágenes, álbumes y contenido multimedia de cientos de sitios web con alta velocidad y calidad original.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Botón de donación oficial principal
                        Button(
                            onClick = { onOpenUrl("https://www.biglexj.com/donaciones") },
                            modifier = Modifier.fillMaxWidth().height(if (isMobile) 42.dp else 38.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00796B),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            Text(
                                "Donar (Yape / Plin / Web) 🤍",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        // Fila de botones secundarios de apoyo
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onOpenUrl("https://buymeacoffee.com/biglexj") },
                                modifier = Modifier.weight(1f).height(if (isMobile) 38.dp else 36.dp),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                Text("☕ Buy Me a Coffee", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onOpenUrl("https://github.com/biglexj/Gallery-DL-GUI") },
                                modifier = Modifier.weight(1f).height(if (isMobile) 38.dp else 36.dp),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                Text("⭐ GitHub", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Buscar actualizaciones
                        OutlinedButton(
                            onClick = {
                                updateMessage = "✅ Estás en la última versión (v$appVersion)"
                            },
                            modifier = Modifier.fillMaxWidth().height(if (isMobile) 38.dp else 36.dp),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            Text("🔄 Buscar actualizaciones de la app", style = MaterialTheme.typography.labelMedium)
                        }

                        if (updateMessage != null) {
                            Text(
                                text = updateMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = if (isMobile) 4.dp else 0.dp, bottom = 0.dp)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
