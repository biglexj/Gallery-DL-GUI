# Integración de Prisma (Aurora Synapse Outbound) & Rediseño UI — Plan

- Estado: `IN_PROGRESS`
- Fecha: `2026-08-17`
- Proyecto: `Gallery-DL-GUI`

## Objetivo

Implementar la capacidad emisora (Outbound) de Aurora Synapse hacia Prisma y adaptar la interfaz de usuario con Material 3 Expressive para permitir la apertura inmediata de imágenes, galerías y medios descargados en el reproductor/visor oficial del ecosistema.

## Alcance

- Incluye:
  - Definición del contrato y cliente emisor `SynapseOutboundClient` (puerto IPC `49280`, esquema URI `prisma://open` y fallback nativo).
  - Integración en la interfaz `DownloadScreen` (botón de acción rápida «▶ Prisma» en cada fila de historial, acción de clic en miniaturas y chip de apertura al completar descarga).
  - Cableado desde `Main.kt` y `App.kt`.
- No incluye:
  - Sistema de temas comunitarios `.elytheme.json` (descartado explícitamente).

## Enfoque

1. Crear `SynapseEnvelope` y `SynapseOutboundClient` en `composeApp/src/desktopMain/kotlin/com/biglexj/gallerydl/platform/synapse/`.
2. Actualizar `Main.kt` y `App.kt` para propagar el manejador `onOpenInPrisma`.
3. Actualizar `DownloadScreen.kt` integrando el botón «▶ Prisma» en `HistoryItemRow`, clic en miniatura y acción rápida tras descarga completada con diseño Material 3 Expressive.
4. Compilar y validar el funcionamiento correcto de la UI y los despachos.

## Criterios de finalización

- [ ] `SynapseOutboundClient` envía el payload IPC `open_media` a Prisma (puerto 49280) o URI `prisma://open?path=...` con fallback seguro.
- [ ] La UI en `HistoryItemRow` muestra el botón «▶ Prisma» estilizado y responsivo.
- [ ] Al pulsar la miniatura o el botón de Prisma, se despacha el medio descargado.
- [ ] El proyecto compila sin advertencias ni errores (`./gradlew compileKotlinDesktop`).

## Autorización

- [x] Plan aprobado para ejecución.
