# 💬 Estándar de Centro de Feedback y Reportes

> **Ámbito**: Convenciones para canalizar opiniones, reporte de bugs, solicitudes de mejora y comunicación directa con el usuario en todas las aplicaciones del ecosistema `biglexj` (Desktop, Android, Web).

---

## 1. 🌐 Canal de Feedback (Fase Actual vs Futura)

### 📌 Fase Provisional (Actual) — GitHub Issues Directo
Mientras el Centro de Feedback centralizado esté completando su integración:
- Todas las aplicaciones DEBEN enlazar directamente al repositorio oficial del proyecto en **GitHub Issues** (`https://github.com/biglexj/{PROJECT_NAME}/issues`).
- El botón en la UI debe etiquetarse como *"Enviar Feedback / Reportar Error 💬"*.

### 🚀 Fase Planificada (Futura) — Centro de Feedback (`https://www.biglexj.com/feedback`)
- **Paso de Metadatos vía URL/JSON**:
  Al accionar *"Enviar Feedback"*, la app abrirá `https://www.biglexj.com/feedback` inyectando automáticamente parámetros de contexto para autocompletar el formulario:
  - `app`: Nombre de la aplicación (ej. `LyraFlow`, `LunaFetch`, `ElyTesia`).
  - `version`: Versión actual instalada (ej. `1.1.0`).
  - `os`: Sistema operativo y arquitectura (ej. `Windows 11 x64`, `Android 14`).
  - `type`: Tipo de reporte inicial (`bug` | `mejora` | `otro`).

- **Experiencia de Usuario**:
  El usuario verá su sesión activa e información de la aplicación pre-cargada, permitiéndole adjuntar capturas, descripción detallada y clasificar la sugerencia sin tener que escribir manualmente la versión o sistema operativo.

---

## 2. 📝 Inclusión en la UI ("Acerca de" / Ajustes)
En todas las ventanas *"Acerca de la Aplicación"* y paneles de Ajustes principales, se debe incluir una opción visible para que el usuario pueda enviar su retroalimentación en cualquier momento.
