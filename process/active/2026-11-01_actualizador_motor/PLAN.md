# Actualizador del Motor (gallery-dl) & Preparación de Release v1.0.1 — Plan

- Estado: `COMPLETED`
- Fecha: `2026-11-01`
- Proyecto: `Gallery-DL-GUI`

## Objetivo

Implementar un actualizador integrado del motor (`gallery-dl`) que permita detectar la versión local en ejecución, consultar las nuevas versiones publicadas en Codeberg (`mikf/gallery-dl`), descargar el binario actualizado de forma atómica en el espacio de usuario (`%APPDATA%\Gallery-DL-GUI\bin\`) y actualizar el binario base empaquetado a `v1.32.19`, preparando el proyecto para el lanzamiento oficial de la versión `1.0.1`.

## Alcance

- Incluye:
  - Creación del servicio `EngineUpdateService` para consulta y descarga de actualizaciones de `gallery-dl` vía Codeberg API.
  - Adaptación de `Downloader.kt` para priorizar el binario actualizado del usuario en `%APPDATA%\Gallery-DL-GUI\bin\gallery-dl.exe` con fallback a los recursos empaquetados y PATH.
  - Integración en la interfaz de `AppSettingsDialog.kt` bajo "Controladores del motor", mostrando versión activa y botón interactivo con progreso y estados ("Buscar actualización", "Descargando X%", "✅ Motor actualizado").
  - Actualización del binario base en el repositorio `bin/gallery-dl.exe` a `v1.32.19`.
  - Preparación de `RELEASE_NOTES.md`, `ROADMAP.md` y comprobación preflight de empaquetado para el release `1.0.1`.
- No incluye:
  - Modificación de la extensión de Chrome/Edge (ya operativa en v1.0.1).

## Enfoque

1. Actualizar el binario empaquetado en el repositorio `bin/gallery-dl.exe` con la versión oficial `v1.32.19` descargada desde Codeberg.
2. Implementar `EngineUpdateService` en `composeApp/src/desktopMain/kotlin/com/biglexj/gallerydl/platform/EngineUpdateService.kt` con soporte para verificación de versión actual (`--version`), consulta a la API de Codeberg (`/api/v1/repos/mikf/gallery-dl/releases/latest`) y descarga segura atómica.
3. Actualizar `Downloader.kt` para resolver en primer orden el binario de usuario en `%APPDATA%\Gallery-DL-GUI\bin\`.
4. Conectar el servicio en `Main.kt`, `App.kt`, `DownloadScreen.kt` y `AppSettingsDialog.kt`, permitiendo comprobar y actualizar el motor con feedback visual en tiempo real.
5. Ejecutar pruebas unitarias y de compilación (`./gradlew desktopTest`, `./gradlew compileKotlinDesktop`).
6. Actualizar la documentación de lanzamiento (`RELEASE_NOTES.md`, `ROADMAP.md`) y validar los scripts de empaquetado.

## Criterios de finalización

- [x] `bin/gallery-dl.exe` actualizado a `v1.32.19` y verificado con `--version`.
- [x] `EngineUpdateService` consulta la API de Codeberg, compara versiones y descarga atómicamente el ejecutable.
- [x] `Downloader.getExecutablePath()` detecta y prioriza `%APPDATA%\Gallery-DL-GUI\bin\gallery-dl.exe`.
- [x] `AppSettingsDialog` muestra la versión activa del motor y permite actualizarlo interactivamente.
- [x] Pruebas automáticas (`desktopTest`) y compilación exitosa sin regresiones.
- [x] Artefactos de release y notas preparadas para publicación.
