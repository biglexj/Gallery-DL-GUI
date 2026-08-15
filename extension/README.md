# 🧩 Extensión de Navegador — Gallery-DL GUI

Esta extensión oficial conecta tu navegador (**Google Chrome**, **Microsoft Edge**, **Brave**, **Opera**) con la aplicación de escritorio **Gallery-DL GUI** para interceptar imágenes de alta resolución, descargar galerías completas y sincronizar cookies de sesión (bypass de contenido protegido o con restricción de edad) con un solo clic.

---

## ⚡ Guía de Instalación Rápida (Paso a Paso)

### 1️⃣ Descargar o Descomprimir la Extensión
1. Si descargaste el paquete de lanzamiento:
   - Extrae el archivo **`Gallery-DL-GUI-Extension-vX.X.X.zip`** en una carpeta fija de tu equipo (por ejemplo en `Documentos\Gallery-DL-GUI-Extension`).
2. Si tienes el repositorio del proyecto:
   - La carpeta lista para usar es **`extension`** dentro de la raíz del proyecto.

---

### 2️⃣ Cargar la Extensión en tu Navegador

1. Abre tu navegador web e ingresa a la sección de extensiones según tu navegador:
   * **Google Chrome**: `chrome://extensions`
   * **Microsoft Edge**: `edge://extensions`
   * **Brave**: `brave://extensions`
   * **Opera**: `opera://extensions`

2. En la esquina superior derecha, activa el interruptor **Modo de desarrollador** *(Developer mode)*.

3. Haz clic en el botón **"Cargar descomprimida"** *(Load unpacked)* situado en la barra superior.

4. Selecciona la carpeta **`extension`** (o la carpeta donde descomprimiste el zip).

5. *(Recomendado)*: En el menú de extensiones de la barra de herramientas del navegador (icono de rompecabezas 🧩), fija *(pin)* el icono de **Gallery-DL GUI** para tenerlo siempre a mano.

---

## 🚀 ¿Cómo se usa?

Asegúrate de que la aplicación **Gallery-DL GUI** esté abierta (o minimizada en la bandeja del sistema). El servidor local escucha automáticamente en `127.0.0.1:18274`.

### 📥 1. Descarga Directa de Galerías / Perfiles
1. Navega a cualquier página con galerías (DeviantArt, Pixiv, ArtStation, Twitter/X, etc.).
2. Abre el menú emergente de la extensión y haz clic en **"Descargar con Gallery-DL GUI"**.
3. La URL se enviará automáticamente a la aplicación de escritorio y comenzará la descarga con el motor `gallery-dl`.

### 🔍 2. Interceptación Inteligente de Imágenes en Pantalla
1. En páginas de arte con tokens de sesión o protección (ej. DeviantArt `/art/` o `/gallery/`), haz clic en **"Escanear imágenes de esta página"**.
2. La extensión extraerá las URLs directas de alta definición de las obras visibles con sus tokens de sesión válidos.
3. Podrás previsualizar las miniaturas numeradas y hacer clic en **"Descargar X imágenes interceptadas"** para guardarlas al instante.

### 🔑 3. Sincronización de Cookies (Bypass de Restricción de Edad)
1. Inicia sesión en tu cuenta en el navegador.
2. Abre el menú de la extensión y pulsa **"Sincronizar Cookies"**.
3. Las cookies de sesión se guardan automáticamente en formato Netscape (`synced_cookies.txt`) y la aplicación de escritorio las usará de inmediato para descargar obras para mayores de edad o restringidas.

---

## ❓ Preguntas Frecuentes y Solución de Problemas

* **¿Por qué dice "No se pudo conectar con Gallery-DL GUI"?**
  * Verifica que la aplicación de escritorio **Gallery-DL GUI** esté ejecutándose en tu PC. Si está cerrada, ábrela desde el acceso directo o la bandeja del sistema.
* **¿Qué navegadores son compatibles?**
  * Todos los navegadores basados en Chromium: Google Chrome, Microsoft Edge, Brave, Opera, Vivaldi, Arc.
