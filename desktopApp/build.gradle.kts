import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin")
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.markdownRenderer)
    implementation(libs.markdownRendererM3)
}

compose.desktop {
    application {
        mainClass = "winzzed.komet.ide.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Komet IDE"
            packageVersion = "1.0.0"
            windows {
                menu = true
                menuGroup = "Komet IDE"
                shortcut = true
            }
        }
    }
}
