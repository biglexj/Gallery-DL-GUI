# ==============================================================================
# Build-Installer.ps1 - Gallery-DL-GUI
# Genera el instalador de Windows con Inno Setup usando la plantilla compartida
# "Biglex Modern 2.0" (Scripts\tools\inno). El repositorio queda autocontenido:
# packaging\windows\setup.iss, BiglexModernUI.iss, identity.iss y version.iss.
#
# Pasos:
#   1. Regenera packaging\windows\version.iss con la version real.
#   2. Genera la distribucion de la app (app-image de jpackage) y la deja
#      preparada en staging: packaging\windows\app\.
#   3. Compila setup.iss con ISCC.exe y deja el instalador en release\.
#
# Uso:
#   .\scripts\packaging\Build-Installer.ps1
#   .\scripts\packaging\Build-Installer.ps1 -Version 1.0.1 -SkipBuild
# ==============================================================================

param(
    [string]$Version,
    [switch]$SkipBuild,
    [switch]$SkipSigning,
    [string]$OutputDirectory,
    [string]$IsccPath = '',

    # Empaqueta aunque los artefactos sean mas viejos que el codigo fuente.
    # Solo para depurar: publicar asi reintroduce el build obsoleto.
    [switch]$ForceStale
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $root "scripts\release\ReleaseTools.ps1")

$packagingDir = Join-Path $root "packaging\windows"
$stagingDir = Join-Path $packagingDir "app"
$versionIss = Join-Path $packagingDir "version.iss"
$setupIss = Join-Path $packagingDir "setup.iss"
$propertiesPath = Join-Path $root "gradle.properties"

# --- Localizador de ISCC -----------------------------------------------------
# Localiza ISCC.exe sin rutas absolutas: primero la copia del workspace,
# subiendo desde este script; despues el PATH del sistema.
function Resolve-Iscc {
    $dir = $PSScriptRoot
    for ($i = 0; $i -lt 8; $i++) {
        $candidate = Join-Path $dir 'Scripts\tools\InnoSetup\ISCC.exe'
        if (Test-Path -LiteralPath $candidate) { return $candidate }
        $parent = Split-Path -Parent $dir
        if (-not $parent -or $parent -eq $dir) { break }
        $dir = $parent
    }
    $onPath = Get-Command ISCC.exe -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    return $null
}

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Fail {
    param([string]$Message)
    Write-Host "ERROR: $Message" -ForegroundColor Red
    throw $Message
}

# --- Invariantes de empaquetado (validador compartido) ---------------------
# La plantilla visual y el arranque post-instalacion son fuente unica del
# workspace. Si un .iss editado a mano o copiado de otra app los rompe, el
# instalador sale mal, asi que el validador compartido aborta el build.
function Resolve-SharedInnoTool {
    param([string]$Relative)
    $dir = $PSScriptRoot
    for ($i = 0; $i -lt 8; $i++) {
        $candidate = Join-Path $dir $Relative
        if (Test-Path -LiteralPath $candidate) { return $candidate }
        $parent = Split-Path -Parent $dir
        if (-not $parent -or $parent -eq $dir) { break }
        $dir = $parent
    }
    return $null
}

