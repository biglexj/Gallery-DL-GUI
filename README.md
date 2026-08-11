# Gallery-DL-GUI

Interfaz gráfica de escritorio para Windows construida con Kotlin y Compose Multiplatform. Facilita el uso del motor `gallery-dl` mediante una pantalla visual para iniciar descargas públicas, elegir la carpeta de destino, controlar la estructura de directorios y consultar el historial local.

Versión actual: `0.0.1`

## Estado del proyecto

Esta es una versión inicial. La compatibilidad real depende de los extractores y restricciones de cada sitio en `gallery-dl`. La aplicación no importa sesiones de navegador ni archivos de autenticación; únicamente utiliza el acceso público disponible para cada extractor.

## Funciones principales

- Descarga de imágenes y galerías compatibles con `gallery-dl`.
- Selección de carpeta de destino.
- Estructuras de salida configurables.
- Registro local de resultados, rutas y miniaturas.
- Panel de actividad y cancelación del proceso activo.

## Requisitos de desarrollo

- Windows 10 u 11.
- JDK 17 completo, con `jpackage` incluido.
- PowerShell.

Compilación y pruebas:

```powershell
.\gradlew.bat :composeApp:desktopTest :composeApp:compileKotlinDesktop --console=plain --no-daemon
```

Generación de distribuciones nativas:

```powershell
.\gradlew.bat :composeApp:packageReleaseDistributionForCurrentOS --console=plain --no-daemon
```

Antes de publicar, se debe verificar el instalador resultante en una instalación limpia y comprobar que el motor externo, la licencia y los avisos de terceros estén incluidos. El lanzamiento también debe adjuntar `gallery-dl-1.31.7-source.zip`, o cumplir mediante otro mecanismo válido de la GPL-2.0 con la entrega del código fuente correspondiente.

## Proyecto original y atribución

Gallery-DL-GUI utiliza como motor externo el proyecto libre [`gallery-dl`](https://github.com/mikf/gallery-dl), creado por Mike Fährmann (`mikf`) y sus colaboradores. El desarrollo principal de `gallery-dl` se trasladó a [Codeberg](https://codeberg.org/mikf/gallery-dl).

Esta interfaz es una adaptación independiente y no oficial. No sustituye, representa ni está afiliada con el proyecto original. La versión de Windows incluye actualmente `gallery-dl 1.31.7`; su [código fuente correspondiente](https://github.com/mikf/gallery-dl/tree/v1.31.7) debe acompañar toda publicación binaria según las condiciones de la GPL-2.0.

## Licencia

Gallery-DL-GUI se distribuye bajo la [GNU GPL v2.0](LICENSE). El motor `gallery-dl` también se distribuye bajo GPL-2.0. Consulta [NOTICE.md](NOTICE.md) para conocer la atribución, la versión integrada y los enlaces al código fuente original.

Puedes utilizar, estudiar, modificar y redistribuir este proyecto respetando las condiciones de la GPL-2.0, incluidos los avisos de autoría y la disponibilidad del código fuente correspondiente.

## Uso responsable

Utiliza la aplicación únicamente para descargar contenido que tengas derecho a guardar. Los sitios compatibles pueden modificar sus sistemas, exigir autenticación o bloquear descargas sin previo aviso.
