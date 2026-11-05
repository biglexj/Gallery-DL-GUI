---
trigger: always_on
---

# Gallery-DL GUI — entrada de instrucciones

## Documentación Core — entrada compartida

Consultar primero [Documentación Core](../../../Docs/README.md) y seleccionar solo los módulos pertinentes. Los estándares compartidos se mantienen allí; este archivo conserva el alcance, los contratos y las decisiones exclusivas de Gallery-DL GUI, y no replica copias normativas.

- [Calidad y entrega proporcionales](../../../Docs/global/quality/quality-baseline.md): build del módulo afectado para ajustes visuales pequeños; pruebas del comportamiento incorporado o afectado para elementos, lógica o páginas nuevos y cambios grandes; revisión acotada del diff para documentación.
- [Procesos según alcance](../../../Docs/global/documentation/process-workflow.md): los trabajos planificados viven en `process/active/YYYY-MM-DD_objetivo/` con `PLAN.md`, `TASKS.md`, `VALIDATION.md` y `APPROVAL.md`; no se crea un plan, un archivo de tareas ni una aprobación aparte por rutina en ajustes ya autorizados.
- [Reglas de trabajo del agente](../../../Docs/global/documentation/agent-working-rules.md): entrada única de instrucciones, límite de 12.000 caracteres por archivo de `.agents/rules/`, commits de resguardo y política de ramas.
- [Comandos mediante Mise](../../../Docs/global/architecture/mise-command-standard.md) y [scripts y alias](../../../Docs/global/architecture/command-scripts-standard.md): `dev`, `build`, `check`, `test`, `release`, `publish`; `release` prepara artefactos sin publicar y `publish` publica de forma explícita.
- [Organización de proyectos y carpetas](../../../Docs/global/architecture/project-structure.md) y [lenguaje de diseño](../../../Docs/global/design/design_system.md) con [textos de interfaz](../../../Docs/global/design/interface-writing.md).
- [Soporte oficial y diálogo «Acerca de»](../../../Docs/features/official-support/official-support-baseline.md), [instaladores](../../../Docs/features/installer/README.md) y [plataforma Windows](../../../Docs/platforms/windows/installer.md).
- [Base de lanzamientos](../../../Docs/global/releases/release-baseline.md), [protocolo de lanzamiento y actualización](../../../Docs/global/releases/protocolo_lanzamiento_actualizacion.md) y [mensaje público para la web](../../../Docs/global/releases/web-release-message-standard.md).
- [Diagnósticos reutilizables](../../../Docs/troubleshooting/README.md) y [promoción a Core](../../../Docs/global/documentation/core-promotion-workflow.md): documentar en Core los fallos confirmados o aprendizajes generalizables y enlazarlos desde el proyecto; mantener locales los detalles exclusivos.

Módulos Core aplicables a este proyecto (tipo, plataforma, stack y funciones): [core_profile.md](core_profile.md).

Entrada única de instrucciones: `.agents/rules/base.md`. No se crean `AGENTS.md` ni `agent.md` equivalentes en la raíz. Las skills generales importadas son apoyo técnico, no una autoridad para redefinir estas bases. Ante una contradicción, comprobar la implementación vigente y resolverla explícitamente.

## Alcance del producto

- Aplicación de escritorio Windows en Kotlin Multiplatform + Compose Multiplatform (JVM), módulo Gradle `composeApp`.
- Motor externo `gallery-dl` (GPL-2.0) ejecutado como proceso. La aplicación no importa sesiones ni autenticación, salvo el archivo de cookies que el usuario sincroniza desde el navegador.
- Distribución oficial en Windows: instalador Inno Setup (plantilla Biglex Modern 2.0) y, por compatibilidad con instalaciones previas, el `.exe` de jpackage/WiX. Detalle en [docs/release-flow.md](../../docs/release-flow.md).
- Extensión de navegador Chromium (Manifest V3) en `extension/`: envía descargas, intercepta imágenes y sincroniza cookies con la aplicación. Guía en [extension/README.md](../../extension/README.md).
- Licencia del proyecto: GNU GPL v2.0. Autoría: biglexj (2026). El motor externo conserva su propia autoría y licencia; ver [NOTICE.md](../../NOTICE.md).

