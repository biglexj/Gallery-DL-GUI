; ============================================================================
; setup.iss  -  Instalador de Windows de Gallery DL GUI (Inno Setup 6.6+)
; ----------------------------------------------------------------------------
; Plantilla compartida: Biglex Modern 2.0 (Scripts\tools\inno). Copia local
; autocontenida: este repositorio NO incluye archivos de la carpeta compartida.
; BiglexModernUI.iss, identity.iss y version.iss viven junto a este script.
;
; version.iss lo genera/actualiza scripts\packaging\Build-Installer.ps1 con la
; version real de la app:
;   #define MyAppVersion "1.2.3"
;
; Compilar (lo hace Build-Installer.ps1):
;   & "D:\Proyectos\biglexj\Scripts\tools\InnoSetup\ISCC.exe" setup.iss
;
; Coexistencia con el instalador MSI (jpackage + WiX): ver MIGRATION.md.
; ============================================================================

#include "version.iss"
#include "identity.iss"

[Setup]
; --- IDENTIDAD DE ACTUALIZACION: unica y estable por app -------------------
; AppId propio de Inno Setup. NO es el UpgradeCode del MSI
; ({D7D11F71-2856-4B35-827C-9EE05A0B9A67}): Windows Installer no reconoce
; una actualizacion de MSI a partir de un AppId de Inno. Ver MIGRATION.md.
AppId={{#MyAppId}}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher=Biglex
AppPublisherURL=https://biglexj.com
AppSupportURL=https://biglexj.com
; Ruta por usuario. Distinta de la del MSI ({localappdata}\GalleryDL-GUI),
; de modo que ambas instalaciones pueden coexistir sin pisar archivos.
DefaultDirName={localappdata}\Programs\{#MyAppDirectory}
DefaultGroupName={#MyAppName}
; --- Pantallas: solo la nuestra + progreso + exito --------------------------
DisableWelcomePage=yes
DisableDirPage=yes
DisableProgramGroupPage=yes
DisableReadyPage=yes
OutputDir=Output
OutputBaseFilename={#MyAppDirectory}-Setup-{#MyAppVersion}
SetupIconFile={#MyAppIcon}
UninstallDisplayIcon={app}\{#MyAppExe}
UninstallDisplayName={#MyAppName}
Compression=lzma2/max
SolidCompression=yes
ShowLanguageDialog=no
; --- Identidad visual moderna + tema claro/oscuro automatico (IS 6.6+) ---
WizardStyle=modern {#MyAppTheme}
#if MyAppTheme == "dark"
WizardBackColor=#15191C
WizardImageBackColor=#15191C
#else
WizardBackColor=#F7F9FA
WizardImageBackColor=clWhite
#endif
WizardBackColorDynamicDark=#15191C
WizardImageBackColorDynamicDark=#15191C
; --- Instalacion por usuario (sin privilegios de administrador) -------------
; PrivilegesRequiredOverridesAllowed se deja vacio a proposito: abrir el
; dialogo "Seleccion del Modo de Instalacion" anade una pantalla extra.
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
; --- Comportamiento --------------------------------------------------------
CloseApplications=yes
RestartApplications=no
SetupLogging=yes
VersionInfoDescription={#MyAppName} instalador
VersionInfoProductName={#MyAppName}
VersionInfoCompany=Biglex
; Sin esta linea, Inno deja FileVersion en 0.0.0.0 y el Explorador de Windows
; muestra "0.0.0.0" en la version del archivo, en contraste con
; ProductVersion y con el EXE de jpackage (FileVersion=1.0.1).
VersionInfoVersion={#MyAppVersion}

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Messages]
spanish.FinishedHeadingLabel=Instalación completada
spanish.FinishedLabel={#MyAppName} se instaló correctamente.
spanish.FinishedLabelNoIcons={#MyAppName} se instaló correctamente.
spanish.ClickFinish=Pulsa Finalizar para cerrar el instalador.

[CustomMessages]
spanish.RunApp=Ejecutar {#MyAppName}

[Files]
; Extraído explícitamente por la UI; al principio para evitar descomprimir payload solid.
Source: "{#MyAppIcon}"; Flags: dontcopy
; Distribucion real generada por jpackage (app-image) y preparada en staging
; por scripts\packaging\Build-Installer.ps1 (carpeta packaging\windows\app).
Source: "app\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; El MSI (jpackage) creaba acceso en el menu Inicio y en el escritorio.
; Se conservan con el nombre de la app moderna para no chocar con el del MSI.
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExe}"

[Run]
; Lanza la app via el shell del escritorio: heredar el contexto del instalador
; hace que el proceso falle con 0xc00004bc. explorer.exe vive en {win}, no en {sys}.
Filename: "{win}\explorer.exe"; Parameters: """{app}\{#MyAppExe}"""; Description: "{cm:RunApp}"; Flags: nowait postinstall skipifsilent

; Si ya hay eventos en la app, integrar estas llamadas sin borrar sus hooks.
#include "BiglexModernUI.iss"

[Code]
procedure InitializeWizard();
begin
  ModernInitialize;
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  ModernPageChanged(CurPageID);
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := ModernNextButtonClick(CurPageID);
end;

[Code]
procedure CancelButtonClick(CurPageID: Integer; var Cancel, Confirm: Boolean);
begin
  ModernCancelButtonClick(CurPageID, Cancel, Confirm);
end;
