# Gallery-DL GUI Connector Extension

Esta es la extensión oficial de navegador para interactuar de forma bidireccional con **Gallery-DL-GUI**.

## Características
1. **Descarga Directa**: Envía la URL de la pestaña activa directamente a la cola de descargas de la app desktop.
2. **Sincronización de Cookies (Bypass Automático)**: Sincroniza al instante las cookies de sesión activa (`auth_secure`, `PHPSESSID`, etc.) de tu navegador hacia la aplicación de escritorio. El motor `gallery-dl` las detecta y utiliza de forma 100% transparente para descargar galerías protegidas o con contenido mature.

## Instalación en Chrome / Edge / Brave
1. Abre tu navegador y dirígete a la página de extensiones: `chrome://extensions/`
2. Activa el **Modo de desarrollador** (Developer mode) en el interruptor de la esquina superior derecha.
3. Haz clic en el botón **Cargar descomprimida** (Load unpacked) en la esquina superior izquierda.
4. Selecciona la carpeta `extension` que se encuentra en la raíz de este proyecto (`D:\Proyectos\biglexj\Gallery-DL-GUI\extension`).
5. ¡Listo! La extensión se agregará a tu barra de herramientas.

## Instrucciones de Uso
1. Inicia la aplicación **Gallery-DL-GUI** (esto levantará automáticamente el servidor local en el puerto de escucha `18274`).
2. Navega al sitio del cual desees descargar (por ejemplo, DeviantArt o Pixiv).
3. Abre el popup de la extensión desde la barra del navegador y haz clic en:
   * **Sincronizar Cookies**: Extrae tus credenciales activas del navegador y las almacena en la ruta de bypass del motor (`D:\Documentos\cookies\synced_cookies.txt`). Verás un mensaje en el panel de logs de la aplicación confirmando la sincronización.
   * **Descargar con Gallery-DL GUI**: Captura la URL del sitio y la encola directamente en el campo de texto de la aplicación para iniciar la descarga.
