package com.biglexj.gallerydl.features.DownloadFeature

import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadHistoryPresentationTest {
    @Test
    fun `muestra el título exacto de la colección por encima de la carpeta`() {
        val item = DownloadHistoryItem(
            id = "hist_1",
            url = "https://www.deviantart.com/autor/gallery/123/high-school-dxd-eyecatches-s2",
            destination = "D:\\Imágenes\\Descargas",
            timestamp = 0L,
            status = "Fallido",
            outputDirectory = "D:\\Imágenes\\Descargas\\High School DXD EyeCatches S2",
            collectionTitle = "High School DXD EyeCatches S2"
        )

        assertEquals("High School DXD EyeCatches S2", resolveHistoryTitle(item))
        assertEquals(
            "D:\\Imágenes\\Descargas\\High School DXD EyeCatches S2",
            resolveHistoryOutputPath(item)
        )
    }

    @Test
    fun `no usa Descargas como título cuando falta metadata antigua`() {
        val item = DownloadHistoryItem(
            id = "hist_2",
            url = "https://www.deviantart.com/autor/gallery/123/high-school-dxd-eyecatches-s2",
            destination = "D:\\Imágenes\\Descargas",
            timestamp = 0L,
            status = "Fallido",
            outputDirectory = "D:\\Imágenes\\Descargas"
        )

        assertEquals("High School Dxd Eyecatches S2", resolveHistoryTitle(item))
    }
}
