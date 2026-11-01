# ==============================================================================
# ReleaseTools.ps1 - Gallery-DL-GUI
# Ecosistema Biglex
# ==============================================================================

Set-StrictMode -Version Latest

function Invoke-Checked {
    param(
        [Parameter(Mandatory)] [string]$Executable,
        [Parameter(Mandatory)] [AllowEmptyString()] [string[]]$ArgumentList
    )

    & $Executable @ArgumentList
    if ($LASTEXITCODE -ne 0) {
        throw "El comando '$Executable' terminó con código de error $LASTEXITCODE."
    }
}

function Get-FullJdk {
    # Se prefieren los JDK 17 instalados por Microsoft: son los que usan
    # jpackage y los que jvmTarget del proyecto (JVM_17) espera.
    $candidates = @()
    $candidates += Get-ChildItem -LiteralPath "C:\Program Files\Microsoft" -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object { $_.FullName }
    $candidates += "C:\Program Files\Java\jdk-17"
    $candidates += $env:JAVA_HOME
    $candidates += "C:\Program Files\Android\Android Studio\jbr"

    $candidates = $candidates | Where-Object {
        $_ -and (Test-Path -LiteralPath (Join-Path $_ "bin\jpackage.exe"))
    }

    $jdk = $candidates | Select-Object -First 1
    if (-not $jdk) {
        throw "No se encontró un JDK completo con jpackage.exe. Configura JAVA_HOME o instala un JDK 17+ completo."
    }
    return $jdk
}

function Get-WindowsSdkTool {
    param([Parameter(Mandatory)] [string]$Name)

    $sdkRoot = "C:\Program Files (x86)\Windows Kits\10\bin"
    if (-not (Test-Path -LiteralPath $sdkRoot)) {
        throw "No se encontró el directorio base de Windows Kits en '$sdkRoot'."
    }

    $sdk = Get-ChildItem -LiteralPath $sdkRoot -Directory |
        Where-Object Name -Match '^10\.' |
        Sort-Object { [version]$_.Name } -Descending |
        Select-Object -First 1
    if (-not $sdk) {
        throw "No se encontró ningún SDK de Windows instalado en '$sdkRoot'."
    }

    $tool = Join-Path $sdk.FullName "x64\$Name"
    if (-not (Test-Path -LiteralPath $tool)) {
        throw "No se encontró la herramienta '$Name' en Windows SDK ('$tool')."
    }
    return $tool
}

function Assert-SemanticVersion {
    param([Parameter(Mandatory)] [string]$Version)

    if ($Version -notmatch '^(\d+)\.(\d+)\.(\d+)$') {
        throw "La versión '$Version' no cumple el formato semántico estricto mayor.menor.parche (ej. 0.0.1 o 1.0.0)."
    }
}

function Assert-AppVersionConstant {
    # La version mostrada en Acerca de/Ajustes y en el endpoint /status sale de
    # AppVersion.VALUE (composeApp\src\commonMain\...\AppVersion.kt). Si se
    # desincroniza de gradle.properties, el binario announcing una version
    # distinta de la que se empaqueta y publica.
    param(
        [Parameter(Mandatory)] [string]$Root,
        [Parameter(Mandatory)] [string]$Version
    )

    $constantFile = Join-Path $Root "composeApp\src\commonMain\kotlin\com\biglexj\gallerydl\AppVersion.kt"
    if (-not (Test-Path -LiteralPath $constantFile)) {
        throw "No se encontró la constante de versión de la app: $constantFile"
    }

    $content = Get-Content -LiteralPath $constantFile -Raw -Encoding UTF8
    $match = [regex]::Match($content, 'const\s+val\s+VALUE\s*=\s*"(\d+\.\d+\.\d+)"')
    if (-not $match.Success) {
        throw "No se pudo leer 'const val VALUE' en $constantFile."
    }

    $declared = $match.Groups[1].Value
    if ($declared -ne $Version) {
        throw ("La versión de la app que se muestra en pantalla (AppVersion.VALUE = '$declared') " +
               "no coincide con la versión que se empaqueta ('$Version'). " +
               "Actualiza AppVersion.kt o gradle.properties antes de entregar.")
    }
}

function Assert-PublishPreflight {
    param(
        [Parameter(Mandatory)] [string]$Root,
        [Parameter(Mandatory)] [string]$Repository,
        [Parameter(Mandatory)] [string]$Tag
    )

    Push-Location $Root
    try {
        $branch = (& git branch --show-current).Trim()
        if ($LASTEXITCODE -ne 0 -or $branch -ne "main") {
            throw "La publicación oficial exige estar en la rama 'main' (rama actual: '$branch')."
        }

        $origin = (& git remote get-url origin).Trim()
        if ($LASTEXITCODE -ne 0 -or $origin -notmatch "github\.com[/:]$([regex]::Escape($Repository))(\.git)?$") {
            throw "El remoto origin ('$origin') no corresponde al repositorio esperado '${Repository}'."
        }

        Invoke-Checked git @("fetch", "origin", "main", "--tags")
        if ((& git rev-parse HEAD).Trim() -ne (& git rev-parse origin/main).Trim()) {
            throw "La rama 'main' local no está sincronizada con 'origin/main'. Realiza git pull/push antes de publicar."
        }

        Invoke-Checked gh @("auth", "status")

        $oldPreference = $ErrorActionPreference
        $ErrorActionPreference = "SilentlyContinue"

        & git rev-parse --quiet --verify "refs/tags/$Tag" *> $null
        $tagExists = ($LASTEXITCODE -eq 0)

        & gh release view $Tag --repo $Repository *> $null
        $releaseExists = ($LASTEXITCODE -eq 0)

        $ErrorActionPreference = $oldPreference

        if ($tagExists) {
            throw "El tag '$Tag' ya existe local o remotamente."
        }
        if ($releaseExists) {
            throw "El GitHub Release '$Tag' ya existe en '${Repository}'."
        }
    } finally {
        Pop-Location
    }
}

function Assert-SignedArtifact {
    param(
        [Parameter(Mandatory)] [string]$Path,
        [Parameter(Mandatory)] [string]$Publisher
    )

    $signature = Get-AuthenticodeSignature -LiteralPath $Path
    if (-not $signature.SignerCertificate -or $signature.SignerCertificate.Subject -ne $Publisher) {
        throw "La firma digital de '$Path' no es válida o no corresponde al firmante esperado '$Publisher'."
    }
}
