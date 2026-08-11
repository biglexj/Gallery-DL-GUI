# 🏛️ Guía Estándar de Arquitectura para Aplicaciones de Escritorio (Desktop Apps)

> **Proyectos de Referencia**: LyraFlow, LunaFetch, Ely-Tesia  
> **Ámbito**: Convenciones de UI/UX, Single-Instance Lock, In-App Auto-Update y Gestión Inteligente de Credenciales para Aplicaciones Desktop (Compose Multiplatform / WPF / Rust).

---

## 1. 🔒 Garantía de Instancia Única (Single Instance Lock) & Condición de Aplicabilidad [CRÍTICO]

> [!IMPORTANT]
> **Condición de Aplicabilidad**: La restricción de instancia única (**Single-Instance Lock**) **APLICA ÚNICAMENTE a aplicaciones que se ejecuten en segundo plano o que mantengan un proceso residente en la bandeja del sistema (System Tray)** (ej. *LunaFetch*, *LyraFlow* con atajos de teclado globales).
> 
> **Excepción de Multi-Instancia en Apps Estándar**: Aplicaciones independientes que NO operan en segundo plano ni en la bandeja (ej. *Ely-Tesia* o editores/visualizadores de archivos) **NO DEBEN aplicar el bloqueo de instancia única**. El usuario debe poder abrir múltiples ventanas e instancias independientes de forma simultánea para comparar o trabajar con diferentes archivos a la vez.

Para prevenir duplicación de procesos e iconos duplicados en la bandeja del sistema (system tray) al relanzar aplicaciones residentes en producción:

- **Mecanismo Obligatorio (Apps de Segundo Plano / Tray)**: La aplicación DEBE adquirir un socket de bucle local (`ServerSocket(127.0.0.1:PORT)`) o un bloqueo exclusivo de archivo (`FileLock`) al iniciar el proceso principal.
- **Bypass en Modo Desarrollo (`isDev`) [OBLIGATORIO Y CRÍTICO]**: El Single-Instance Lock **NUNCA DEBE bloquear ni cerrar la aplicación cuando se ejecuta desde el entorno de desarrollo** (`./gradlew :composeApp:run`, IDE IntelliJ/VSCode o cuando la propiedad del sistema `-D{app}.dev=true` / `idea.active` está presente).
  - La aplicación DEBE detectar el flag `isDev` (por ejemplo, `System.getProperty("lyraflow.dev") == "true"` inyectado en los `jvmArgs` de Gradle en `build.gradle.kts`) y **retornar `true` sin bloquear ni finalizar con `exitProcess(0)`**.
  - Esto garantiza que el desarrollador pueda compilar, probar e interactuar con la versión en desarrollo sin que la versión instalada en Windows cierre o interfiera con la app dev.
- **Comportamiento en Producción**: Si una segunda instancia del ejecutable distribuido (`.exe` / `.msi`) intenta iniciar en producción en una app de segundo plano, detectará la falla al adquirir el bloqueo e **inmediatamente traerá al frente la ventana de la primera instancia activa** (o la desminimizará de la bandeja) y **finalizará la nueva instancia con código 0**.
- **Limpieza de Recursos**: Liberar el socket/bloqueo de forma segura durante el desecho de la aplicación (`DisposableEffect` / `onCloseRequested`).

---

## 2. ⚡ Actualizaciones In-App de Fricción Cero (Patrón LunaFetch Auto-Updater)
Las actualizaciones de versión DEBEN ofrecer una experiencia sin fricción operativa:

- **Verificación Silenciosa**: Comprobación background al iniciar sin interrumpir al usuario.
- **Flujo In-App**:
  1. El usuario hace clic en *"Actualizar ahora"*.
  2. La app descarga el binario ejecutable (`.exe` / `.msi`) en segundo plano mostrando una barra de progreso en tiempo real.
  3. Una vez completada la descarga, la app ofrece el botón *"Instalar y Reiniciar"*.
  4. Al pulsar *"Instalar y Reiniciar"*, la aplicación ejecuta el instalador silencioso en modo pasivo (`/passive` o `/qn`), cierra la versión antigua, actualiza en caliente y **vuelve a abrir la nueva versión automáticamente**.
