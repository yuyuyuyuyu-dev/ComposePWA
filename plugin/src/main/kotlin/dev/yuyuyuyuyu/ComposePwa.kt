package dev.yuyuyuyuyu

import com.github.gradle.node.npm.task.NpxTask
import dev.yuyuyuyuyu.tasks.AddNecessaryHtmlTags
import dev.yuyuyuyuyu.tasks.shared.targetResourcesDirPath
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
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

        project.tasks.register("initComposePwaForWasm") { task ->
            task.dependsOn(
                "addNecessaryHtmlTags",
                "copyWorkboxConfigForWasm",
                "copyResisterServiceWorkerJs",
                "copyManifestJson",
                "copyIcons",
            )
        }

        project.tasks.register("initComposePwaForJs") { task ->
            task.dependsOn(
                "addNecessaryHtmlTags",
                "copyWorkboxConfigForJs",
                "copyResisterServiceWorkerJs",
                "copyManifestJson",
                "copyIcons",
            )
        }

        project.tasks.register("buildWasmAsPwa", NpxTask::class.java) { task ->
            task.dependsOn(
                "npmInstall",
                "wasmJsBrowserDistribution",
                "initComposePwaForWasm",
            )
            task.command.set("workbox-cli")
            task.args.set(listOf("generateSW", "workbox-config-for-wasm.js"))
        }

        project.tasks.register("buildJsAsPwa", NpxTask::class.java) { task ->
            task.dependsOn(
                "npmInstall",
                "jsBrowserDistribution",
                "initComposePwaForJs",
            )
            task.command.set("workbox-cli")
            task.args.set(listOf("generateSW", "workbox-config-for-js.js"))
        }

        project.tasks.matching { it.name == "wasmJsBrowserDistribution" }.configureEach { task ->
            task.dependsOn("initComposePwaForWasm")
            task.finalizedBy("buildWasmAsPwa")
        }

        project.tasks.matching { it.name == "jsBrowserDistribution" }.configureEach { task ->
            task.dependsOn("initComposePwaForJs")
            task.finalizedBy("buildJsAsPwa")
        }

        addExecutionOrderOfTasks(project)

        project.afterEvaluate {
        }
    }

    private fun registerCopyWorkboxConfigForWasm(project: Project) {
        project.tasks.register("copyWorkboxConfigForWasm", Copy::class.java) { task ->
            val fileName = "workbox-config-for-wasm.js"
            val destDir = "."

            val file = readResourceFile(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            task.from(file)
            task.into(destDir)
            task.onlyIf { !task.destinationDir.resolve(fileName).exists() }
        }
    }

    private fun registerCopyWorkboxConfigForJs(project: Project) {
        project.tasks.register("copyWorkboxConfigForJs", Copy::class.java) { task ->
            val fileName = "workbox-config-for-js.js"
            val destDir = "."

            val file = readResourceFile(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            task.from(file)
            task.into(destDir)
            task.onlyIf { !task.destinationDir.resolve(fileName).exists() }
        }
    }

    private fun registerCopyResisterServiceWorkerJs(project: Project) {
        project.tasks.register("copyResisterServiceWorkerJs", Copy::class.java) { task ->
            val fileName = "registerServiceWorker.js"
            val destDir = targetResourcesDirPath

            val file = readResourceFile(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            task.from(file)
            task.into(destDir)
            task.onlyIf { !task.destinationDir.resolve(fileName).exists() }
        }
    }

    private fun registerCopyManifestJson(project: Project) {
        project.tasks.register("copyManifestJson", Copy::class.java) { task ->
            val fileName = "manifest.json"
            val destDir = targetResourcesDirPath

            val file = readResourceFile(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            task.from(file)
            task.into(destDir)
            task.onlyIf { !task.destinationDir.resolve(fileName).exists() }
        }
    }

    private fun registerCopyIcons(project: Project) {
        project.tasks.register("copyIcons", Copy::class.java) { task ->
            val dirName = "icons"
            val destDir = targetResourcesDirPath

            val file = readResourceFile("${dirName}.zip")
            if (file == null) {
                println("error: ${dirName}.zip not found")
                return@register
            }

            task.from(project.zipTree(file))
            task.into(destDir)
            task.onlyIf { !task.destinationDir.resolve(dirName).exists() }
        }
    }

    private fun registerAddNecessaryHtmlTags(project: Project) {
        project.tasks.register("addNecessaryHtmlTags", AddNecessaryHtmlTags::class.java) { task ->
            task.mustRunAfter(
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
            .configureEach { task ->
                task.mustRunAfter(
                    "copyWorkboxConfigForWasm",
                    "copyWorkboxConfigForJs",
                )
            }

        project.tasks
            .matching { it.name == "convertXmlValueResourcesForCommonMain" }
            .configureEach { task ->
                task.mustRunAfter(
                    "copyWorkboxConfigForWasm",
                    "copyWorkboxConfigForJs",
                )
            }

        project.tasks
            .matching { it.name == "CopyNonXmlValueResourcesTask" }
            .configureEach { task ->
                task.mustRunAfter(
                    "copyWorkboxConfigForWasm",
                    "copyWorkboxConfigForJs",
                )
            }

        // Wasm
        project.tasks
            .matching { it.name == "copyNonXmlValueResourcesForWasmJsMain" }
            .configureEach { task -> task.mustRunAfter("copyWorkboxConfigForWasm") }

        project.tasks
            .matching { it.name == "processSkikoRuntimeForKWasm" }
            .configureEach { task -> task.mustRunAfter("copyWorkboxConfigForWasm") }

        project.tasks
            .matching { it.name == "wasmJsProcessResources" }
            .configureEach { task ->
                task.mustRunAfter(
                    "copyManifestJson",
                    "copyResisterServiceWorkerJs",
                )
            }

        project.tasks
            .matching { it.name == "convertXmlValueResourcesForWasmJsMain" }
            .configureEach { task -> task.mustRunAfter("copyWorkboxConfigForWasm") }

        // Web
        project.tasks
            .matching { it.name == "copyNonXmlValueResourcesForWebMain" }
            .configureEach { task ->
                task.mustRunAfter(
                    "copyWorkboxConfigForWasm",
                    "copyWorkboxConfigForJs",
                )
            }

        project.tasks
            .matching { it.name == "convertXmlValueResourcesForWebMain" }
            .configureEach { task -> task.mustRunAfter("copyWorkboxConfigForWasm") }
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
