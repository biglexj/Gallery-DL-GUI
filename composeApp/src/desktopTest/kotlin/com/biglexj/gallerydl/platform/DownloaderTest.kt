package com.biglexj.gallerydl.platform

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloaderTest {
    @Test
    fun `prioriza el motor actualizado del usuario sobre los recursos empaquetados`() {
        val userEngineDir = Files.createTempDirectory("gallery-dl-user-engine-").toFile()
        val userExe = userEngineDir.resolve(Downloader.getExecutableName())
        userExe.writeBytes(byteArrayOf(1))

        val resourcesDir = Files.createTempDirectory("gallery-dl-app-resources-").toFile()
        val resourcesExe = resourcesDir.resolve("bin/${Downloader.getExecutableName()}")
        resourcesExe.parentFile.mkdirs()
        resourcesExe.writeBytes(byteArrayOf(2))

        val prevUserProp = System.getProperty("gallerydl.user.engine.dir")
        val prevResProp = System.getProperty("compose.application.resources.dir")

        try {
            System.setProperty("gallerydl.user.engine.dir", userEngineDir.absolutePath)
            System.setProperty("compose.application.resources.dir", resourcesDir.absolutePath)

            assertEquals(userExe.absolutePath, Downloader.getExecutablePath())
        } finally {
            if (prevUserProp == null) System.clearProperty("gallerydl.user.engine.dir") else System.setProperty("gallerydl.user.engine.dir", prevUserProp)
            if (prevResProp == null) System.clearProperty("compose.application.resources.dir") else System.setProperty("compose.application.resources.dir", prevResProp)

            userExe.delete()
            userEngineDir.delete()
            resourcesExe.delete()
            resourcesExe.parentFile.delete()
            resourcesDir.delete()
        }
    }

    @Test
    fun `prioriza el motor incluido en los recursos de la aplicación cuando no hay motor de usuario`() {
        val emptyUserDir = Files.createTempDirectory("gallery-dl-empty-user-").toFile()
        val resourcesDir = Files.createTempDirectory("gallery-dl-app-resources-").toFile()
        val executable = resourcesDir.resolve("bin/${Downloader.getExecutableName()}")
        val previousUserDir = System.getProperty("gallerydl.user.engine.dir")
        val previousResourcesDir = System.getProperty("compose.application.resources.dir")

        executable.parentFile.mkdirs()
        executable.writeBytes(byteArrayOf(1))

        try {
            System.setProperty("gallerydl.user.engine.dir", emptyUserDir.absolutePath)
            System.setProperty("compose.application.resources.dir", resourcesDir.absolutePath)

            assertEquals(executable.absolutePath, Downloader.getExecutablePath())
        } finally {
            if (previousUserDir == null) {
                System.clearProperty("gallerydl.user.engine.dir")
            } else {
                System.setProperty("gallerydl.user.engine.dir", previousUserDir)
            }
            if (previousResourcesDir == null) {
                System.clearProperty("compose.application.resources.dir")
            } else {
                System.setProperty("compose.application.resources.dir", previousResourcesDir)
            }
            emptyUserDir.delete()
            executable.delete()
            executable.parentFile.delete()
            resourcesDir.delete()
        }
    }

    @Test
    fun `compara versiones semanticas correctamente para actualizaciones de motor`() {
        assertTrue(EngineUpdateService.isNewerVersion("1.31.7", "1.32.19"))
        assertTrue(EngineUpdateService.isNewerVersion("v1.31.7", "v1.32.19"))
        assertTrue(EngineUpdateService.isNewerVersion("1.32.17", "1.32.19"))
        assertFalse(EngineUpdateService.isNewerVersion("1.32.19", "1.32.19"))
        assertFalse(EngineUpdateService.isNewerVersion("1.32.19", "1.31.7"))
        assertFalse(EngineUpdateService.isNewerVersion("2.0.0", "1.32.19"))
        assertTrue(EngineUpdateService.isNewerVersion("Desconocida", "1.32.19"))
    }
}
