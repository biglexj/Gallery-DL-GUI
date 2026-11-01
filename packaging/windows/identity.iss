; Configuración por aplicación. Mantener MyAppId estable entre versiones.
; Gallery DL GUI — instalador Inno Setup (Biglex Modern 2.0).
#ifndef MyAppName
  #define MyAppName "Gallery DL GUI"
#endif
; AppId propio y estable de Inno Setup. NO reutilizar el UpgradeCode del MSI
; ({D7D11F71-2856-4B35-827C-9EE05A0B9A67}): Windows Installer no reconoce una
; actualización de MSI a partir de un AppId de Inno. Ver MIGRATION.md.
#ifndef MyAppId
  #define MyAppId "{802F319B-753A-4A89-A679-A0CC8AC9B521}"
#endif
; Carpeta de instalación por usuario y nombre de la salida del instalador.
#ifndef MyAppDirectory
  #define MyAppDirectory "GalleryDL-GUI"
#endif
; Lanzador real generado por jpackage dentro del app-image.
#ifndef MyAppExe
  #define MyAppExe "GalleryDL-GUI.exe"
#endif
; Icono oficial transparente de la app (composeApp\src\desktopMain\resources).
#ifndef MyAppIcon
  #define MyAppIcon "icon.ico"
#endif
; Tema automatico: la UI sigue al tema claro/oscuro de Windows al arrancar.
#ifndef MyAppTheme
  #define MyAppTheme "dynamic"
#endif

#ifndef MyAppSubtitle
  #define MyAppSubtitle "Descarga tus imágenes y galerías."
#endif
