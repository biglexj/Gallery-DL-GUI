# Arquitectura de Gallery-DL GUI

Estructura real del repositorio y ubicación de cada pieza. Este documento sustituye al árbol genérico de Core para stacks web: Gallery-DL GUI es una aplicación de escritorio Kotlin Multiplatform y **no** usa `frontend/`, `backend/` ni `src/` en la raíz.

## Raíz del repositorio

```text
Gallery-DL-GUI/
├── .agents/rules/          # Entrada de instrucciones del agente (base.md + perfiles y referencias)
├── composeApp/             # Módulo Gradle de la aplicación (Kotlin Multiplatform)
├── extension/              # Extensión de navegador Chromium (Manifest V3)
├── packaging/              # Instalador Inno Setup y manifiestos WinGet
├── scripts/                # Scripts de build, empaquetado y release
├── assets/                 # Recursos canónicos de marca (iconos, capturas)
├── bin/                    # Motor gallery-dl usado en desarrollo
├── docs/                   # Documentación técnica del proyecto
├── process/                # Procesos planificados (active / completed / archive)
├── gradle/                 # Wrapper y catálogo de dependencias
├── gradlew, gradlew.bat    # Wrappers técnicos del stack (se conservan)
├── mise.toml               # Interfaz de comandos del proyecto
├── build.gradle.kts        # Plugins y alias de tareas Gradle de nivel raíz
├── settings.gradle.kts     # Includes de módulos Gradle
├── gradle.properties       # Versión de la aplicación y flags de Gradle
├── README.md               # Documentación pública
├── ROADMAP.md              # Hoja de ruta de producto
├── RELEASE_NOTES.md        # Historial técnico por versión
├── RELEASE_MESSAGE.md      # Mensaje público del último lanzamiento
├── NOTICE.md, PRIVACY.md, LICENSE
└── GalleryDLGUI_Dev_Certificate.pfx
```

## Módulo `composeApp`

Código de la aplicación, organizado por destino y por semántica de carpet:

```text
composeApp/src/
├── commonMain/kotlin/com/biglexj/gallerydl/
│   ├── App.kt                      # Composición raíz de la interfaz
│   ├── AppVersion.kt               # Versión visible en pantalla (AppVersion.VALUE)
│   ├── features/
│   │   ├── DownloadFeature/        # Pantalla de descarga, ajustes e historial
│   │   └── synapse/                # Contrato de sobres del protocolo Aurora Synapse
│   ├── shared/components/          # Componentes transversales (ThemeModeButton)
│   ├── theme/                      # Tema Material 3 Expressive
│   └── utils/                      # Utilidades de formato e imagen
├── desktopMain/kotlin/com/biglexj/gallerydl/
│   ├── Main.kt                     # Arranque, estado global y ciclo de vida
│   └── platform/                   # Integración con Windows
│       ├── AppSettings.kt          # Preferencias persistentes
│       ├── DesktopDownloadService.kt
│       ├── DirectoryChooser.kt
│       ├── Downloader.kt           # Localización del motor gallery-dl
│       ├── EngineUpdateService.kt  # Actualización del motor desde Codeberg
│       ├── InterceptionServer.kt   # Servidor local de la extensión
│       ├── ModernTrayManager.kt    # Bandeja del sistema
│       ├── SingleInstanceLock.kt   # Instancia única y reactivación
│       └── synapse/                # URI, cliente saliente y registro de protocolo
└── desktopTest/kotlin/…            # Pruebas de escritorio (JUnit)
```

Convención de nombres: archivos y clases en `PascalCase`, funciones y variables en `camelCase`, constantes en `SCREAMING_SNAKE_CASE`. Los componentes de interfaz se extraen a subcomponentes cuando un archivo se acerca al límite de crecimiento acordado (≈800 líneas preferidas, 1.200 como máximo absoluto).

## Códigos de terceros

- **Motor `gallery-dl`**: se distribuye junto a la aplicación (`bin/gallery-dl.exe` en desarrollo; recursos empaquetados `bin/` en la distribución). Sus licences y atribuciones están en `NOTICE.md`.
- **Plantilla de instalador**: la plantilla visual «Biglex Modern 2.0» y el validador de invariantes viven en `Scripts\tools\inno` del workspace. `packaging/windows/` guarda la copia que el repositorio usa para compilar de forma autocontenida.

## Recursos de marca

`assets/branding/icons/icon-transparent.png` es el icono canónico transparente; `assets/branding/icons/icon.png` es la variante con fondo de marca. Los recursos de Compose y de la extensión son derivados para sus consumidores oficiales y deben seguir el [Asset Organization Standard](../../Docs/global/architecture/asset-organization-standard.md).

## Convenciones de carpetas especiales

- `process/` — trabajos planificados, con ciclo `PLAN.md` → `TASKS.md` → `VALIDATION.md` → `APPROVAL.md`.
- `docs/` — documentación técnica del proyecto (esta carpeta).
- `scripts/` — scripts de implementación, organizados por propósito.
- `temp/`, `scratch/` y `test/` — carpetas de trabajo temporal, ignoradas por Git. `test/` y `plan/` **no existen**: los planes viven en `process/`.
- `release/` — artefactos generados por `mise release`; ignorada por Git.
