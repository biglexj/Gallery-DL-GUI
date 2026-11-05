# Contratos locales de ejecución

Comportamiento real de Gallery-DL GUI en el equipo del usuario. Estos contratos son exclusivos del producto: no están cubiertos por los módulos de Core y no deben cambiar sin actualizar este documento y las pruebas correspondientes.

## Motor `gallery-dl`

El motor se ejecuta como proceso externo. `Downloader.getExecutablePath()` resuelve el binario en este orden:

1. `%APPDATA%\Gallery-DL-GUI\bin\gallery-dl.exe` — motor actualizado por el usuario. Es la prioridad máxima.
2. Recursos empaquetados en tiempo de ejecución: `<compose.application.resources.dir>/bin/gallery-dl.exe` (instalador Inno Setup y jpackage).
3. Rutas de desarrollo: `bin/`, `../bin/` y `bin/` bajo el directorio de trabajo actual.
4. Último recurso: el `PATH` del sistema.

En Windows el ejecutable es `gallery-dl.exe`; en otros sistemas operativos, `gallery-dl`.

Anulación del directorio del motor: propiedad de sistema `-Dgallerydl.user.engine.dir=<ruta>`.

El motor incluido en la distribución es `gallery-dl 1.31.7` (ver `NOTICE.md`). La versión efectiva se consulta con `gallery-dl --version` y se muestra en Ajustes.

## Actualización del motor

`EngineUpdateService` consulta la API de Codeberg (`https://codeberg.org/api/v1/repos/mikf/gallery-dl/releases/latest`), descarga el `.exe` de la última versión estable y lo deja en el directorio de motor del usuario. Es una **actualización del motor**, no de la aplicación: la app no se autoactualiza desde GitHub Releases.

## Cookies del navegador

- Formato: Netscape (`cookies.txt`), el que `gallery-dl` acepta como `--cookies`.
- Archivo: `synced_cookies.txt`.
- Ubicación preferida: `D:\Documentos\cookies\` si ese directorio existe.
- Ubicación de reserva: `%USERPROFILE%\Documents\cookies\`, creado si hace falta.
- La ruta definitiva se guarda en la preferencia `defaultBrowserCookies` de la aplicación.

La extensión de navegador y el servidor de intercepción escriben en la misma ubicación, de modo que las cookies sincronizadas están disponibles de inmediato para las descargas.

## Servidor de intercepción

- Escucha en `127.0.0.1:18274` (solo loopback).
- Es el destino de la extensión `extension/`: descarga de galerías, descarga directa de imágenes interceptadas y sincronización de cookies.
- `InterceptionServer` usa el mismo directorio de cookies descrito arriba.

## Preferencias e historial

- Raíz de configuración: `%APPDATA%\Gallery-DL-GUI\` (respaldo: `%XDG_CONFIG_HOME%` o `~/.config`).
- Historial de descargas: `%APPDATA%\Gallery-DL-GUI\download-history.json`.
- La preferencia `lastDestination` recuerda el último destino de descarga elegido.

## Destino de las descargas

- Las descargas iniciadas desde la interfaz usan el destino elegido por el usuario y la estructura de carpetas configurada.
- Las capturas de imágenes interceptadas desde la extensión usan, si no hay destino configurado, `%USERPROFILE%\Downloads`, y crean una subcarpeta con el título de la página saneado (caracteres `\ / : * ? " < > |` sustituidos por `_`, máximo 120 caracteres, `Gallery_Images` como reserva).

## Instancia única y bandeja

- `SingleInstanceLock` garantiza una única instancia funcional por usuario y sesión. La segunda invocación entrega su intención (URL, cookies o historial) a la instancia primaria, restaura la ventana, la enfoca y termina con código de éxito.
- `ModernTrayManager` mantiene la aplicación residente en la bandeja: el cierre de ventana minimiza en lugar de terminar, y el menú contextual permite abrir la ventana y salir.
- En desarrollo (`mise dev` o ejecución desde el IDE) el bloqueo no debe interferir con la instalación estable del usuario.

## Protocolo Aurora Synapse

- Esquemas registrados en Windows: `gallerydl://` y `aurora-synapse://gallerydl/`.
- Endpoint local `/synapse` para la recepción de intenciones remotas.
- `SynapseUriParser` interpreta los sobres; `SynapseOutboundClient` reenvía intents a los puertos de Prisma cuando corresponde; `WindowsProtocolRegistrar` mantiene el registro en HKCU.

Los contratos del protocolo están en Core: `Docs/features/aurora-synapse/`.
