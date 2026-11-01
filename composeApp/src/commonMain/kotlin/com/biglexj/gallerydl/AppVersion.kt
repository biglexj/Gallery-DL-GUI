package com.biglexj.gallerydl

/**
 * Version informativa de la aplicacion.
 *
 * Es la unica copia del valor dentro del codigo: la usan la pantalla de
 * Ajustes (bloque "Acerca de"), el mensaje de "Buscar actualizaciones" y el
 * endpoint `/status` del protocolo Aurora Synapse.
 *
 * Debe coincidir con `gallerydlgui.versionName` de `gradle.properties`, que es
 * la fuente que consume el empaquetado (jpackage / Inno Setup) y el manifiesto
 * WinGet. El guard de `scripts\release\build-release.ps1` aborta la entrega si
 * las dos se desincronizan.
 */
object AppVersion {
    const val VALUE = "1.0.1"
}
