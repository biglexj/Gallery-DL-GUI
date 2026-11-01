# ==============================================================================
# build-release.ps1 - Gallery-DL-GUI
# Script Oficial de Compilación, Empaquetado y Publicación de Release
# Ecosistema Biglex (basado en la plantilla oficial de Core-Docs)
# ------------------------------------------------------------------------------
# COPIA VERSIONADA del flujo de release.
#
# La ruta histórica scripts\build\build-release.ps1 está EXCLUIDA de git por la
# regla preexistente "**/build/" del .gitignore (collide con las carpetas de
# salida de Gradle). Por eso este script vive en scripts\release\, que sí está
# versionado: es el punto de integración real y el único que sobrevive a un
# commit.
#
# Novedad de esta copia respecto a la de scripts\build\: integra el instalador
# Inno Setup (Biglex Modern 2.0) generado por scripts\packaging\Build-Installer.ps1
# —artefacto, checksums, assets de GitHub Releases y manifiesto WinGet con
# InstallerType "inno"—. Ver packaging\windows\MIGRATION.md.
#
# Uso:
#   .\scripts\release\build-release.ps1
#   .\scripts\release\build-release.ps1 -Version 1.0.1 -LocalOnly
# ==============================================================================

param(
    [string]$Version,
    [string]$ReleaseNotesFile = "RELEASE_MESSAGE.md",
    [switch]$LocalOnly,
    [switch]$SkipTests,
    [switch]$SkipBuild,
    [switch]$SkipSigning
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $root "scripts\release\ReleaseTools.ps1")

$repository = "biglexj/Gallery-DL-GUI"
$appName = "GalleryDL-GUI"
$propertiesPath = Join-Path $root "gradle.properties"
$releaseNotesPath = Join-Path $root $ReleaseNotesFile

if (-not (Test-Path -LiteralPath $propertiesPath)) {
    throw "No se encontró el archivo de propiedades: $propertiesPath"
}

$properties = Get-Content -LiteralPath $propertiesPath -Raw -Encoding UTF8
$versionMatch = [regex]::Match($properties, '(?m)^gallerydlgui\.versionName=(\d+\.\d+\.\d+)$')
$codeMatch = [regex]::Match($properties, '(?m)^gallerydlgui\.versionCode=(\d+)$')

if (-not $versionMatch.Success -or -not $codeMatch.Success) {
    throw "No se pudo leer la versión/código en $propertiesPath."
}

$currentVersion = $versionMatch.Groups[1].Value
$newCode = [int]$codeMatch.Groups[1].Value

if (-not $Version) { $Version = $currentVersion }
Assert-SemanticVersion $Version

if (-not (Test-Path -LiteralPath $releaseNotesPath)) {
    throw "No se encontró el archivo de notas de lanzamiento: $releaseNotesPath"
}
if (-not $LocalOnly -and $SkipSigning) {
    throw "Una publicación oficial remota no puede usar -SkipSigning."
}

$tag = "v$Version"
$output = Join-Path $root "release"
if (-not $LocalOnly) { Assert-PublishPreflight -Root $root -Repository $repository -Tag $tag }

# Actualizar propiedades si cambia la versión
if ($Version -ne $currentVersion) {
    $newCode++
    $properties = $properties `
        -replace '(?m)^(gallerydlgui\.versionName=)\d+\.\d+\.\d+$', "`${1}$Version" `
        -replace '(?m)^(gallerydlgui\.versionCode=)\d+$', "`${1}$newCode"
    Set-Content -LiteralPath $propertiesPath -Value $properties -Encoding UTF8 -NoNewline
}

# La version visible en la app (Acerca de/Ajustes y endpoint /status) debe
# coincidir con la que se empaqueta y se publica.
Assert-AppVersionConstant -Root $root -Version $Version

$jdk = Get-FullJdk
$signTool = if ($SkipSigning) { $null } else { Get-WindowsSdkTool "signtool.exe" }
$env:JAVA_HOME = $jdk

# Compilación con Gradle (Distribución unificada en un solo binario .exe)
if (-not $SkipBuild) {
    $tasks = @(
        ":composeApp:createDistributable",
        ":composeApp:packageExe"
    )
    if (-not $SkipTests) { $tasks = @(":composeApp:desktopTest") + $tasks }
    Invoke-Checked (Join-Path $root "gradlew.bat") (@("-Dorg.gradle.java.home=$jdk") + $tasks)
}

