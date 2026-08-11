package com.biglexj.gallerydl.platform

import com.biglexj.gallerydl.features.DownloadFeature.DirectoryStructure
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopDownloadServiceTest {
    @Test
    fun `la configuración conserva el acceso público de DeviantArt`() {
        val config = buildGalleryDlConfig(
            destination = "D:\\Imágenes\\Descargas",
            extractorKey = "deviantart",
            directoryStructure = DirectoryStructure.CollectionOnly
        )

        assertFalse(config.contains("\"public\""))
        assertFalse(config.contains("\"mature\""))
        assertContains(config, "\"original\":false")
        assertContains(config, "{album|gallery_title|folder[title]|category}")
    }

    @Test
    fun `el comando de DeviantArt no activa opciones de sesión`() {
        val options = buildDeviantartOverrideOptions()

        assertContains(options, "extractor.deviantart.original=false")
        assertFalse(options.any { it.contains("mature") })
        assertFalse(options.any { it.contains("jwt") })
    }

    @Test
    fun `resuelve la ruta final después de que Gallery-DL crea el archivo`() {
        val destination = Files.createTempDirectory("gallery-dl-destination-").toFile()
        val collection = destination.resolve("Mi colección").apply { mkdirs() }
        val downloadedFile = collection.resolve("imagen.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        try {
            val resolved = resolveDownloadedFiles(
                candidates = listOf(downloadedFile.absolutePath),
                destination = destination.absolutePath
            )

            assertEquals(listOf(downloadedFile.canonicalPath), resolved)
        } finally {
            downloadedFile.delete()
            collection.delete()
            destination.delete()
        }
    }

    @Test
    fun `conserva la carpeta creada aunque el archivo haya fallado`() {
        val destination = Files.createTempDirectory("gallery-dl-destination-").toFile()
        val collection = destination.resolve("Mi colección").apply { mkdirs() }
        val failedFile = collection.resolve("imagen-no-descargada.png")

        try {
            val outputDirectory = resolveOutputDirectory(
                downloadedFiles = emptyList(),
                candidates = listOf(failedFile.absolutePath),
                destination = destination.absolutePath
            )

            assertEquals(collection.canonicalPath, outputDirectory)
        } finally {
            collection.delete()
            destination.delete()
        }
    }

    @Test
    fun `interpreta correctamente las rutas omitidas que Gallery-DL prefija con numeral`() {
        val destination = Files.createTempDirectory("gallery-dl-destination-").toFile()
        val collection = destination.resolve("High School DXD EyeCatches S2").apply { mkdirs() }
        val downloadedFile = collection.resolve("imagen.png").apply { writeBytes(byteArrayOf(1)) }

        try {
            val resolved = resolveDownloadedFiles(
                candidates = listOf("# ${downloadedFile.absolutePath}"),
                destination = destination.absolutePath
            )

            assertEquals(listOf(downloadedFile.canonicalPath), resolved)
        } finally {
            downloadedFile.delete()
            collection.delete()
            destination.delete()
        }
    }
}
