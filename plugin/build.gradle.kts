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
            implementationClass = "dev.yuyuyuyuyu.ComposePwaPlugin"
            description = "A Gradle plugin to convert Compose Multiplatform web apps to PWA"
            displayName = "Compose PWA"
            tags = listOf("compose multiplatform", "web", "pwa")
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
