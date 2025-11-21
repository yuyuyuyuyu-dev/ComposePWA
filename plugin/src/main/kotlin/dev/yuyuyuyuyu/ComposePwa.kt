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

        registerCopyWorkboxConfigForWasm(project)
        registerCopyWorkboxConfigForJs(project)
        registerCopyResisterServiceWorkerJs(project)
        registerCopyManifestJson(project)
        registerCopyIcons(project)
        registerAddNecessaryHtmlTags(project)
        project.tasks.register("initComposePwaForWasm") {
            dependsOn(
                "addNecessaryHtmlTags",
                "copyWorkboxConfigForWasm",
                "copyResisterServiceWorkerJs",
                "copyManifestJson",
                "copyIcons",
            )
        }
        project.tasks.register("initComposePwaForJs") {
            dependsOn(
                "addNecessaryHtmlTags",
                "copyWorkboxConfigForJs",
                "copyResisterServiceWorkerJs",
                "copyManifestJson",
                "copyIcons",
            )
        }
        project.tasks.register<NpxTask>("buildWasmAsPwa") {
            dependsOn(
                "npmInstall",
                "wasmJsBrowserDistribution",
                "initComposePwaForWasm",
            )
            command.set("workbox-cli")
            args.set(listOf("generateSW", "workbox-config-for-wasm.js"))
        }
        project.tasks.register<NpxTask>("buildJsAsPwa") {
            dependsOn(
                "npmInstall",
                "jsBrowserDistribution",
                "initComposePwaForJs",
            )
            command.set("workbox-cli")
            args.set(listOf("generateSW", "workbox-config-for-js.js"))
        }

        project.tasks.matching { it.name == "wasmJsBrowserDistribution" }.configureEach {
            dependsOn("initComposePwaForWasm")
            finalizedBy("buildWasmAsPwa")
        }
        project.tasks.matching { it.name == "jsBrowserDistribution" }.configureEach {
            dependsOn("initComposePwaForJs")
            finalizedBy("buildJsAsPwa")
        }

        addExecutionOrderOfTasks(project)

        project.afterEvaluate {
        }
    }

    private fun registerCopyWorkboxConfigForWasm(project: Project) {
        project.tasks.register<Copy>("copyWorkboxConfigForWasm") {
            val fileName = "workbox-config-for-wasm.js"
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

    private fun registerCopyWorkboxConfigForJs(project: Project) {
        project.tasks.register<Copy>("copyWorkboxConfigForJs") {
            val fileName = "workbox-config-for-js.js"
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

    private fun registerAddNecessaryHtmlTags(project: Project) {
        project.tasks.register<AddNecessaryHtmlTags>("addNecessaryHtmlTags") {
            mustRunAfter(
                "copyWorkboxConfigForWasm",
                "copyWorkboxConfigForJs",
                "copyResisterServiceWorkerJs",
                "copyManifestJson",
                "copyIcons",
            )
        }
    }

    private fun addExecutionOrderOfTasks(project: Project) {
        // Common
        project.tasks
            .matching { it.name == "copyNonXmlValueResourcesForCommonMain" }
            .configureEach { mustRunAfter("copyWorkboxConfigForWasm") }

        project.tasks
            .matching { it.name == "convertXmlValueResourcesForCommonMain" }
            .configureEach {
                mustRunAfter(
                    "copyWorkboxConfigForWasm",
                    "copyWorkboxConfigForJs",
                )
            }

        project.tasks
            .matching { it.name == "CopyNonXmlValueResourcesTask" }
            .configureEach {
                mustRunAfter(
                    "copyWorkboxConfigForWasm",
                    "copyWorkboxConfigForJs",
                )
            }

        // Wasm
        project.tasks
            .matching { it.name == "copyNonXmlValueResourcesForWasmJsMain" }
            .configureEach { mustRunAfter("copyWorkboxConfigForWasm") }

        project.tasks
            .matching { it.name == "processSkikoRuntimeForKWasm" }
            .configureEach { mustRunAfter("copyWorkboxConfigForWasm") }

        project.tasks
            .matching { it.name == "wasmJsProcessResources" }
            .configureEach { mustRunAfter("copyManifestJson") }

        project.tasks
            .matching { it.name == "convertXmlValueResourcesForWasmJsMain" }
            .configureEach { mustRunAfter("copyWorkboxConfigForWasm") }

        // Web
        project.tasks
            .matching { it.name == "copyNonXmlValueResourcesForWebMain" }
            .configureEach {
                mustRunAfter(
                    "copyWorkboxConfigForWasm",
                    "copyWorkboxConfigForJs",
                )
            }

        project.tasks
            .matching { it.name == "convertXmlValueResourcesForWebMain" }
            .configureEach { mustRunAfter("copyWorkboxConfigForWasm") }
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
