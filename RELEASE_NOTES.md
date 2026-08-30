# 🚀 Gallery-DL-GUI — Historial de Versiones

📌 **Versión actual: `1.0.0` · Versión mínima requerida: `0.0.1`**

> [!IMPORTANT]
> **Estándar SemVer Flexible (Core-Docs v1.7.0):**
> - Se utiliza SemVer estándar (`MAJOR.MINOR.PATCH`) sin límites artificiales por dígito (segmentos mayores a 9 como `0.0.12` son 100% válidos).
> - Se incrementa `PATCH`, `MINOR` o `MAJOR` según el alcance real del cambio y compatibilidad, sin saltos forzados de versión basados únicamente en alcanzar un dígito 9.

> Las versiones se muestran en orden descendente: la más reciente aparece arriba y la más antigua abajo.

---

## [1.0.0] - 2026-08-30

Primera versión mayor oficial de Gallery-DL GUI, que introduce una renovación integral de identidad visual mediante un icono maestro canónico con canal alfa transparente y empaquetado nativo `.ico` con 10 capas de resolución adaptativa para todas las superficies del sistema operativo Windows.

Nuevo selector de tema tri-estado canónico con renderizado Canvas vectorial nativo en cabecera, permitiendo alternar de forma fluida entre Modo Sistema, Modo Claro y Modo Oscuro, con persistencia automática de preferencias entre sesiones.

Nuevo sistema de nomenclatura limpia para las descargas, el cual extrae automáticamente el título real de cada ilustración como prefijo principal del archivo y reubica el identificador cronológico al final, eliminando prefijos redundantes y garantizando un orden ascendente impecable al visualizar carpetas en el explorador.

Integración completa con el Aurora Synapse Protocol (`gallerydl://` y `aurora-synapse://gallerydl/`), incorporando registro nativo en Windows (HKCU), endpoint `/synapse` y recepción remota LAN para encolar descargas compartidas desde dispositivos móviles (Lienzo Gallery / Android Share Target).

Garantía estricta de instancia única (Single-Instance Lock) con despacho en caliente y soporte de bandeja del sistema (System Tray), complementado con la distribución en un único instalador ejecutable (`.exe`) firmado digitalmente con Authenticode y extensión de navegador Manifest V3 optimizada.

---

## [0.0.2] - 2026-08-17

Nuevo motor de intercepción inteligente para descarga de ilustraciones de máxima resolución, el cual captura de manera transparente los enlaces de entrega autenticados desde el navegador, resolviendo de forma definitiva el acceso a obras con restricción de edad y galerías protegidas.

Sincronización automática de todas las descargas interceptadas con el historial local de la aplicación, incorporando vistas previas en miniatura, organización por carpetas de colección y accesos directos tanto al explorador de archivos como a la web de origen.

Mejoras integrales en el rendimiento y estabilidad de arranque de la aplicación de escritorio, optimizando los tiempos de inicialización de la ventana gráfica y estableciendo un canal de comunicación local seguro y ligero.

---

## [0.0.1] - 2026-08-11

Primera versión pública de Gallery-DL-GUI para Windows. Incluye una interfaz de escritorio para ejecutar descargas públicas mediante `gallery-dl`, elegir el destino y la estructura de carpetas, supervisar el progreso y conservar un historial local con rutas y miniaturas.

La aplicación se distribuye bajo GPL-2.0 y reconoce expresamente a Mike Fährmann y a los colaboradores de `gallery-dl`. El lanzamiento debe incluir la licencia, los avisos de terceros y el acceso al código fuente correspondiente de `gallery-dl 1.31.7`.

La autenticación importada fue retirada de esta versión después de comprobar que no ofrecía resultados fiables en los sitios evaluados. La compatibilidad queda limitada al acceso público que proporcione cada extractor.
