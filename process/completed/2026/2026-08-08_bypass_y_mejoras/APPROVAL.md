# Aplanamiento de directorios y mejoras de descarga — Aprobación

- Estado: `APPROVED`

## Controles

- [x] Validación técnica del agente.
- [x] Validación funcional del tester.
- [x] Aprobación final de Biglex.

## Decisión

- [x] `APPROVED`
- [ ] `REWORK`

## Resumen

Se ha completado el ciclo de refinamiento funcional de la aplicación:
1. El historial persiste la carpeta real emitida por Gallery-DL y el botón "Abrir" la prioriza sobre la carpeta raíz.
2. Las rutas se capturan antes de la descarga y se validan al finalizar, por lo que también se reconocen archivos nuevos y archivos omitidos por existir previamente.
3. El historial muestra el título exacto de la colección, la ruta real y la fecha/hora con jerarquía tipográfica diferenciada.
4. Se retiró la autenticación importada y el motor queda limitado al acceso público disponible para cada extractor.
5. Se retiró `deviantart.public=false` y se alineó `original=false` con Gallery-DL actual para evitar URLs WixMP defectuosas.
6. Biglex realizará la validación funcional de los estados del botón Descargar y de las descargas públicas compatibles.
