# Migración de instalador: MSI (jpackage + WiX) → EXE (Inno Setup, Biglex Modern 2.0)

Documento de referencia del instalador de Windows de **Gallery DL GUI**.

- Instalador anterior: `GalleryDL-GUI-<version>.msi` generado por `jpackage` (WiX),
  producido por Compose Desktop (`composeApp/build.gradle.kts`, `TargetFormat.Msi`).
- Instalador nuevo: `GalleryDL-GUI-Setup-<version>.exe` generado con Inno Setup 6.7.3
  usando la plantilla compartida **Biglex Modern 2.0** (`packaging/windows/setup.iss`).

> Este documento describe el estado **verificado en la máquina de desarrollo**. La
> aceptación visual humana del nuevo instalador no está presupuesta.

## 1. Identidades técnicas (por qué son instaladores distintos)

| | MSI (jpackage + WiX) | EXE (Inno Setup) |
| --- | --- | --- |
| Motor | Windows Installer (msiexec) | Inno Setup 6.7.3 |
| Identidad | `ProductCode` cambia en cada compilación; `UpgradeCode` `{D7D11F71-2856-4B35-827C-9EE05A0B9A67}` | `AppId` `{802F319B-753A-4A89-A679-A0CC8AC9B521}` (estable) |
| Registro | Windows Installer (`HKLM\…\Installer`, producto por usuario) | `HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\{802F319B-…}` |
| Desinstalador | `MsiExec.exe /X{ProductCode}` | `unins000.exe` en la carpeta de instalación |
| Ruta por defecto | `%LOCALAPPDATA%\GalleryDL-GUI` | `%LOCALAPPDATA%\Programs\GalleryDL-GUI` |
| Privilegios | por usuario (`perUserInstall = true`) | por usuario (`PrivilegesRequired=lowest`), sin elevación |

Datos comprobados en la máquina:

- MSI de referencia `composeApp\build\compose\binaries\main\msi\GalleryDL-GUI-0.0.2.msi`
  → `ProductCode {A9B54C0C-AB57-349D-BFFF-340BB0BAFAAF}`, `UpgradeCode {D7D11F71-2856-4B35-827C-9EE05A0B9A67}`.
