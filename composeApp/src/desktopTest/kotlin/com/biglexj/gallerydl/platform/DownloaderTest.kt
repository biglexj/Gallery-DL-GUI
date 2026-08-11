package com.biglexj.gallerydl.platform

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloaderTest {
    @Test
    fun `prioriza el motor incluido en los recursos de la aplicación`() {
        val resourcesDir = Files.createTempDirectory("gallery-dl-app-resources-").toFile()
        val executable = resourcesDir.resolve("bin/gallery-dl.exe")
        val previousResourcesDir = System.getProperty("compose.application.resources.dir")

        executable.parentFile.mkdirs()
        executable.writeBytes(byteArrayOf(1))

        try {
            System.setProperty("compose.application.resources.dir", resourcesDir.absolutePath)

            assertEquals(executable.absolutePath, Downloader.getExecutablePath())
        } finally {
            if (previousResourcesDir == null) {
                System.clearProperty("compose.application.resources.dir")
            } else {
                System.setProperty("compose.application.resources.dir", previousResourcesDir)
            }
            executable.delete()
            executable.parentFile.delete()
            resourcesDir.delete()
        }
    }
}
