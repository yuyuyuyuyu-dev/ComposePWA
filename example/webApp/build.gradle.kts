import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    // Builds this web app as a PWA. Resolved from the local includeBuild (see
    // settings.gradle.kts), so the catalog version is overridden by the local source.
    alias(libs.plugins.composePwa)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)

            implementation(libs.compose.ui)
        }
    }
}