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
    website = "https://github.com/yu-ko-ba/ComposeMultiplatformPWA#readme"
    vcsUrl = "https://github.com/yu-ko-ba/ComposeMultiplatformPWA"

    plugins {

        create("dev.yuyuyuyuyu.composemultiplatformpwa") {
            id = "dev.yuyuyuyuyu.composemultiplatformpwa"
            implementationClass = "dev.yuyuyuyuyu.ComposeMultiplatformPwaPlugin"
            description = "hoge"
            displayName = "Compose Multiplatform PWA"
            tags = listOf("compose multiplatform", "web", "pwa")
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("com.github.node-gradle.node:com.github.node-gradle.node.gradle.plugin:7.1.0")
}

mavenPublishing {
    configure(GradlePublishPlugin())
}
