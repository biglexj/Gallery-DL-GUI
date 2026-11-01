---
trigger: always_on
---

# Perfil de Documentación Core — Gallery-DL-GUI

- Última revisión: 2026-08-17
- Tipo principal: `types/desktop` (Aplicación de Escritorio)
- Plataformas: `platforms/windows`
- Stack: `stacks/kotlin` (Kotlin Multiplatform + Compose Desktop JVM)
- Funciones activas: `features/single-instance`, `features/system-tray`, `features/aurora-synapse`, `features/design-system`

## Regla de selección

Antes de aplicar la Documentación Core, consultar únicamente:

1. Las reglas globales pertinentes (`global/architecture`, `global/design`, `global/documentation`).
2. El tipo principal en `Docs/types/desktop`.
3. La plataforma en `Docs/platforms/windows`.
4. El stack utilizado en `Docs/stacks/kotlin`.
5. Las funciones realmente adoptadas: `features/single-instance`, `features/system-tray`, `features/aurora-synapse`, `features/design-system`.

## Documentos Core seleccionados

- `Docs/types/desktop/desktop_app_standards.md`
- `Docs/types/desktop/window-state.md`
- `Docs/features/single-instance/README.md`
- `Docs/features/system-tray/README.md`
- `Docs/features/aurora-synapse/README.md`
- `Docs/features/aurora-synapse/uri-specification.md`
- `Docs/features/aurora-synapse/single-instance-dispatch.md`
- `Docs/features/aurora-synapse/payload-contracts.md`
- `Docs/features/design-system/theme-unification.md`
- `Docs/troubleshooting/stacks/kotlin/compose_desktop_window_render_lifecycle_lock.md`

## Excepciones locales

- Punto 5 de Temas Comunitarios (`.elytheme.json`) descartado por no aplicar a herramientas de descarga directa de galerías.
