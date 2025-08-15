package dev.yuyuyuyuyu

import com.github.gradle.node.npm.task.NpxTask
import dev.yuyuyuyuyu.tasks.AddNecessaryHtmlTags
import dev.yuyuyuyuyu.tasks.shared.targetResourcesDirPath
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Suppress("unused")
class ComposePwa : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.github.node-gradle.node")

        registerCopyWorkboxConfigJs(project)
        registerCopyResisterServiceWorkerJs(project)
        registerCopyManifestJson(project)
        registerCopyIcons(project)
        project.tasks.register<AddNecessaryHtmlTags>("addNecessaryHtmlTags") {
            dependsOn("copyWorkboxConfigJs")
            dependsOn("copyResisterServiceWorkerJs")
            dependsOn("copyManifestJson")
            dependsOn("copyIcons")
        }
        project.tasks.register("initComposePwa") {
            dependsOn(
                "addNecessaryHtmlTags",
                "copyWorkboxConfigJs",
                "copyResisterServiceWorkerJs",
                "copyManifestJson",
                "copyIcons",
            )
        }
        project.tasks.register<NpxTask>("buildAsPwa") {
            dependsOn(
                "npmInstall",
                "wasmJsBrowserDistribution",
                "initComposePwa",
            )

            command.set("workbox-cli")
            args.set(listOf("generateSW", "workbox-config.js"))
        }

        project.plugins.withId("org.jetbrains.kotlin.wasm.js") {
            project.tasks.named("wasmJsBrowserDistribution") {
                dependsOn("initComposePwa")
                finalizedBy("buildAsPwa")
            }
        }

        project.afterEvaluate {
            project.tasks
                .findByName("copyNonXmlValueResourcesForWasmJsMain")
                ?.mustRunAfter(project.tasks.named("initComposePwa"))

            project.tasks
                .findByName("processSkikoRuntimeForKWasm")
                ?.mustRunAfter(project.tasks.named("copyWorkboxConfigJs"))

            project.tasks
                .findByName("convertXmlValueResourcesForCommonMain")
                ?.mustRunAfter(project.tasks.named("copyWorkboxConfigJs"))

            project.tasks
                .findByName("copyNonXmlValueResourcesForCommonMain")
                ?.mustRunAfter(project.tasks.named("copyWorkboxConfigJs"))
        }
    }

    private fun registerCopyWorkboxConfigJs(project: Project) {
        project.tasks.register<Copy>("copyWorkboxConfigJs") {
            val fileName = "workbox-config.js"
            val destDir = "."

            val file = readResourceFile(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            from(file)
            into(destDir)
            onlyIf { !destinationDir.resolve(fileName).exists() }
        }
    }

    private fun registerCopyResisterServiceWorkerJs(project: Project) {
        project.tasks.register<Copy>("copyResisterServiceWorkerJs") {
            val fileName = "registerServiceWorker.js"
            val destDir = targetResourcesDirPath

            val file = readResourceFile(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            from(file)
            into(destDir)
            onlyIf { !destinationDir.resolve(fileName).exists() }
        }
    }

    private fun registerCopyManifestJson(project: Project) {
        project.tasks.register<Copy>("copyManifestJson") {
            val fileName = "manifest.json"
            val destDir = targetResourcesDirPath

            val file = readResourceFile(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            from(file)
            into(destDir)
            onlyIf { !destinationDir.resolve(fileName).exists() }
        }
    }

    private fun registerCopyIcons(project: Project) {
        project.tasks.register<Copy>("copyIcons") {
            val dirName = "icons"
            val destDir = targetResourcesDirPath

            val file = readResourceFile("${dirName}.zip")
            if (file == null) {
                println("error: ${dirName}.zip not found")
                return@register
            }

            from(project.zipTree(file))
            into(destDir)
            onlyIf { !destinationDir.resolve(dirName).exists() }
        }
    }

    private fun readResourceFile(fileName: String): File? {
        val tempDir = Files.createTempDirectory("resources").toFile()
        val resourceUrl = this::class.java.classLoader.getResource(fileName)
        if (resourceUrl == null) {
            println("$fileName not found")
            return null
        }

        val tempFile = File(tempDir, File(fileName).name)
        resourceUrl.openStream().use { input ->
            Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        return tempFile
    }
}
