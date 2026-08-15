package com.biglexj.gallerydl.features.DownloadFeature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AppSettingsDialog(
    autoStart: Boolean,
    minimizeToTray: Boolean,
    onSetAutoStart: (Boolean) -> Unit,
    onSetMinimizeToTray: (Boolean) -> Unit,
    onOpenCookiesFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚙️ Ajustes del Sistema",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Iniciar con Windows (Autorun)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Iniciar con Windows",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Inicia Gallery-DL GUI automáticamente en segundo plano al encender tu PC.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoStart,
                        onCheckedChange = onSetAutoStart
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 2. Minimizar a la bandeja
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Minimizar a la bandeja del sistema",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Al pulsar cerrar (X), la ventana se oculta en el área de notificación de la barra de tareas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = minimizeToTray,
                        onCheckedChange = onSetMinimizeToTray
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 3. Conector de Extensión Web
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Conector de Navegador (Extensión)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Servidor local activo en 127.0.0.1:18274. Permite que la extensión de Chrome envíe descargas y sincronice cookies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onOpenCookiesFolder,
                        modifier = Modifier.padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📁 Abrir carpeta de cookies sincronizadas")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Listo")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
