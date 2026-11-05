---
trigger: always_on
---

# Auto-actualización y descargas

> [!NOTE]
> Este archivo es un **puente** a la Documentación Core. La norma vive en Core; aquí solo se registra lo propio de Gallery-DL GUI.

Fuentes canónicas:

- [Docs/features/auto-updater/auto_updater.md](../../../Docs/features/auto-updater/auto_updater.md): requisitos de comprobación, descarga e instalación directa de actualizaciones.
- [Docs/features/auto-updater/desktop-update-flow.md](../../../Docs/features/auto-updater/desktop-update-flow.md): flujo de escritorio.
- [Docs/features/auto-updater/github_auto_updater_guide.md](../../../Docs/features/auto-updater/github_auto_updater_guide.md): guía de descarga directa.

## Decisiones de Gallery-DL GUI

- **El motor `gallery-dl` se actualiza por separado** desde Codeberg, no desde GitHub Releases: el binario se verifica por hash y lo actualiza el usuario. La autoactualización de la propia aplicación desde GitHub Releases no está adoptada.
- Las descargas capturadas por la extensión se guardan en el almacenamiento persistente de la aplicación (`filesDir` en escritorio) con barra de progreso visible.
