# 🎯 Gallery-DL-GUI — Roadmap

Plan de trabajo, objetivos de producto y hoja de ruta estratégica del proyecto.

> **Regla del roadmap:** El Roadmap reúne la visión general del producto y las ideas por estado. Las tareas activas de desarrollo se gestionan de forma atómica en procesos dentro de `process/active/YYYY-MM-DD_objetivo/`. Una vez aprobado un proceso, se archiva en `process/archive/YYYY/` o `process/completed/YYYY/` y se actualiza esta hoja de ruta marcando los hitos como completados.

---

## 🔴 Pendientes activos

- [ ] **v0.0.3** — Soporte para descargas en segundo plano con bandeja del sistema (System Tray) y notificaciones nativas en Windows.

---

## 🟡 Intermedio (Prioridad Media/Baja)

- [ ] Integración nativa del estado de la extensión de Chrome directamente en los ajustes de la app.
- [ ] Soporte para descarga masiva de listas de URLs desde archivos de texto.
- [ ] Soporte Multiplataforma para Android.

---

## ⚪ Descartado / En Pausa

- ⏸️ Migración completa a Tauri (Descartada tras análisis comparativo en favor de Kotlin Multiplatform).

---

## 🟢 Completado

- [x] **v0.0.2 (Extensión & Bypass Inteligente)** — Conector de navegador Chromium para sincronización de cookies, motor de intercepción para descargas autenticadas (+18) y sincronización con el historial local.
- [x] **v0.0.1 (Lanzamiento inicial)** — Aplanamiento de carpetas, historial con miniaturas y endurecimiento del flujo de descarga. Ver: [2026-08-08_bypass_y_mejoras](file:///d:/Proyectos/biglexj/Gallery-DL-GUI/process/completed/2026/2026-08-08_bypass_y_mejoras/).
- [x] **Preparación local de v0.0.1** — Versión, licencia GPL-2.0, atribución, pruebas automatizadas e instaladores generados.