function Assert-PackagingInvariants {
    param([string]$SetupIss, [string]$PackagingDir)

    $sharedDir = Resolve-SharedInnoTool 'Scripts\tools\inno'
    if (-not $sharedDir) {
        Write-Host "AVISO: no se localizo Scripts\tools\inno en este workspace: se omite la verificacion de empaquetado." -ForegroundColor Yellow
        return
    }

    # Plantilla visual con fuente unica: el repo guarda lo que se compilo.
    $sharedUi = Join-Path $sharedDir 'BiglexModernUI.iss'
    $localUi = Join-Path $PackagingDir 'BiglexModernUI.iss'
    if ((Test-Path -LiteralPath $sharedUi) -and (Test-Path -LiteralPath $localUi)) {
        Copy-Item -LiteralPath $sharedUi -Destination $localUi -Force
        Write-Host "Plantilla visual sincronizada con la copia compartida del workspace." -ForegroundColor Gray
    }

    $checker = Join-Path $sharedDir 'Check-Packaging.ps1'
    if (-not (Test-Path -LiteralPath $checker)) {
        Write-Host "AVISO: no se localizo Check-Packaging.ps1: se omite la verificacion de empaquetado." -ForegroundColor Yellow
        return
    }
    $shell = Get-Command pwsh.exe -ErrorAction SilentlyContinue
    if (-not $shell) { $shell = Get-Command powershell.exe -ErrorAction SilentlyContinue }
    if (-not $shell) {
        Write-Host "AVISO: no hay pwsh.exe ni powershell.exe: se omite la verificacion de empaquetado." -ForegroundColor Yellow
        return
    }

    Write-Host "Verificando las invariantes de empaquetado (Check-Packaging.ps1)..." -ForegroundColor Cyan
    & $shell.Source -NoProfile -ExecutionPolicy Bypass -File $checker -SetupIss $SetupIss -Quiet
    if ($LASTEXITCODE -ne 0) {
        Write-Fail ("Las invariantes de empaquetado no se cumplen (Check-Packaging.ps1 devolvio $LASTEXITCODE). " +
                    "Revisa el mensaje del validador de arriba y corrige el setup.iss antes de compilar.")
    }
    Write-Host "Invariantes de empaquetado correctas." -ForegroundColor Green
}

# --- Candado anti-build-obsoleto -------------------------------------------
# Si el artefacto que se empaqueta es mas viejo que el codigo fuente, el
# instalador queda "recien compilado" pero lleva dentro una app vieja.
# Regla de Core: Docs/global/architecture/command-scripts-standard.md,
# principio 2.
# SHA-256 con la API de .NET en lugar de Get-FileHash: ese cmdlet no esta
# disponible en el modulo Microsoft.PowerShell.Utility cuando `powershell` se
# invoca con -NoProfile, que es como lo llama el pipeline de release.
function Get-Sha256Hex {
    param([Parameter(Mandatory)][string]$LiteralPath)
    $algo = [System.Security.Cryptography.SHA256]::Create()
    $stream = $null
    try {
        $stream = [System.IO.File]::OpenRead($LiteralPath)
        return ([BitConverter]::ToString($algo.ComputeHash($stream))).Replace('-', '')
    }
    finally {
        if ($stream) { $stream.Dispose() }
        $algo.Dispose()
    }
}

function Get-NewestWriteTime {
    param([string[]]$Paths)
    $best = $null
    $bestPath = $null
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) { continue }
        $item = if (Test-Path -LiteralPath $path -PathType Container) {
            Get-ChildItem -LiteralPath $path -Recurse -File -Force -ErrorAction SilentlyContinue |
                Sort-Object LastWriteTime -Descending | Select-Object -First 1
        }
        else {
            Get-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
        }
        if ($item -and (-not $best -or $item.LastWriteTime -gt $best)) {
            $best = $item.LastWriteTime
            $bestPath = $item.FullName
        }
    }
    return [pscustomobject]@{ Time = $best; Path = $bestPath }
}