# Limpieza del directorio release/
New-Item -ItemType Directory -Path $output -Force | Out-Null
$resolvedRoot = [IO.Path]::GetFullPath($root).TrimEnd('\') + '\'
$resolvedOutput = [IO.Path]::GetFullPath($output).TrimEnd('\') + '\'
if (-not $resolvedOutput.StartsWith($resolvedRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw "La salida resuelta quedó fuera del directorio del proyecto."
}

try {
    Get-ChildItem -LiteralPath $output -File -ErrorAction SilentlyContinue | Remove-Item -Force
} catch [System.UnauthorizedAccessException] {
    $lockedPath = if ($_.Exception.PSObject.Properties['ItemName']) { $_.Exception.ItemName } else { $_.Exception.Message }
    $lockingProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object { $_.ExecutablePath -and $_.ExecutablePath.Equals($lockedPath, [StringComparison]::OrdinalIgnoreCase) } |
        ForEach-Object { "$($_.Name) (PID $($_.ProcessId))" }
    $lockDescription = if ($lockingProcesses) { $lockingProcesses -join ", " } else { "un proceso externo" }
    throw "No se puede limpiar '$lockedPath': está bloqueado por $lockDescription. Cierra la aplicación e reintenta."
}

# Copiar binario único ejecutable
$exe = Join-Path $output "$appName-Windows-$Version.exe"
Copy-Item -LiteralPath (Join-Path $root "composeApp\build\compose\binaries\main\exe\$appName-$Version.exe") -Destination $exe
(Get-Item -LiteralPath $exe).IsReadOnly = $false

# Firma Authenticode si existe certificado
if (-not $SkipSigning) {
    $certificate = Join-Path $root "GalleryDLGUI_Dev_Certificate.pfx"
    if (Test-Path -LiteralPath $certificate) {
        Invoke-Checked $signTool @("sign", "/fd", "SHA256", "/p", "biglexj", "/f", $certificate, $exe)
        Assert-SignedArtifact -Path $exe -Publisher "CN=biglexj"
    }
}

# Instalador moderno Inno Setup (plantilla Biglex Modern 2.0).
# Reutiliza la distribucion ya generada por Gradle (app-image de jpackage).
# Se invoca con parametros CON NOMBRE: Invoke-Checked solo acepta listas
# posicionales (splat de array), y "-Version" llegaria como valor de $Version.
$installer = Join-Path $output "$appName-Setup-$Version.exe"
$installerScript = Join-Path $root "scripts\packaging\Build-Installer.ps1"
$installerParams = @{
    Version         = $Version
    OutputDirectory = $output
    SkipBuild       = $true
}
if ($SkipSigning) { $installerParams["SkipSigning"] = $true }
& $installerScript @installerParams
if ($LASTEXITCODE -ne 0) {
    throw "El build del instalador Inno Setup termino con codigo de error $LASTEXITCODE."
}

# Verificar artefacto final
$publishedArtifacts = @($exe, $installer)
foreach ($artifact in $publishedArtifacts) {
    if (-not (Test-Path -LiteralPath $artifact) -or (Get-Item -LiteralPath $artifact).Length -eq 0) {
        throw "El artefacto final no existe o está vacío: $artifact"
    }
}

# Generar checksums SHA256
# Se escribe como UTF-8 sin BOM y con saltos LF (no CRLF): el archivo se
# publica como asset y se verifica con `sha256sum -c`, que con CRLF falla con
# "No such file or directory" porque el \r se cuela en el nombre. El formato
# (hash en minusculas, dos espacios, nombre) es el estandar de GNU coreutils.
$hashPath = Join-Path $output "SHA256SUMS.txt"
$hashLines = @(
    $publishedArtifacts |
        ForEach-Object { Get-FileHash -LiteralPath $_ -Algorithm SHA256 } |
        ForEach-Object { "{0}  {1}" -f $_.Hash.ToLowerInvariant(), (Split-Path $_.Path -Leaf) }
)
[IO.File]::WriteAllText(
    $hashPath,
    (($hashLines -join "`n") + "`n"),
    (New-Object System.Text.UTF8Encoding($false))
)

if ($LocalOnly) {
    Write-Host "Build local completado exitosamente en: $output" -ForegroundColor Green
    exit 0
}

# Actualizar y Validar Manifiesto WinGet si aplica
if (Test-Path -LiteralPath (Join-Path $root "packaging\winget")) {
    $wingetManifestDirectory = Join-Path $root "packaging\winget\manifests\b\biglexj\GalleryDLGUI\$Version"
    New-Item -ItemType Directory -Path $wingetManifestDirectory -Force | Out-Null
    $sha256 = (Get-FileHash -LiteralPath $installer -Algorithm SHA256).Hash.ToUpperInvariant()
    $releaseDate = (Get-Date).ToString("yyyy-MM-dd")

    @"
# yaml-language-server: `$schema=https://aka.ms/winget-manifest.version.1.10.0.schema.json
PackageIdentifier: biglexj.GalleryDLGUI
PackageVersion: $Version
DefaultLocale: es-PE
ManifestType: version
ManifestVersion: 1.10.0
"@ | Set-Content -LiteralPath (Join-Path $wingetManifestDirectory "biglexj.GalleryDLGUI.yaml") -Encoding UTF8 -NoNewline

    @"
# yaml-language-server: `$schema=https://aka.ms/winget-manifest.installer.1.10.0.schema.json
PackageIdentifier: biglexj.GalleryDLGUI
PackageVersion: $Version
# Instalador Inno Setup (Biglex Modern 2.0). El tipo "inno" del esquema de
# WinGet permite que el agente aplique los interruptores propios de Inno
# Setup; el tipo generico "exe" tambien es valido pero exige declarar los
# interruptores a mano. Ver packaging\windows\MIGRATION.md.
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
ReleaseDate: $releaseDate
Installers:
  - Architecture: x64
    InstallerUrl: https://github.com/biglexj/Gallery-DL-GUI/releases/download/v$Version/$appName-Setup-$Version.exe
    InstallerSha256: $sha256
ManifestType: installer
ManifestVersion: 1.10.0
"@ | Set-Content -LiteralPath (Join-Path $wingetManifestDirectory "biglexj.GalleryDLGUI.installer.yaml") -Encoding UTF8 -NoNewline

    @"
# yaml-language-server: `$schema=https://aka.ms/winget-manifest.defaultLocale.1.10.0.schema.json
PackageIdentifier: biglexj.GalleryDLGUI
PackageVersion: $Version
PackageLocale: es-PE
Publisher: biglexj
PublisherUrl: https://github.com/biglexj
PublisherSupportUrl: https://github.com/biglexj/Gallery-DL-GUI/issues
Author: biglexj
PackageName: Gallery-DL GUI
PackageUrl: https://github.com/biglexj/Gallery-DL-GUI
License: GNU GPL v2.0
LicenseUrl: https://github.com/biglexj/Gallery-DL-GUI/blob/main/LICENSE
Copyright: Copyright (c) 2026 biglexj
ShortDescription: Interfaz gráfica para descargar galerías de imágenes y multimedia con gallery-dl.
Description: |-
  Gallery-DL GUI permite estructurar, descargar y gestionar galerías públicas mediante gallery-dl
  con selección visual de rutas, monitoreo de progreso e historial local.
Moniker: gallerydlgui
ReleaseNotesUrl: https://github.com/biglexj/Gallery-DL-GUI/releases/tag/v$Version
ManifestType: defaultLocale
ManifestVersion: 1.10.0
"@ | Set-Content -LiteralPath (Join-Path $wingetManifestDirectory "biglexj.GalleryDLGUI.locale.es-PE.yaml") -Encoding UTF8 -NoNewline

    Invoke-Checked winget @("validate", "--manifest", $wingetManifestDirectory, "--ignore-warnings")
}

# Git Commit, Tag y Push Atómico
Push-Location $root
try {
    Invoke-Checked git @("add", "-A")
    if (Test-Path -LiteralPath (Join-Path $root "packaging\winget")) {
        Invoke-Checked git @("add", (Join-Path $root "packaging\winget"))
    }
    Invoke-Checked git @("diff", "--cached", "--check")
    & git diff --cached --quiet
    if ($LASTEXITCODE -ne 0) {
        Invoke-Checked git @("commit", "-m", "release: Gallery-DL GUI $tag")
    }
    Invoke-Checked git @("tag", "-a", $tag, "-m", "Gallery-DL GUI $tag")
    Invoke-Checked git @("push", "--atomic", "origin", "HEAD:main", "refs/tags/$tag")
} finally {
    Pop-Location
}

# Publicar en GitHub Releases
$assets = @($exe, $installer, $hashPath)
Invoke-Checked gh (@("release", "create", $tag) + $assets + @(
    "--repo", $repository,
    "--verify-tag",
    "--title", "Gallery-DL GUI $tag",
    "--notes-file", $releaseNotesPath
))

Write-Host "🎉 Publicación oficial de Gallery-DL GUI $tag completada con éxito." -ForegroundColor Green
