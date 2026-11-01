# Actualizador del Motor (gallery-dl) & Preparación de Release v1.0.1 — Aprobación

- Proceso: `2026-11-01_actualizador_motor`
- Fecha: `2026-11-01`
- Estado: `APPROVED`

## Resumen de ejecución

Se ha completado la implementación del actualizador integrado del motor (`gallery-dl`) y la actualización del binario base a `v1.32.19`:

1. **Binario de fábrica (`bin/gallery-dl.exe`)**: Descargado y verificado en `v1.32.19` desde Codeberg (`mikf/gallery-dl`).
2. **Servicio desacoplado (`EngineUpdateService`)**:
   - Consulta a la API oficial de Codeberg (`/api/v1/repos/mikf/gallery-dl/releases/latest`).
   - Comparación semántica de versiones.
   - Descarga atómica en espacio de usuario (`%APPDATA%\Gallery-DL-GUI\bin\gallery-dl.exe`).
3. **Resolución de ruta (`Downloader.kt`)**: Prioriza el ejecutable de usuario en `AppData` sobre los recursos empaquetados.
4. **Interfaz de usuario (`AppSettingsDialog.kt`)**:
   - Badge visual con la versión activa del motor.
   - Botón interactivo «🔄 Actualizar» con feedback y barra de progreso.
5. **Pruebas y Validación**:
   - Pruebas unitarias de resolución y comparación semántica en `DownloaderTest.kt` (`BUILD SUCCESSFUL`).
   - Sincronización de `RELEASE_NOTES.md`, `RELEASE_MESSAGE.md` y `ROADMAP.md` para la versión `1.0.1`.

## Decisión final

- [x] Aprobado para integración y preparación del release `1.0.1`.