function Assert-BuildFresh {
    param([string[]]$Artifacts, [string]$AppRoot, [string[]]$SourceRoots, [switch]$Force)
    $exeItem = $null
    foreach ($a in $Artifacts) {
        if (Test-Path -LiteralPath $a) {
            $i = Get-Item -LiteralPath $a
            if (-not $exeItem -or $i.LastWriteTime -gt $exeItem.LastWriteTime) { $exeItem = $i }
        }
    }
    if (-not $exeItem) {
        Write-Fail "No se encontro ningun artefacto compilado de: $($Artifacts -join ', ')`n  Compila la app sin el flag de omision."
    }
    $watch = @($SourceRoots)
    $watch += @(
        (Join-Path $AppRoot 'gradle.properties'),
        (Join-Path $AppRoot 'build.gradle.kts'),
        (Join-Path $AppRoot 'settings.gradle.kts')
    )
    $newest = Get-NewestWriteTime -Paths $watch
    if (-not $newest.Time) { return }
    if ($exeItem.LastWriteTime -ge $newest.Time) {
        Write-Step "Artefacto al dia: $($exeItem.LastWriteTime.ToString('dd/MM HH:mm:ss'))  $($exeItem.Name)"
        return
    }
    $rel = if ($newest.Path) { $newest.Path.Substring($AppRoot.Length).TrimStart('\') } else { '(desconocido)' }
    if ($Force) {
        Write-Host "AVISO: se empaqueta un build OBSOLETO por -ForceStale." -ForegroundColor Yellow
        Write-Host "       artefacto $($exeItem.LastWriteTime.ToString('dd/MM HH:mm:ss')) < fuente $($newest.Time.ToString('dd/MM HH:mm:ss')) $rel" -ForegroundColor Yellow
        return
    }
    Write-Fail ("El artefacto a empaquetar es MAS VIEJO que el codigo fuente:`n" +
                "  artefacto : $($exeItem.LastWriteTime.ToString('dd/MM HH:mm:ss'))  $($exeItem.FullName)`n" +
                "  fuente    : $($newest.Time.ToString('dd/MM HH:mm:ss'))  $rel`n`n" +
                "Vuelve a compilar la app (sin el flag de omision), o usa -ForceStale solo para depurar.")
}

if (-not (Test-Path -LiteralPath $setupIss)) {
    throw "No se encontro el script de Inno Setup: $setupIss"
}

# --- Version ----------------------------------------------------------------
$properties = Get-Content -LiteralPath $propertiesPath -Raw -Encoding UTF8
$versionMatch = [regex]::Match($properties, '(?m)^gallerydlgui\.versionName=(\d+\.\d+\.\d+)$')
if (-not $versionMatch.Success) {
    throw "No se pudo leer gallerydlgui.versionName en $propertiesPath."
}
if (-not $Version) { $Version = $versionMatch.Groups[1].Value }
Assert-SemanticVersion $Version
Assert-AppVersionConstant -Root $root -Version $Version

# --- 1) version.iss ---------------------------------------------------------
$versionContent = @"
; ---------------------------------------------------------------------------
; version.iss - generada por el script de build con la version real del app.
; El script de build debe reescribir este archivo en cada release:
;   #define MyAppVersion "1.2.3"
; ---------------------------------------------------------------------------
#define MyAppVersion "$Version"
"@
[IO.File]::WriteAllText($versionIss, ($versionContent -replace "`r`n", "`n"), (New-Object System.Text.UTF8Encoding($true)))
Write-Host "version.iss regenerado con la version $Version" -ForegroundColor Cyan

# --- 2) Distribucion de la app (app-image de jpackage) ----------------------
$appImageRoot = Join-Path $root "composeApp\build\compose\binaries\main\app"

if (-not $SkipBuild) {
    $jdk = Get-FullJdk
    $env:JAVA_HOME = $jdk
    Write-Host "Compilando la distribucion de la app con JDK $jdk" -ForegroundColor Cyan
    Invoke-Checked (Join-Path $root "gradlew.bat") (@("-Dorg.gradle.java.home=$jdk") + @(":composeApp:createDistributable"))
}

if (-not (Test-Path -LiteralPath $appImageRoot)) {
    throw "No se encontro la distribucion de la app en $appImageRoot. Ejecuta el build sin -SkipBuild."
}

# El app-image de jpackage vive en una carpeta con el nombre del paquete.
$appImage = Get-ChildItem -LiteralPath $appImageRoot -Directory |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "app") } |
    Select-Object -First 1
if (-not $appImage) {
    throw "El app-image de jpackage no contiene la carpeta 'app': $appImageRoot"
}
Write-Host "App-image: $($appImage.FullName)" -ForegroundColor Cyan

# Candado anti-build-obsoleto: el app-image se empaqueta tal cual, asi que un
# -SkipBuild sobre codigo nuevo produciria un instalador "nuevo" con la app vieja.
# jpackage nombra el ejecutable como la carpeta del app-image (<Paquete>.exe).
$appExe = Join-Path $appImage.FullName "$($appImage.Name).exe"
if (-not (Test-Path -LiteralPath $appExe)) {
    $foundExe = Get-ChildItem -LiteralPath $appImage.FullName -File -Filter '*.exe' -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($foundExe) { $appExe = $foundExe.FullName }
}
$sourceRoots = @(Join-Path $root "composeApp\src")
Assert-BuildFresh -Artifacts @($appExe) -AppRoot $root -SourceRoots $sourceRoots -Force:$ForceStale

