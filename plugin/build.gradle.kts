import com.vanniktech.maven.publish.GradlePublishPlugin

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("com.gradle.plugin-publish") version "1.3.1"
    `java-gradle-plugin`
    id("com.vanniktech.maven.publish") version "0.33.0"
    `kotlin-dsl`
    id("com.github.node-gradle.node") version "7.1.0"
}

kotlin {
    jvmToolchain(17)
}

group = "dev.yuyuyuyuyu"
version = "0.3.0"

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
    implementation(localGroovy())
    implementation("com.github.node-gradle.node:com.github.node-gradle.node.gradle.plugin:7.1.0")
    implementation("org.jsoup:jsoup:1.21.1")
}

mavenPublishing {
    configure(GradlePublishPlugin())
}
