import com.vanniktech.maven.publish.GradlePublishPlugin

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.gradlePluginPublish)
    `java-gradle-plugin`
    alias(libs.plugins.vanniktechMavenPublish)
    alias(libs.plugins.nodeGradle)
}

kotlin {
    jvmToolchain(21)
}

group = "dev.yuyuyuyuyu"
version = "0.6.1"

gradlePlugin {
    website = "https://github.com/yu-ko-ba/ComposePWA#readme"
    vcsUrl = "https://github.com/yu-ko-ba/ComposePWA"

    plugins {
        create("Compose PWA") {
            id = "dev.yuyuyuyuyu.composepwa"
            displayName = "Compose PWA"
            description =
                "This Gradle plugin provides a Gradle Task, that build your Compose Multiplatform Web App as a PWA."
            tags = listOf("compose multiplatform", "web", "pwa")
            implementationClass = "dev.yuyuyuyuyu.ComposePwa"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(localGroovy())
    implementation(libs.nodeGradle.plugin)
    implementation(libs.jsoup)

    compileOnly(libs.kotlin.gradlePlugin)
}

mavenPublishing {
    configure(GradlePublishPlugin())
}
