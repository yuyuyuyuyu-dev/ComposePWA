import com.vanniktech.maven.publish.GradlePublishPlugin

plugins {
    kotlin("jvm")
    alias(libs.plugins.gradle.plugin.publish)
    `java-gradle-plugin`
    alias(libs.plugins.vanniktech.maven.publish)
    `kotlin-dsl`
    alias(libs.plugins.nodeGradle)
}

group = "dev.yuyuyuyuyu"
version = "0.1.0"

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
            implementationClass = "dev.yuyuyuyuyu.ComposePwaPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(libs.nodeGradle)
    implementation(libs.jsoup)
}

mavenPublishing {
    configure(GradlePublishPlugin())
}
