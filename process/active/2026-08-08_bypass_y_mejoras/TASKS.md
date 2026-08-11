# Aplanamiento de directorios y mejoras de descarga — Tareas

- Estado: `IMPLEMENTED_PENDING_FUNCTIONAL_VALIDATION`

## Ejecución

- [x] T01 — Refactorizar `DownloadService.kt` y `DesktopDownloadService.kt` para incorporar parámetros de estructura de directorios.
- [x] T03 — Crear e implementar `ImageLoader.kt` (`expect`/`actual`) para cargar imágenes de disco local.
- [x] T04 — Rediseñar `DownloadScreen.kt` a un layout M3 de doble columna con visor de miniaturas del historial y selectores desplegables.
- [x] T05 — Mover la lógica bloqueante del proceso CLI al pool de hilos `Dispatchers.IO`.
- [x] T06 — Eliminar el `TASKS.md` de la raíz del proyecto y estructurar la carpeta `process/`.
- [x] T07 — Implementar callback `onOpenUrl` e interfaz clickeable hipervínculo en el historial de descargas.
- [x] T08 — Migrar la inyección de opciones CLI a generación de archivo de configuración temporal JSON para evitar el stripping de comillas de Windows.
- [x] T09 — Reemplazar el campo `OutlinedTextField` de lectura de directorio por un contenedor Surface personalizado para evitar menús contextuales e inserciones de texto redundantes.
- [x] T10 — Inyectar `DarkDefaultContextMenuRepresentation` en Desktop para forzar que los menús contextuales del portapapeles utilicen estilos oscuros coherentes con la aplicación.
- [x] T11 — Agregar la opción "Autor / Colección" a la estructura de directorios con mapeo de metadatos robusto.
- [x] T13 — Resolver y abrir dinámicamente la subcarpeta real de descargas del historial a partir de los archivos escritos.
- [x] T14 — Capturar la ruta final mediante `--Print {_path}` y persistir el directorio real de salida en el historial.
- [x] T16 — Aislar Gallery-DL de configuraciones globales externas mediante `--config-ignore` y usar el directorio temporal del sistema.
- [x] T17 — Añadir pruebas automatizadas para configuración, resolución de rutas y presentación del historial.
- [x] T18 — Corregir la lectura de rutas omitidas con prefijo `#` y capturar el título exacto de la colección desde Gallery-DL.
- [x] T19 — Mostrar título, ruta real y fecha/hora como los tres datos jerárquicos de cada elemento del historial.
- [x] T21 — Desactivar el botón Descargar mientras la URL esté vacía, sin impedir la cancelación de una descarga activa.
- [x] T22 — Retirar la autenticación importada y simplificar la aplicación a descargas públicas.