- **Cero Fricción**: **El usuario NUNCA debe verse obligado a buscar en la carpeta de Descargas, mover archivos o ejecutar manualmente el ejecutable.**

---

## 3. 🧠 Migración Reactiva a Modo Inteligente al Ingresar API Keys
Para garantizar la mejor experiencia con Inteligencia Artificial:

- **Estado Sin API Key / Modo Local**: Por defecto, la app opera en modo offline / literal (*Voz original* / transcripción local) con los modos IA (*Inteligente* / *Personalizado*) bloqueados reactivamente (`🔒`).
- **Auto-migración al ingresar API Key**: Tan pronto como el usuario pega o ingresa su clave de API por primera vez (cambiando de estado vacío a no vacío), la aplicación **migra automáticamente de "Voz original" a "Inteligente"**.
- **Preservación de Preferencias**: Si el usuario cambia manualmente el modo posteriormente, su preferencia se respeta íntegramente.

---

## 4. 🎨 Integración con el Sistema Operativo y Bandeja (System Tray)
- **Minimización a la Bandeja**: El botón de cerrar (`X`) o minimizar debe ocultar la ventana en el tray manteniendo activo el atajo global (*Global Hotkey*).
- **Menú Contextual Nativo**: Menú de bandeja con atajos de teclado, opción para abrir/restaurar ventana y opción de salida definitiva.

---

## 5. 📐 Persistencia Obligatoria del Estado y Dimensiones de la Ventana (Window State Persistence) [CRÍTICO]
Toda aplicación de escritorio DEBE recordar automáticamente su tamaño (ancho, alto), posición en pantalla y estado de maximizado (`isMaximized` / `WindowPlacement.Maximized`) entre sesiones:

- **Restauración al Iniciar**: Al arrancar la aplicación, se leen los valores guardados en la configuración local de usuario (`window_state`). Si el usuario previamente maximizó la ventana o cambió su tamaño, la app **DEBE abrirse exactamente con las mismas dimensiones y estado de maximizado que tenía antes de cerrarse**.
- **Guardado Continuo o al Cerrar**: Al cambiar las dimensiones de la ventana, al maximizar/restaurar o al ejecutar `onCloseRequested`, la aplicación guarda las propiedades de `WindowState` de forma transparente.
- **Prohibición**: Queda strictly prohibido forzar que la ventana se reinicie siempre en un tamaño fijo predeterminado o en modo flotante sin recordar si el usuario la maximizó o personalizó en su sesión previa.

---

## 6. 🎨 Personalización y Localización del Instalador de Escritorio (Custom Installer & Branding) [PLANIFICADO]
Toda aplicación distribuida en Windows (.exe / .msi) DEBE personalizar los recursos del instalador nativo en lugar de mantener las pantallas, textos o licencias genéricas del generador por defecto:

- **Localización al Español**: La interfaz del instalador debe estar configurada en español (`es-PE` / `es-ES`), evitando pantallas de bienvenida, botones o descripciones genéricas en inglés.
- **Branding e Identidad Visual**:
  - Banner e imágenes del instalador con el logotipo oficial del proyecto y la paleta de colores del sistema de diseño.
  - Icono personalizado tanto en el instalador ejecutable como en el panel de desinstalación de Windows (*Programas y características* / *Aplicaciones instaladas*).
  - Nombre de la organización estandarizado (`biglexj`), derechos de autor (`Copyright (c) 2026 Biglex J`) y enlaces de soporte oficial (`https://github.com/biglexj`).
- **Comportamiento Silencioso / Pasivo**: Garantizar que los instaladores ejecutable y MSI soporten los modos `/passive` y `/qn` sin errores para permitir la actualización in-app sin fricción.