- Instalación MSI **1.0.0** presente en la máquina, registrada en
  `HKLM\Software\Microsoft\Windows\CurrentVersion\Uninstall\{3829D65C-BC12-305A-967A-74EA572096EC}`,
  con `InstallLocation = C:\Users\biglexj\AppData\Local\GalleryDL-GUI\`.
  **No se desinstaló ni se modificó durante este trabajo.**

### Por qué el `AppId` de Inno NO puede ser el `UpgradeCode` del MSI

Windows Installer solo reconoce actualizaciones entre paquetes que comparten
`UpgradeCode` **y** respetan sus reglas de *major upgrade*. Un instalador Inno
Setup es un ejecutable independiente: no participa del modelo de *Windows Installer*
en absoluto. Usar `{D7D11F71-…}` como `AppId` de Inno solo provocaría que Inno
confundiera su propia clave de desinstalación con la del MSI y que su
desinstalador intentara borrar archivos del MSI. Por eso `identity.iss` declara un
`AppId` propio, estable y exclusivo.

## 2. Coexistencia MSI/EXE

**Sí pueden coexistir**, y esa es la decisión adoptada:

1. **Motores y registros distintos.** El MSI se registra en el catálogo de Windows
   Installer; Inno Setup registra su propia entrada `Uninstall` bajo `HKCU`. Ninguno
   pisa la clave del otro y ambos aparecen por separado en «Aplicaciones y
   características».
2. **Rutas distintas.** El MSI instala en `%LOCALAPPDATA%\GalleryDL-GUI` y el
   instalador nuevo en `%LOCALAPPDATA%\Programs\GalleryDL-GUI`. No comparten
   archivos, de modo que una desinstalación no borra la otra.
3. **Accesos directos con nombre propio.** El MSI crea «GalleryDL-GUI»; Inno crea
   «Gallery DL GUI» (menu Inicio y escritorio). Conviven sin colisión de nombre.

### Estado real de esta máquina

| Componente | Estado | Nota |
| --- | --- | --- |
| MSI 1.0.0 | **Instalado** (`%LOCALAPPDATA%\GalleryDL-GUI`) | Intacto, no desinstalado |
| EXE Inno 1.0.0 | Instalado por esta validación | `%LOCALAPPDATA%\Programs\GalleryDL-GUI` |
| Datos de usuario | Intactos | `%APPDATA%\Gallery-DL-GUI\download-history.json` y `%APPDATA%\gallery-dl` |

### Desinstalar el MSI antes de migrar (recomendado, no obligatorio)

Windows Installer mantiene la instalación antigua registrada y sus accesos
directos. Si el usuario quiere **una sola** instalación:

1. «Aplicaciones y características» → **GalleryDL-GUI** (el MSI) → *Desinstalar*.
   O por consola: `msiexec /x {3829D65C-BC12-305A-967A-74EA572096EC}`.
2. Después, instalar `GalleryDL-GUI-Setup-<version>.exe`.

El desinstalador del MSI **no** borra `%APPDATA%\Gallery-DL-GUI` (los datos de
usuario viven ahí, no en la carpeta de instalación). Aun así, la comprobación
recomendada es hacer una copia de `%APPDATA%\Gallery-DL-GUI` antes de desinstalar.

> Nota: el `ProductCode` del MSI 1.0.0 instalado es `{3829D65C-…}`; el MSI 0.0.2
> archivado en el repositorio es `{A9B54C0C-…}`. Son paquetes distintos.

### Qué rutas deja cada uno

| | MSI | EXE (Inno) |
| --- | --- | --- |
| Programa | `%LOCALAPPDATA%\GalleryDL-GUI\` | `%LOCALAPPDATA%\Programs\GalleryDL-GUI\` |
| Registro | `HKLM\…\Uninstall\{3829D65C-…}` | `HKCU\…\Uninstall\{802F319B-…}` |
| Datos de usuario | `%APPDATA%\Gallery-DL-GUI\` (compartido) | `%APPDATA%\Gallery-DL-GUI\` (compartido, **nunca** lo borra Inno) |

## 3. Manifiestos de WinGet

Los manifiestos viven en `packaging/winget/manifests/b/biglexj/GalleryDLGUI/<version>/`.

| Versión | `InstallerType` publicado | Artefacto | Estado |
| --- | --- | --- | --- |
| 0.0.1 | `wix` | `GalleryDL-GUI-Windows-0.0.1.msi` | Publicado. **No se toca.** |
| 0.0.2 | `wix` | `GalleryDL-GUI-Windows-0.0.2.msi` | Publicado. **No se toca.** |
| 1.0.0 | `exe` | `GalleryDL-GUI-Windows-1.0.0.exe` | Publicado. **No se toca.** |

### Decisión sobre las versiones ya publicadas

En WinGet, la versión de un paquete es **inmutable**: una vez aceptada en
`microsoft/winget-pkgs`, el manifiesto de esa versión no se reescribe. Reescribir
`1.0.0` (o `0.0.1` / `0.0.2`) a mano solo produciría un pull request rechazado.

Además, el manifiesto publicado de **1.0.0** tiene un problema real que conviene
dejar registrado: declara `InstallerType: exe` con `InstallerSwitches: /passive`
(`/passive` es un interruptor de NSIS, no de Inno Setup ni de jpackage) y apunta al
ejecutable único de jpackage, que **no registra desinstalador**. Es decir, ese
manifiesto no describe un instalador con desinstalación. **No se modifica** (la
versión es inmutable), pero **no se debe reutilizar como plantilla**: las
versiones nuevas usan el instalador Inno Setup.

### Manifiesto preparado para las versiones nuevas

A partir de la próxima versión, `scripts/build/build-release.ps1` genera el
manifiesto apuntando al instalador de Inno Setup:

```yaml
InstallerType: inno
Scope: user
InstallModes:
  - interactive
  - silent
  - silentWithProgress
InstallerSwitches:
  Silent: /VERYSILENT /SUPPRESSMSGBOXES /NORESTART /SP-
  SilentWithProgress: /SILENT /SUPPRESSMSGBOXES /NORESTART /SP-
UpgradeBehavior: install
```

**Sobre `inno` frente a `exe`.** El esquema de WinGet (`winget-manifest.installer.1.10.0`)
acepta ambos: `inno` está pensado para instaladores Inno Setup y permite que el
agente aplique sus interruptores y detecte el desinstalador; `exe` es el tipo
genérico y obliga a declarar cada interruptor a mano. Se eligió `inno` porque el
artefacto nuevo **es** un instalador Inno Setup. La variante genérica `exe`, si se
prefiere, es:

```yaml
InstallerType: exe
Scope: user
InstallModes:
  - interactive
  - silent
  - silentWithProgress
