# Flujo local de release

Decisiones y pasos propios de Gallery-DL GUI para preparar y publicar una versión. Las bases comunes están en Core: [base de lanzamientos](../../Docs/global/releases/release-baseline.md), [protocolo de lanzamiento y actualización](../../Docs/global/releases/protocolo_lanzamiento_actualizacion.md) y [mensaje público para la web](../../Docs/global/releases/web-release-message-standard.md).

## Comandos

La interfaz de comandos es Mise:

| Comando | Efecto |
|---|---|
| `mise dev` | Ejecuta la aplicación en modo desarrollo (`:composeApp:run`). |
| `mise build` | Compila el ejecutable de escritorio (`:composeApp:packageExe`), sin instalador. |
| `mise check` | Verificación estática de compilación (`:composeApp:compileKotlinDesktop`), sin empaquetar. |
| `mise test` | Pruebas de escritorio (`:composeApp:desktopTest`). |
| `mise release` | Prepara los artefactos de distribución en `release/`. **No publica.** |
| `mise publish` | Publica la versión en GitHub Releases. Requiere autorización explícita. |

`--plan` es válido en `dev`, `build`, `check`, `test` y `release`: muestra el comando previsto sin ejecutar procesos, escribir archivos ni consultar la red. `--check` es válido en `publish`: comprueba estado del repositorio, tag, artefactos y notas en modo de solo lectura.

`gradlew` y `gradlew.bat` son wrappers técnicos exigidos por Gradle; se conservan, pero la interfaz del proyecto es Mise.

## Scripts

| Script | Función |
|---|---|
| `scripts/release/build-release.ps1` | Script oficial de build, empaquetado y publicación. Con `-LocalOnly` prepara artefactos y **no publica**. |
| `scripts/release/ReleaseTools.ps1` | Utilidades compartidas: JDK completo, herramientas del SDK, versión semántica, preflight de publicación, firma y comprobación de la constante de versión. |
| `scripts/packaging/Build-Installer.ps1` | Genera el instalador Inno Setup a partir de la distribución de jpackage y aplica el candado anti-build-obsoleto. |
| `scripts/mise-task.ts` | Despachador único de las tareas de Mise. |

`scripts/build/build-release.ps1` es una copia local antigua **no versionada** (la regla `**/build/` de `.gitignore` excluye esa carpeta). No es la ruta vigente: la versión que se versiona y que ejecuta Mise es `scripts/release/build-release.ps1`.

## Versionado

- SemVer estricto `MAJOR.MINOR.PATCH`, sin límite artificial de dígitos: `1.2.12` es válido. No aplica la regla del `.9` de versiones anteriores del proyecto.
- La versión pública es inmutable. Si la versión local ya está publicada, hay que avanzar a la siguiente.
- `gallerydlgui.versionName` y `gallerydlgui.versionCode` viven en `gradle.properties`. `versionCode` es un contador independiente y siempre creciente.
- `AppVersion.VALUE` en `composeApp/src/commonMain/kotlin/com/biglexj/gallerydl/AppVersion.kt` muestra la versión en pantalla. `Assert-AppVersionConstant` aborta el build si no coincide con `gradle.properties`.

## Artefactos

Una versión publica dos ejecutables en Windows, porque usan motores, registros y rutas de instalación distintos:

| Artefacto | Motor | Ruta de instalación |
|---|---|---|
| `GalleryDL-GUI-Setup-<versión>.exe` | Inno Setup (plantilla Biglex Modern 2.0) | `%LOCALAPPDATA%\Programs\GalleryDL-GUI` |
| `GalleryDL-GUI-Windows-<versión>.exe` | jpackage / WiX (legado) | `%LOCALAPPDATA%\GalleryDL-GUI` |

El instalador Inno Setup es el recomendado y actualiza la versión anterior en la misma carpeta. El `.exe` heredado se conserva por compatibilidad con las instalaciones MSI previas. La guía completa está en [`packaging/windows/MIGRATION.md`](../packaging/windows/MIGRATION.md).

Además se publica `SHA256SUMS.txt` con los hashes de los artefactos, en formato GNU coreutils (hash en minúsculas, dos espacios, nombre) y con saltos LF.

Los manifiestos WinGet viven en `packaging/winget/manifests/b/biglexj/GalleryDLGUI/<versión>/` y se validan con `winget validate` durante la publicación.

## Reglas de publicación

1. La publicación exige rama `main`, remoto `origin` coherente con `biglexj/Gallery-DL-GUI`, `main` sincronizado con `origin/main`, `gh` autenticado y tag y release inexistentes. Lo comprueba `Assert-PublishPreflight`.
2. Una publicación remota no puede usar `-SkipSigning`.
3. La firma usa `GalleryDLGUI_Dev_Certificate.pfx`. `Assert-SignedArtifact` comprueba que el firmante sea `CN=biglexj`.
4. `git add -A` y el commit/tag los hace el script oficial, nunca a mano.
5. El mensaje público se toma de `RELEASE_MESSAGE.md` y se adjunta como cuerpo de la GitHub Release, sin rutas locales, volcados de consola ni nombres de variables internas.

## Pendiente de decisión

La ficha del producto en la web de Aurora (`biglexj.com`) se alimenta del endpoint administrativo `POST /api/admin/developer-app-releases` descrito en el [estándar web de Core](../../Docs/global/releases/web-release-message-standard.md). **Este proyecto no tiene todavía una integración para ese envío**, y `mise publish` no lo anuncia ni lo ejecuta. Hasta que exista, la publicación se limita a GitHub Releases, y Aurora resuelve la versión desde la release pública como respaldo.