# Staging: el script de Inno toma el payload de packaging\windows\app.
if (Test-Path -LiteralPath $stagingDir) {
    Remove-Item -LiteralPath $stagingDir -Recurse -Force
}
New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null
Copy-Item -Path (Join-Path $appImage.FullName "*") -Destination $stagingDir -Recurse -Force

$stagedFiles = @(Get-ChildItem -LiteralPath $stagingDir -Recurse -File)
if ($stagedFiles.Count -eq 0) {
    throw "El staging quedo vacio: $stagingDir"
}
$stagedBytes = ($stagedFiles | Measure-Object -Property Length -Sum).Sum
Write-Host ("Staging: {0} archivos, {1} MB" -f $stagedFiles.Count, [math]::Round($stagedBytes / 1MB, 1)) -ForegroundColor Cyan

# --- 3) Compilacion con Inno Setup -----------------------------------------
if (-not $IsccPath) { $IsccPath = Resolve-Iscc }
if (-not $IsccPath) { Write-Fail 'No se localizo ISCC.exe en Scripts\tools\InnoSetup ni en el PATH.' }
Write-Host "Compilador Inno Setup: $IsccPath" -ForegroundColor Cyan

if (-not $OutputDirectory) { $OutputDirectory = Join-Path $root "release" }
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$outputDir = [IO.Path]::GetFullPath($OutputDirectory)

Assert-PackagingInvariants -SetupIss $setupIss -PackagingDir $packagingDir

Push-Location $packagingDir
try {
    Invoke-Checked $IsccPath @("/O$outputDir", $setupIss)
} finally {
    Pop-Location
}

$installer = Join-Path $outputDir "GalleryDL-GUI-Setup-$Version.exe"
if (-not (Test-Path -LiteralPath $installer)) {
    $produced = Get-ChildItem -LiteralPath $outputDir -Filter "*Setup-$Version*.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if (-not $produced) {
        throw "ISCC no genero el instalador esperado en $outputDir."
    }
    $installer = $produced.FullName
}

# --- Firma opcional (mismo certificado de desarrollo que el release) -------
if (-not $SkipSigning) {
    $certificate = Join-Path $root "GalleryDLGUI_Dev_Certificate.pfx"
    if (Test-Path -LiteralPath $certificate) {
        $signTool = Get-WindowsSdkTool "signtool.exe"
        Invoke-Checked $signTool @("sign", "/fd", "SHA256", "/p", "biglexj", "/f", $certificate, $installer)
        Assert-SignedArtifact -Path $installer -Publisher "CN=biglexj"
    }
}

$hash = (Get-Sha256Hex -LiteralPath $installer).ToLowerInvariant()
Write-Host ""
Write-Host "Instalador generado: $installer" -ForegroundColor Green
Write-Host ("Tamano: {0} MB" -f [math]::Round((Get-Item -LiteralPath $installer).Length / 1MB, 1))
Write-Host "SHA256: $hash"
Write-Host "Usuario actual, sin elevacion (PrivilegesRequired=lowest)."

# Sincronizar copia en '12. Releases'
$releasesGlobalDir = 'D:\Proyectos\biglexj\12. Releases'
if (Test-Path -LiteralPath $releasesGlobalDir) {
    $installerLeaf = Split-Path $installer -Leaf
    Get-ChildItem -LiteralPath $releasesGlobalDir -Filter "GalleryDL-GUI-Setup-*.exe" -File |
        Where-Object { $_.Name -ne $installerLeaf } |
        ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue }
    Copy-Item -LiteralPath $installer -Destination (Join-Path $releasesGlobalDir $installerLeaf) -Force
    Write-Host "Copia sincronizada en 12. Releases\$installerLeaf" -ForegroundColor Green
}

exit 0