InstallerSwitches:
  Silent: /VERYSILENT /SUPPRESSMSGBOXES /NORESTART /SP-
  SilentWithProgress: /SILENT /SUPPRESSMSGBOXES /NORESTART /SP-
UpgradeBehavior: install
```

En ambos casos el `InstallerSha256` y el `InstallerUrl` apuntan a
`GalleryDL-GUI-Setup-<version>.exe`, y el `UpgradeBehavior: install` hace que una
versión nueva **actualice** la anterior por el mismo `AppId`, sin instalación
paralela.

Ambas variantes se validaron con `winget validate --ignore-warnings` (resultado:
«Validación del manifiesto correcta», código 0) sobre un manifiesto de prueba de la
versión 1.0.1 generado en `temp/`. No se publicó nada.

## 4. Validación realizada en la máquina (21-10-2026)

Ejecutada sobre `release\GalleryDL-GUI-Setup-1.0.0.exe` compilado con Inno Setup
6.7.3 y firmado con el certificado de desarrollo.

| Comprobación | Resultado |
| --- | --- |
| Compilación (Gradle + ISCC) | Correcta. `Successful compile`, 70,7 MB, SHA256 `b5b4a20c…` (build con la plantilla acumulada de las 23:35) |
| Ícono centrado (logo 64×64) | Correcto. Glifo 28×26 px; desplazamiento del centro respecto al centro de la caja: **dx = 0 px, dy = 0 px** (antes: glifo 22×20, **dx = −4 px, dy = −4 px**) |
| Ícono centrado (cabecera 20×20) | Correcto. Glifo 16×14 px; **dx = 0 px, dy = 0 px** (antes: glifo 12×11, **dx = −2 px, dy = −2,5 px**) |
| Foco inicial en la ruta | Correcto: al abrir, el campo de ruta ya tiene el foco (TEdit) y su indicador visible, **sin** pulsar Tab |
| Foco en «Finalizar» (éxito) | Correcto: la pantalla de éxito entrega el foco al botón «Finalizar» (no a la casilla «Ejecutar»), de modo que Enter cierra |
| Área cliente y contrato visual | 560 × 444 (+ marco y barra nativos). Controles en las coordenadas del contrato: instalador (20,388) 520×46, tarjeta (20,262) 520×70, logo (248,96) 64×64, icono (260,108) 40×40, título (20,174), separador (20,60) |
| Instalación por usuario sin elevación | Correcta. Log de Inno: `User privileges: None`, `Administrative install mode: No`, raíz `HKEY_CURRENT_USER` |
| Teclado en la entrada | `Tab`: ruta → carpeta → instalar → ruta (tecla real). `Shift+Tab`: instalar → carpeta → ruta → instalar |
| Campo de ruta editable | Correcto: una tecla real inserta el carácter en el campo (`xC:\TEST\EDITABLE`); ruta larga renderizada dentro de la tarjeta sin desbordar |
| Selector de carpeta | Se abre de verdad (diálogo nativo «Selecciona la ubicación de instalación») con `Enter` **y** con `Espacio` |
| `Enter` instala | Correcto: entra en la página nativa «Instalando» |
| `Enter` en «Finalizar» (pantalla de éxito) | Correcto: cierra el instalador (antes no cerraba) |
| Cancelación con la X | Correcto: `WM_CLOSE` → confirmación «¿Salir de la instalación?» → «Sí» cancela sin tocar la instalación existente |
| Desinstalación | Limpia (código 0): borra la carpeta de programa, los accesos directos y la clave de desinstalación. **No** borra `%APPDATA%\Gallery-DL-GUI` ni `%APPDATA%\gallery-dl`, y no toca la instalación MSI ni su clave en `HKLM` |
| Reinstalación / actualización | `1.0.0` → `1.0.1` con el mismo `AppId`: misma carpeta, misma clave de desinstalación, versión actualizada, **una sola** carpeta en `%LOCALAPPDATA%\Programs`. Sin instalación paralela |
| App abre | Correcto: la app instalada por Inno abre su ventana (`Gallery-DL GUI`) desde `%LOCALAPPDATA%\Programs\GalleryDL-GUI` |
| Tema claro y oscuro | Capturas reales de ambos (`screenshots/01` y `screenshots/02`). El tema oscuro es el del sistema en esa máquina; el claro se compiló con `/DMyAppTheme=light`. Ícono centrado en ambos (dx = 0 px en los dos) |
| Manifiesto winget nuevo | `winget validate` correcto con `inno` **y** con `exe` (manifiesto de prueba de la 1.0.1 en `temp/`; no se publicó nada) |
| Flujo de release versionado | `scripts\release\build-release.ps1 -LocalOnly -SkipBuild` se ejecutó de principio a fin: invocó `Build-Installer.ps1`, dejó `GalleryDL-GUI-Setup-1.0.0.exe` (firmado), el EXE heredado y `SHA256SUMS.txt` con ambos hashes. Sin commit, tag ni publicación |

### Cómo se midió el centrado del ícono

Captura de la ventana del instalador (`PrintWindow`), región del recuadro leída en
coordenadas de cliente (logo 248,96 64×64; cabecera 20,19 20×20). Se descartan
los píxeles iguales al fondo del recuadro y al fondo de la ventana, y con los
restantes se toma la caja envolvente del glifo. El desplazamiento reportado es
la distancia entre el **centroide del glifo** y el **centro de la caja**:
`dx` horizontal, `dy` vertical. Antes (plantilla de las 21:29) el glifo quedaba
pegado a la esquina superior izquierda; después (plantilla de las 23:35) el
centroide coincide con el centro de la caja.

### Pendiente

- **Escalas 125 %, 150 % y 200 %: no verificadas físicamente.** Requieren cambiar la
  escala del sistema y **reiniciar la sesión**; en esta máquina no se hizo para no
  interrumpir el trabajo de los demás agentes. El código escala una sola vez con
  `ScaleX`/`ScaleY` (contrato de la plantilla), pero eso no sustituye la prueba.
- El MSI 1.0.0 que estaba instalado en la máquina **sigue instalado** y no se tocó.
- La aceptación visual humana del nuevo instalador no está presupuesta: lo anterior
  son comprobaciones técnicas y capturas, no una aprobación de diseño.

### Capturas

En `packaging\windows\screenshots\`:

1. `01-entrada-tema-oscuro.png` — entrada (tema oscuro del sistema).
2. `02-entrada-tema-claro.png` — entrada (muestra compilada con tema claro).
3. `03-selector-de-carpeta.png` — selector de carpeta abierto.
4. `04-progreso-nativo.png` — página nativa de progreso.
5. `05-exito-nativo.png` — página nativa de éxito.
6. `06-ruta-editable-nombre-largo.png` — ruta editable con nombre largo.
7. `07-app-abierta.png` — la app abierta desde la instalación nueva.

## 5. Flujo de build

```powershell
# Solo el instalador (recompila la app, genera staging y compila Inno Setup)
.\scripts\packaging\Build-Installer.ps1

