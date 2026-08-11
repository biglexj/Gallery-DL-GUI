# 🚀 Guía de Auto-Actualización Directa desde GitHub (Android & KMP)

Esta guía explica cómo implementar en cualquier proyecto (Android nativo o Kotlin Multiplatform) la funcionalidad de comprobar actualizaciones automáticamente desde **GitHub Releases**, mostrar el **Modal Central Interactivo al 80%**, descargar la última versión de la APK en segundo plano con reporte de progreso (0-100%) y solicitar la instalación limpia con `FileProvider`.

---

## 🛠️ Estructura del Componente

### 1. Modelo de Datos (`UpdateRelease.kt`)

```kotlin
data class UpdateRelease(
    val version: String,
    val downloadUrl: String,
    val releasePageUrl: String,
    val body: String = ""
)
```

---

### 2. Parseador y Verificador (`UpdateChecker.kt`)

```kotlin
object UpdateChecker {
    private fun extractStringValue(json: String, key: String): String? {
        val search = "\"$key\":"
        val keyIndex = json.indexOf(search)
        if (keyIndex == -1) return null
        val start = json.indexOf("\"", keyIndex + search.length)
        if (start == -1) return null
        val end = json.indexOf("\"", start + 1)
        if (end == -1) return null
        return json.substring(start + 1, end)
    }

    fun parseUpdateRelease(json: String): UpdateRelease? {
        val tagName = extractStringValue(json, "tag_name") ?: return null
        val htmlUrl = extractStringValue(json, "html_url") ?: return null
        val body = extractStringValue(json, "body").orEmpty()

        val apkIndex = json.indexOf(".apk\"")
        val downloadUrl = if (apkIndex != -1) {
            val assetsRegion = json.substring(0, apkIndex)
            val downloadKeySearch = "\"browser_download_url\":"
            val downloadKeyIndex = assetsRegion.lastIndexOf(downloadKeySearch)
            if (downloadKeyIndex != -1) {
                val start = assetsRegion.indexOf("\"", downloadKeyIndex + downloadKeySearch.length)
                if (start != -1) {
                    val end = json.indexOf("\"", start + 1)
                    if (end != -1) json.substring(start + 1, end) else htmlUrl
                } else htmlUrl
            } else htmlUrl
        } else htmlUrl

        val cleanVersion = tagName.removePrefix("v").trim()
        return UpdateRelease(
            version = cleanVersion,
            downloadUrl = downloadUrl,
            releasePageUrl = htmlUrl,
            body = body
        )
    }

    fun isNewerVersion(current: String, remote: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val remoteParts = remote.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(currentParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (c > r) return false
        }
        return false
    }
}
```

---

### 3. Modal Central Interactivo al 80% de Ancho (`UpdateModalDialog.kt`)

> [!IMPORTANT]
> Los diálogos de actualización en móvil DEBEN usar `DialogProperties(usePlatformDefaultWidth = false)` y `fillMaxWidth(0.80f)` con `widthIn(max = 480.dp)`. Esto evita el encajonamiento de Compose y garantiza que botones de acción como "Descargar" o "Instalar" no se corten.

```kotlin
@Composable
fun UpdateModalDialog(
    state: LunaFetchState,
    presenter: LunaFetchPresenter,
) {
    if (!state.showUpdateModal || state.availableUpdate == null) return

    Dialog(
        onDismissRequest = { presenter.dismissUpdateModal() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !state.isUpdateDownloading,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .widthIn(max = 480.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            // Contenido con Novedades formateadas en Markdown y Barra de Progreso (0-100%)
        }
    }
}
```

---

### 4. Cierre Síncrono de Diálogos Flotantes ("Acerca de")

Al accionar "Buscar actualizaciones" desde una ventana flotante ("Acerca de"), la interfaz DEBE:
1. Llamar a `onDismiss()` en la ventana flotante para cerrarla inmediatamente.
2. Actualizar el estado de la UI (`showUpdateModal = true`) de forma síncrona en el hilo principal antes o simultáneamente con la corrutina de red. Esto elimina cualquier pantalla vacía o destello blanco entre diálogos.

```kotlin
OutlinedButton(
    onClick = {
        onDismiss() // Cierra el modal "Acerca de"
        presenter.checkForUpdates(manual = true) // Abre síncronamente el modal de actualización
    },
    modifier = Modifier.fillMaxWidth().height(44.dp),
    shape = RoundedCornerShape(50),
) {
    Text("🔄 Buscar actualizaciones", style = MaterialTheme.typography.labelLarge)
}
```

---

### 5. Renderizado Limpio del `body` en Markdown

```kotlin
fun renderCleanReleaseNotes(rawMarkdown: String): String {
    if (rawMarkdown.isBlank()) return ""
    return rawMarkdown.lineSequence()
        .map { line ->
            var clean = line.trim()
            if (clean.startsWith("#")) {
                clean = clean.dropWhile { it == '#' || it.isWhitespace() }
            }
            clean = clean.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            clean = clean.replace(Regex("\\*(.*?)\\*"), "$1")
            if (clean.startsWith("- ") || clean.startsWith("* ")) {
                clean = "• " + clean.substring(2)
            }
            clean
        }
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
}
```

---

### 6. Descargar e Iniciar Instalación Nativa en Android (`FileProvider`)

```kotlin
fun installDownloadedApk(context: Context, filePath: String) {
    val file = File(filePath)
    if (!file.exists()) return
    val apkUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
```

---

## 📌 Permisos e Integración en `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

---

## 📝 Resumen de Reglas de Oro

1. **80% Ancho de pantalla** (`usePlatformDefaultWidth = false`) en modales de actualización.
2. **Cierre de diálogos previos**: Si el usuario consulta actualizaciones desde "Acerca de", cerrar ese cuadro primero.
3. **Transición síncrona sin parpadeos**: Actualizar el estado en el hilo principal antes de resolver la corrutina de red.
4. **Ocultamiento de Banners duplicados**: Si el modal central está activo, el `UpdateBanner` superior no se renderiza.
