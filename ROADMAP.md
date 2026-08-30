# 🎯 Gallery-DL-GUI — Roadmap

Plan de trabajo, objetivos de producto y hoja de ruta estratégica del proyecto.

> **Regla del roadmap:** El Roadmap reúne la visión general del producto y las ideas por estado. Las tareas activas de desarrollo se gestionan de forma atómica en procesos dentro de `process/active/YYYY-MM-DD_objetivo/`. Una vez aprobado un proceso, se archiva en `process/archive/YYYY/` o `process/completed/YYYY/` y se actualiza esta hoja de ruta marcando los hitos como completados.

---

## 🔴 Pendientes activos

- [x] **v1.0.0** — Versión mayor oficial completada y lista para distribución.

---

## 🟡 Intermedio (Prioridad Media/Baja)

- [ ] Soporte para descarga masiva de listas de URLs desde archivos de texto o portapapeles en lote.
- [ ] Soporte multiplataforma ampliado (Linux / macOS / Android nativo).

---

## ⚪ Descartado / En Pausa

- ⏸️ Migración completa a Tauri (Descartada tras análisis comparativo en favor de Kotlin Multiplatform).

---

## 🟢 Completado

- [x] **v1.0.0** — Primera versión mayor estable:
  - Renovación integral de branding con icono transparente canónico y diseño Material 3 Expressive.
  - Selector de tema tri-estado canónico (Sistema ➔ Claro ➔ Oscuro) con renderizado Canvas vectorial puro.
  - Integración completa con el **Aurora Synapse Protocol**: esquemas `gallerydl://` y `aurora-synapse://gallerydl/`, registro nativo en Windows (HKCU), endpoint `/synapse` y recepción remota LAN para dispositivos móviles (Lienzo Gallery / Android Share).
  - Nomenclatura limpia basada en título de colección con numeración cronológica ascendente (`{title}_{id}.{ext}`).
  - Garantía estricta de instancia única (Single-Instance Lock) con despacho y enfoque en caliente, y soporte de bandeja del sistema (System Tray).
  - Empaquetado en instalador ejecutable `.exe` unificado y conector de navegador Manifest V3 optimizado con iconos multi-resolución.
- [x] **v0.0.2 (Extensión & Bypass Inteligente)** — Conector de navegador Chromium para sincronización de cookies, motor de intercepción para descargas autenticadas (+18) y sincronización con el historial local.
- [x] **v0.0.1 (Lanzamiento inicial)** — Aplanamiento de carpetas, historial con miniaturas y endurecimiento del flujo de descarga. Ver: [2026-08-08_bypass_y_mejoras](file:///d:/Proyectos/biglexj/Gallery-DL-GUI/process/completed/2026/2026-08-08_bypass_y_mejoras/).
- [x] **Preparación local de v0.0.1** — Versión, licencia GPL-2.0, atribución, pruebas automatizadas e instaladores generados.