# Reutilizando la distribucion ya generada por Gradle
.\scripts\packaging\Build-Installer.ps1 -Version 1.0.1 -SkipBuild

# Release oficial (genera el MSI heredado, el EXE Inno, checksums y manifiestos)
.\scripts\release\build-release.ps1
```

`Build-Installer.ps1` deja el instalador en `release\GalleryDL-GUI-Setup-<version>.exe`
y regenera `packaging\windows\version.iss` con la versión real.

> **Punto de integración versionado.** El flujo de release vive en
> `scripts\release\build-release.ps1` porque la ruta histórica
> `scripts\build\build-release.ps1` está **excluida de git** por la regla
> preexistente `**/build/` del `.gitignore` (colisiona con las carpetas de salida
> de Gradle). Comprobado con `git check-ignore -v`:
>
> ```
> IGNORADO   scripts/build/build-release.ps1   <- .gitignore:5:**/build/
> VERSIONADO scripts/release/build-release.ps1
> VERSIONADO scripts/packaging/Build-Installer.ps1
> VERSIONADO scripts/release/ReleaseTools.ps1
> ```
>
> La copia de `scripts\build\build-release.ps1` se dejó tal como estaba; todo el
> trabajo de integración vive en la ruta versionada. El entry point oficial del
> release debe ser `scripts\release\build-release.ps1`.

## 6. Compilador

`D:\Proyectos\biglexj\Scripts\tools\InnoSetup\ISCC.exe` (Inno Setup 6.7.3).
Se puede indicar otra ruta con `-IsccPath` o con la variable de entorno
`INNO_SETUP_ISCC`.
