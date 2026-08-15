import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.Sync
import java.io.File

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val appVersion = providers.gradleProperty("gallerydlgui.versionName").get()
val appVersionCode = providers.gradleProperty("gallerydlgui.versionCode").get().toInt()
val bundledAppResourcesDir = layout.buildDirectory.dir("bundledAppResources")

val prepareBundledAppResources by tasks.registering(Sync::class) {
    into(bundledAppResourcesDir)

    from(rootProject.file("LICENSE")) {
        into("common")
    }
    from(rootProject.file("NOTICE.md")) {
        into("common")
    }
    from(rootProject.file("bin/gallery-dl.exe")) {
        into("windows/bin")
    }
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
                implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
                implementation("org.jetbrains.compose.material3:material3:1.11.0-alpha07")
                implementation("org.jetbrains.compose.ui:ui:1.11.1")
                implementation("org.jetbrains.compose.components:components-resources:1.11.1")
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.biglexj.gallerydl.MainKt"
        val jpackageJdk: String? = listOf(
            "C:/Program Files/Microsoft/jdk-17.0.19.10-hotspot",
            "C:/Program Files/Java/jdk-17",
            "C:/Program Files/Eclipse Adoptium/jdk-17",
            System.getenv("JAVA_HOME") ?: ""
        ).firstOrNull { it.isNotBlank() && File(it, "bin/jpackage.exe").exists() }

        if (jpackageJdk != null) {
            javaHome = jpackageJdk
        }
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            appResourcesRootDir.set(bundledAppResourcesDir)
            packageName = "GalleryDL-GUI"
            packageVersion = appVersion
            vendor = "biglexj"
            description = "Gallery-DL GUI — Descargador de galerías de imágenes"
            copyright = "Copyright © 2026 biglexj. Licenciado bajo GPL-2.0."
            licenseFile.set(rootProject.file("LICENSE"))

            windows {
                msiPackageVersion = appVersion
                exePackageVersion = appVersion
                upgradeUuid = "d7d11f71-2856-4b35-827c-9ee05a0b9a67"
                shortcut = true
                menu = true
                dirChooser = true
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
            }
        }
    }
}

tasks.configureEach {
    if (name == "prepareAppResources") {
        dependsOn(prepareBundledAppResources)
    }
}
