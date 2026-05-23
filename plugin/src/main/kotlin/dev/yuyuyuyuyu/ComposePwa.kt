package dev.yuyuyuyuyu

import com.github.gradle.node.npm.task.NpxTask
import dev.yuyuyuyuyu.tasks.AddNecessaryHtmlTags
import dev.yuyuyuyuyu.tasks.DeployResourceFile
import dev.yuyuyuyuyu.tasks.DeployZipResource
import dev.yuyuyuyuyu.tasks.shared.targetResourcesDirPath
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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

//        addExecutionOrderOfTasks(project)

        project.afterEvaluate {
        }
    }

    private fun registerCopyWorkboxConfigForWasm(project: Project) {
        project.tasks.register("copyWorkboxConfigForWasm", DeployResourceFile::class.java) { task ->
            val fileName = "workbox-config-for-wasm.js"

            task.resourceFileName.set(fileName)
            task.destinationFileProperty.set(project.layout.projectDirectory.file(fileName))
            task.onlyIf { !task.destinationFileProperty.get().asFile.exists() }
        }
    }

    private fun registerCopyWorkboxConfigForJs(project: Project) {
        project.tasks.register("copyWorkboxConfigForJs", DeployResourceFile::class.java) { task ->
            val fileName = "workbox-config-for-js.js"

            task.resourceFileName.set(fileName)
            task.destinationFileProperty.set(project.layout.projectDirectory.file(fileName))
            task.onlyIf { !task.destinationFileProperty.get().asFile.exists() }
        }
    }

    private fun registerCopyResisterServiceWorkerJs(project: Project) {
        project.tasks.register(
            "copyResisterServiceWorkerJs",
            DeployResourceFile::class.java
        ) { task ->
            val fileName = "registerServiceWorker.js"

            task.resourceFileName.set(fileName)
            task.destinationFileProperty.set(
                project.layout.projectDirectory.dir(
                    targetResourcesDirPath
                ).file(fileName)
            )
            task.onlyIf { !task.destinationFileProperty.get().asFile.exists() }
        }
    }

    private fun registerCopyManifestJson(project: Project) {
        project.tasks.register("copyManifestJson", DeployResourceFile::class.java) { task ->
            val fileName = "manifest.json"

            task.resourceFileName.set(fileName)
            task.destinationFileProperty.set(
                project.layout.projectDirectory.dir(
                    targetResourcesDirPath
                ).file(fileName)
            )
            task.onlyIf { !task.destinationFileProperty.get().asFile.exists() }
        }
    }

    private fun registerCopyIcons(project: Project) {
        project.tasks.register("copyIcons", DeployZipResource::class.java) { task ->
            val dirName = "icons"

            task.resourceFileName.set("${dirName}.zip")
            task.destinationDirectoryProperty.set(
                project.layout.projectDirectory.dir(
                    targetResourcesDirPath
                )
            )
            task.onlyIf {
                !task.destinationDirectoryProperty.get().asFile.resolve(dirName).exists()
            }
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
        project.extensions.configure(KotlinMultiplatformExtension::class.java) { kmpExt ->
            kmpExt.sourceSets.matching { it.name == "wasmJsMain" }.configureEach { sourceSet ->
                sourceSet.resources.srcDir(project.tasks.named("copyResisterServiceWorkerJs"))
                sourceSet.resources.srcDir(project.tasks.named("copyManifestJson"))
                sourceSet.resources.srcDir(project.tasks.named("copyIcons"))
            }
            kmpExt.sourceSets.matching { it.name == "jsMain" }.configureEach { sourceSet ->
                sourceSet.resources.srcDir(project.tasks.named("copyResisterServiceWorkerJs"))
                sourceSet.resources.srcDir(project.tasks.named("copyManifestJson"))
                sourceSet.resources.srcDir(project.tasks.named("copyIcons"))
            }
        }
    }
}
