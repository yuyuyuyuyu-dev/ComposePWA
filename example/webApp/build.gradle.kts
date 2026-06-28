import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    // Builds this web app as a PWA. Resolved from source via includeBuild("../plugin")
    // in settings.gradle.kts, so there is no version and no Gradle Plugin Portal lookup.
    id("dev.yuyuyuyuyu.composepwa")
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