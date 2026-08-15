# Notas de lanzamiento de Gallery-DL-GUI

## [0.0.2] - 2026-08-15

Incorporación de la extensión de navegador oficial (Gallery-DL GUI Connector) para Google Chrome y navegadores Chromium, permitiendo enviar galerías completas a la aplicación de escritorio y sincronizar cookies de sesión activa con un solo clic sin configuraciones manuales.

Nuevo motor de intercepción inteligente para descarga de ilustraciones de máxima resolución, el cual captura de manera transparente los enlaces de entrega autenticados desde el navegador, resolviendo de forma definitiva el acceso a obras con restricción de edad y galerías protegidas.

Sincronización automática de todas las descargas interceptadas con el historial local de la aplicación, incorporando vistas previas en miniatura, organización por carpetas de colección y accesos directos tanto al explorador de archivos como a la web de origen.

Mejoras integrales en el rendimiento y estabilidad de arranque de la aplicación de escritorio, optimizando los tiempos de inicialización de la ventana gráfica y estableciendo un canal de comunicación local seguro y ligero.

---

## [0.0.1] - 2026-08-11

Primera versión pública de Gallery-DL-GUI para Windows. Incluye una interfaz de escritorio para ejecutar descargas públicas mediante `gallery-dl`, elegir el destino y la estructura de carpetas, supervisar el progreso y conservar un historial local con rutas y miniaturas.

La aplicación se distribuye bajo GPL-2.0 y reconoce expresamente a Mike Fährmann y a los colaboradores de `gallery-dl`. El lanzamiento debe incluir la licencia, los avisos de terceros y el acceso al código fuente correspondiente de `gallery-dl 1.31.7`.

La autenticación importada fue retirada de esta versión después de comprobar que no ofrecía resultados fiables en los sitios evaluados. La compatibilidad queda limitada al acceso público que proporcione cada extractor.