## Contratos locales de ejecución

Detalle en [docs/runtime-contracts.md](../../docs/runtime-contracts.md). Resumen operativo:

- **Motor**: `%APPDATA%\Gallery-DL-GUI\bin\gallery-dl.exe` (binario actualizado por el usuario, prioridad máxima) → recursos empaquetados `bin/` → `bin/` del repositorio en desarrollo. Anulación: `-Dgallerydl.user.engine.dir`.
- **Cookies del navegador**: formato Netscape en `synced_cookies.txt`, en `D:\Documentos\cookies\` si existe y, en su defecto, en `%USERPROFILE%\Documents\cookies\`.
- **Servidor de intercepción de la extensión**: `127.0.0.1:18274`.
- **Preferencias e historial**: `%APPDATA%\Gallery-DL-GUI\` (`download-history.json`).
- **Destino por defecto de las descargas capturadas**: `%USERPROFILE%\Downloads`.
- **Instancia única** con reactivación y bandeja del sistema; en desarrollo el bloqueo no debe interferir con la instalación estable.

## Estructura y arquitectura

[docs/architecture.md](../../docs/architecture.md) describe la estructura real del repositorio y dónde vive cada pieza. No aplica la convención genérica de Core para stacks web (`frontend/`, `backend/`, `src/`), que este proyecto no usa.

## Comandos

Mise es la interfaz de comandos del proyecto: `mise dev`, `mise build`, `mise check`, `mise test`, `mise release`, `mise publish`. `gradlew` y `gradlew.bat` son wrappers técnicos del stack y se conservan, pero no son la interfaz del proyecto. Detalle en [README.md](../../README.md).

## Decisiones y excepciones locales

- **Identidad visual**: Material 3 Expressive es el lenguaje de diseño adoptado; el alcance concreto está en [design_system.md](design_system.md).
- **Autoactualización**: la aplicación actualiza el **motor** `gallery-dl` desde Codeberg. La autoactualización de la propia aplicación desde GitHub Releases no está adoptada; [auto_updater.md](auto_updater.md) y [github_auto_updater_guide.md](github_auto_updater_guide.md) son referencias a los módulos de Core, no reglas vigentes del proyecto.
- **Centro de feedback**: no se expone la entrada al feedback central de `biglexj.com`; el soporte se dirige al repositorio de GitHub. Ver [feedback_center.md](feedback_center.md).
- **Soporte oficial**: la sección «Acerca de» expone donación, Buy Me a Coffee y GitHub, conforme a [official-support-baseline.md](../../../Docs/features/official-support/official-support-baseline.md).
- **Versionado**: SemVer sin límite artificial de dígitos (no aplica la regla del `.9` de versiones anteriores del proyecto). La versión visible en pantalla (`AppVersion.VALUE`) debe coincidir con `gradle.properties`; lo verifica `Assert-AppVersionConstant` en `scripts/release/ReleaseTools.ps1`.
- **Hoja de ruta**: los pendientes generales viven en `ROADMAP.md`; el detalle técnico y la trazabilidad, en `process/`.

## Referencias locales especializadas

- [core_profile.md](core_profile.md) — módulos Core aplicables a este proyecto.
- [design_system.md](design_system.md) — identidad visual y alcance del diseño.
- [folder_structure.md](folder_structure.md) — estructura de carpetas del repositorio.
- [desktop_app_standards.md](desktop_app_standards.md) — estándares de aplicaciones de escritorio.
- [auto_updater.md](auto_updater.md) — autoactualización desde GitHub Releases.
- [github_auto_updater_guide.md](github_auto_updater_guide.md) — guía de autoactualización directa.
- [feedback_center.md](feedback_center.md) — centro de feedback y reportes.
