# 🚀 Regla y Sistema de Auto-Actualización desde GitHub Releases

> [!IMPORTANT]
> Todos los proyectos de aplicación (Android, Kotlin Multiplatform, Desktop) DEBEN incluir compatibilidad para la **comprobación automática, descarga interactiva e instalación directa de actualizaciones desde GitHub Releases**.

## Requerimientos Obligatorios

1. **Verificación Silenciosa al Iniciar**:
   - Al iniciar la aplicación, consultar la API de GitHub (`https://api.github.com/repos/{owner}/{repo}/releases/latest`) en segundo plano sin interrumpir al usuario.
   - **No mostrar ningún mensaje** si ya se está en la última versión (el check de inicio es transparente).
2. **Verificación Manual**:
   - Si el usuario pulsa "Buscar actualizaciones" (en Ajustes o en el diálogo Acerca de), mostrar la verificación inmediatamente:
     - Hay nueva versión → Abrir el `UpdateModalDialog` al 80% de ancho de forma síncrona.
     - Ya en la última versión → Mostrar un **Toast flotante centrado en la parte superior** (✅ "¡Estás en la última versión!") que se desvanece automáticamente a los **4 segundos**. No usar diálogos bloqueantes para este caso.
3. **Transición Flotante y Cierre de Diálogos Secundarios [CRÍTICO]**:
   - Al accionar "Buscar actualizaciones" desde un diálogo flotante (ej. "Acerca de"), la ventana flotante DEBE cerrarse en la misma acción y activar `showUpdateModal = true` de forma síncrona en el hilo principal.
   - **Prohibido**: No dejar gaps ni estados intermedios vacíos que causen parpadeos o destellos blancos entre modales. Si el "Acerca de" es una página completa, no aplica el cierre.
4. **Modal Central Interactivo (Ancho al 80%) [CRÍTICO]**:
   - En Android y escritorio, mostrar las actualizaciones dentro de un **Modal Central Interactivo (`UpdateModalDialog`)**.
   - El diálogo DEBE usar `DialogProperties(usePlatformDefaultWidth = false)` con `fillMaxWidth(0.80f)` (máximo 480.dp) para garantizar espacio holgado sin compresión de botones.
   - Ocultar automáticamente el `UpdateBanner` superior cuando el modal central esté visible (`state.showUpdateModal == true`).
5. **Sanitización Canónica del `body` en Markdown [CRÍTICO]**:
   - Las notas de versión DEBEN sanitizarse mediante la función canónica **`sanitizeReleaseNotes(body: String): String`** para eliminar Markdown crudo (`#`, `**`, `*`, `-`, enlaces `[]()` y etiquetas HTML) antes de mostrarse en la UI.
   - Las líneas de encabezado (`#`) se convierten en texto plano. Los ítems de lista (`-`, `*`) se reemplazan por `• `. El resultado se muestra en un contenedor desplazable con títulos legibles.
6. **Descarga e Instalación Directa en Android**:
   - Utilizar flujo de red HTTP con seguimiento de redirecciones 302/307 y reporte de progreso en vivo (0-100%).
   - Validar la integridad del archivo ejecutable o APK (`PK\x03\x04` magic bytes).
   - Solicitar la instalación limpia mediante `androidx.core.content.FileProvider` (`Intent.ACTION_VIEW` con `application/vnd.android.package-archive`).
7. **Notificación de Usuario (Toast Flotante)**:
   - Los avisos de "Sin actualizaciones" o errores de red deben mostrarse mediante un **Toast flotante** (sin bloqueo de UI), posicionado **centrado en la parte superior** de la pantalla, con una duración exacta de **4 segundos** antes de desaparecer.
8. **Consistencia Paritaria Windows Desktop JVM [CRÍTICO]**:
   - Ninguna funcionalidad de descarga o actualización debe estar restringida únicamente a Android.
   - El motor **`AutoDownloader`** DEBE ejecutar peticiones HTTP asíncronas en ambas plataformas, descargando al almacenamiento persistente (`.elytesia/` en Windows / `filesDir` en Android) con barra o indicador de progreso (0-100%) visible.
   - Para versiones Windows JVM, incluir la lógica de ejecución del instalador (`.exe` o `.msi`) mediante `ProcessBuilder` con privilegios solicitados y cierre de la instancia actual.
9. **Verificación Previa Obligatoria de la Release en GitHub [CRÍTICO]**:
   - Antes de cambiar el número de versión (`versionName` / `versionCode`), redactar notas de versión (`RELEASE_NOTES.md`) o preparar anuncios (`RELEASE_MESSAGE.md`), el agente DEBE verificar la última release publicada en GitHub Releases (`https://api.github.com/repos/{owner}/{repo}/releases/latest`).
   - La nueva versión DEBE ser estrictamente superior a la última tag publicada en GitHub para evitar colisiones de versionado (ej. si `v1.1.0` ya fue lanzada en GitHub, el nuevo lanzamiento DEBE ser `v1.1.1`).
10. **Script Oficial de Release `build-release.ps1` [CRÍTICO]**:
   - Si el proyecto tiene un script oficial de release (ej. `build-release.ps1`), el agente DEBE usarlo siempre. Nunca hacer commits, tags ni `gh release create` manualmente:
     ```powershell
     .\build-release.ps1 -Version "X.Y.Z"
     ```
   - El script compila, firma, adjunta artefactos (EXE/MSI/MSIX) y publica la release en GitHub de forma atómica.
   - **Título de la Release (CRÍTICO)**: El título debe ser únicamente `{NombreApp} vX.Y.Z` (ej. `LyraFlow v1.1.1`). **Prohibido** añadir subtítulos o descripciones largas al título; el detalle va siempre en el body (`RELEASE_MESSAGE.md`).
