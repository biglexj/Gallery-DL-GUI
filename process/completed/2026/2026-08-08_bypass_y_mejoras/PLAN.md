# Aplanamiento de directorios y mejoras de descarga — Plan

- Estado: `APPROVED`
- Fecha: `2026-08-08`
- Proyecto: `Gallery-DL-GUI`

## Objetivo

Implementar el control flexible de estructura de carpetas, un historial persistente con miniaturas y un flujo público de descarga aislado de configuraciones externas.

## Alcance

- **Incluye**:
  - Implementación de selección de estructura de carpetas (Default, CollectionOnly, Flat).
  - Componente de visualización del historial en dos columnas con miniaturas locales dinámicas generadas en tiempo real.
  - Almacenamiento persistente en base a las preferencias locales del usuario.
- **No incluye**:
  - Autenticación de cuentas o contenido privado.

## Enfoque

1. Modificar firmas de servicios y agregar modelos en `DownloadService.kt`.
2. Actualizar `DesktopDownloadService.kt` para integrar la configuración temporal y la recolección de archivos descargados.
3. Crear utilidad multiplataforma de carga de imágenes `ImageLoader.kt` (`expect`/`actual`).
4. Rediseñar `DownloadScreen.kt` para implementar interfaz Material Expressive con doble columna, dropdowns y miniaturas.
5. Inicializar la estructura oficial `process/` y eliminar `TASKS.md` de la raíz del proyecto.

## Criterios de finalización

- [x] Ejecución sin bloqueos del hilo principal de Compose.
- [x] Los ítems del historial muestran miniaturas correctas de la primera imagen descargada.
- [x] Persistencia local del historial a través de reinicios de la aplicación.
- [x] La estructura de directorios sigue estrictamente las reglas oficiales de `Core-Docs`.
- [x] Las URLs en el panel de historial son clickables y abren el navegador nativo del sistema.
- [x] La visualización de la "Carpeta de Guardado" no muestra cursor ni menús de portapapeles.
- [x] Los menús contextuales de texto del sistema heredan correctamente el tema oscuro de la aplicación.
- [x] La estructura de directorios soporta la combinación flexible "Autor / Colección".
- [x] El botón "Abrir" del historial abre dinámicamente la carpeta de destino de descarga real de la colección.

## Autorización

- [x] Plan aprobado para ejecución.
