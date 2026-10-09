import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            kotlin.srcDirs("src/main/kotlin")
            resources.srcDirs("src/main/resources")
            dependencies {
                // The shared module owns all the IDE UI and logic; this module only
                // provides the desktop entry point plus the JVM actual implementations.
                implementation(project(":shared"))

                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "winzzed.komet.ide.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg)
            packageName = "Komet IDE"
            packageVersion = "0.0.9"
            windows {
                menu = true
                menuGroup = "Komet IDE"
                shortcut = true
            }
        }
    }
}
