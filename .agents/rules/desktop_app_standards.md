---
trigger: always_on
---

# Estándares de aplicaciones de escritorio

> [!NOTE]
> Este archivo es un **puente** a la Documentación Core. La norma vive en Core; aquí solo se registran las decisiones propias de Gallery-DL GUI.

Fuentes canónicas:

- [Docs/types/desktop/desktop_app_standards.md](../../../Docs/types/desktop/desktop_app_standards.md): índice de compatibilidad y ubicación actual de cada concepto.
- [Docs/types/desktop/README.md](../../../Docs/types/desktop/README.md): base del tipo de proyecto de escritorio.
- [Docs/features/single-instance/README.md](../../../Docs/features/single-instance/README.md): instancia única y reactivación.
- [Docs/features/system-tray/README.md](../../../Docs/features/system-tray/README.md): bandeja del sistema.
- [Docs/types/desktop/window-state.md](../../../Docs/types/desktop/window-state.md): estado, tamaño y posición de la ventana.

## Decisiones de Gallery-DL GUI

- Gallery-DL GUI **sí** aplica instancia única: permanece residente en la bandeja del sistema mientras hay descargas en curso.
- El bloqueo de instancia tiene **bypass en desarrollo**: la aplicación comprueba `System.getProperty("gallerydlgui.dev") == "true"`, inyectada en los `jvmArgs` de Gradle en `build.gradle.kts`, y devuelve el control sin bloquear ni terminar el proceso. En desarrollo el bloqueo tampoco interfiere con la instalación estable.
