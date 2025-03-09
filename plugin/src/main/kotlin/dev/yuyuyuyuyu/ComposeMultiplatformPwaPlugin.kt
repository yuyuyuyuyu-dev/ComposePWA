package dev.yuyuyuyuyu

import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Suppress("unused")
class ComposeMultiplatformPwaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.github.node-gradle.node")

        project.tasks.register<NpxTask>("buildPWA") {
            dependsOn(
                "initializeComposeMultiplatformPWA",
                "clean",
                "npmInstall",
                "wasmJsBrowserDistribution",
            )

            command.set("workbox-cli")
            args.set(listOf("generateSW", "workbox-config.js"))
        }

        project.tasks.register("initializeComposeMultiplatformPWA") {
            dependsOn(
                "copyWorkboxConfigJs",
                "copyResisterServiceWorkerJs",
                "copyManifestJson",
                "copyIcons",
            )
        }

        project.tasks.register<Copy>("copyWorkboxConfigJs") {
            val fileName = "workbox-config.js"
            val destDir = "."

            val file = readResourcesToTempDir(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            from(file)
            into(destDir)
            onlyIf { !project.file("${destDir}/${fileName}").exists() }
        }

        project.tasks.register<Copy>("copyResisterServiceWorkerJs") {
            val fileName = "registerServiceWorker.js"
            val destDir = "src/wasmJsMain/resources"

            val file = readResourcesToTempDir(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            from(file)
            into(destDir)
            onlyIf { !project.file("${destDir}/${fileName}").exists() }
        }

        project.tasks.register<Copy>("copyManifestJson") {
            val fileName = "manifest.json"
            val destDir = "src/wasmJsMain/resources"

            val file = readResourcesToTempDir(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            from(file)
            into(destDir)
            onlyIf { !project.file("${destDir}/${fileName}").exists() }
        }

        project.tasks.register<Copy>("copyIcons") {
            val dirName = "icons"
            val destDir = "src/wasmJsMain/resources"

            val file = readResourcesToTempDir("${dirName}.zip")
            if (file == null) {
                println("error: ${dirName}.zip not found")
                return@register
            }

            from(project.zipTree(file))
            into(destDir)
            onlyIf { !project.file("${destDir}/${dirName}").exists() }
        }
    }

    private fun readResourcesToTempDir(resourceDirPath: String): File? {
        val tempDir = Files.createTempDirectory("resources").toFile()
        val resourceUrl = this::class.java.classLoader.getResource(resourceDirPath)
        if (resourceUrl == null) {
            println("$resourceDirPath not found")
            return null
        }

        val tempFile = File(tempDir, File(resourceDirPath).name)
        resourceUrl.openStream().use { input ->
            Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        return tempFile
    }
}
