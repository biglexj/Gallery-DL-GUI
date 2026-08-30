package com.biglexj.gallerydl.platform.synapse

/**
 * Acciones y contratos de datos recibidos a través del Aurora Synapse Protocol.
 * Conforme a `Core-Docs/features/aurora-synapse/payload-contracts.md` y `uri-specification.md`.
 */
sealed class SynapseAction {
    /**
     * Trae la ventana viva de la aplicación al frente y solicita foco.
     */
    object Focus : SynapseAction()

    /**
     * Encola una URL y solicita iniciar la descarga automáticamente.
     */
    data class Download(
        val url: String,
        val destination: String? = null,
        val silent: Boolean = false,
        val source: String? = null
    ) : SynapseAction()

    /**
     * Rellena la URL en la interfaz para preparación/inspección sin iniciar descarga automática inmediata.
     */
    data class Enqueue(
        val url: String,
        val source: String? = null
    ) : SynapseAction()

    /**
     * Abre la carpeta de descargas en el explorador del sistema.
     */
    data class OpenFolder(
        val path: String? = null
    ) : SynapseAction()
}
