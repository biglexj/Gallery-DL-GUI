# Actualizador del Motor (gallery-dl) & Preparación de Release v1.0.1 — Tareas

- Estado: `COMPLETED`

## Ejecución

- [x] T01 — Descargar y actualizar `bin/gallery-dl.exe` a `v1.32.19` en el repositorio.
- [x] T02 — Crear `EngineUpdateService` en `com.biglexj.gallerydl.platform` para consulta de releases en Codeberg y descarga atómica a `%APPDATA%\Gallery-DL-GUI\bin\gallery-dl.exe`.
- [x] T03 — Actualizar `Downloader.kt` para incorporar y priorizar el ejecutable actualizado en AppData.
- [x] T04 — Integrar la interfaz del actualizador de motor en `AppSettingsDialog.kt` y cablear los callbacks a través de `DownloadScreen.kt`, `App.kt` y `Main.kt`.
- [x] T05 — Añadir pruebas unitarias para `Downloader` y resolución de versión de motor.
- [x] T06 — Compilar y ejecutar `./gradlew desktopTest` y verificar compilación limpia.
- [x] T07 — Actualizar `RELEASE_NOTES.md`, `RELEASE_MESSAGE.md` y `ROADMAP.md` documentando el actualizador del motor.
- [x] T08 — Preparar la validación en `VALIDATION.md`.

Las pruebas no se documentan aquí. Deben registrarse en `VALIDATION.md`.
