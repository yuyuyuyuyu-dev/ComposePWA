package dev.yuyuyuyuyu

import com.github.gradle.node.npm.task.NpxTask
import dev.yuyuyuyuyu.tasks.AddNecessaryHtmlTags
import dev.yuyuyuyuyu.tasks.DeployResourceFile
import dev.yuyuyuyuyu.tasks.DeployZipResource
import dev.yuyuyuyuyu.tasks.shared.resolveTargetResourcesDirPath
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class ComposePwa : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.github.node-gradle.node")

        val targetResourcesDir = targetResourcesDir(project)

        registerCopyWorkboxConfigForWasm(project)
        registerCopyWorkboxConfigForJs(project)
        registerCopyResisterServiceWorkerJs(project, targetResourcesDir)
        registerCopyManifestJson(project, targetResourcesDir)
        registerCopyIcons(project, targetResourcesDir)
        registerAddNecessaryHtmlTags(project, targetResourcesDir)

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
        project.tasks.register("copyWorkboxConfigForWasm", DeployResourceFile::class.java) { task ->
            val fileName = "workbox-config-for-wasm.js"

            task.resourceFileName.set(fileName)
            task.destinationFileProperty.set(project.layout.projectDirectory.file(fileName))
            task.onlyIf {
                !task.destinationFileProperty
                    .get()
                    .asFile
                    .exists()
            }
        }
    }

    private fun registerCopyWorkboxConfigForJs(project: Project) {
        project.tasks.register("copyWorkboxConfigForJs", DeployResourceFile::class.java) { task ->
            val fileName = "workbox-config-for-js.js"

            task.resourceFileName.set(fileName)
            task.destinationFileProperty.set(project.layout.projectDirectory.file(fileName))
            task.onlyIf {
                !task.destinationFileProperty
                    .get()
                    .asFile
                    .exists()
            }
        }
    }

    private fun registerCopyResisterServiceWorkerJs(
        project: Project,
        targetResourcesDir: Provider<Directory>,
    ) {
        project.tasks.register("copyResisterServiceWorkerJs", DeployResourceFile::class.java) { task ->
            val fileName = "registerServiceWorker.js"

            task.resourceFileName.set(fileName)
            task.destinationFileProperty.set(targetResourcesDir.map { it.file(fileName) })
            task.onlyIf {
                !task.destinationFileProperty
                    .get()
                    .asFile
                    .exists()
            }
        }
    }

    private fun registerCopyManifestJson(
        project: Project,
        targetResourcesDir: Provider<Directory>,
    ) {
        project.tasks.register("copyManifestJson", DeployResourceFile::class.java) { task ->
            val fileName = "manifest.json"

            task.resourceFileName.set(fileName)
            task.destinationFileProperty.set(targetResourcesDir.map { it.file(fileName) })
            // After copyIcons, so copyIcons' manifest.json check observes the state from
            // before this task creates the bundled manifest.
            task.mustRunAfter("copyIcons")
            task.onlyIf {
                !task.destinationFileProperty
                    .get()
                    .asFile
                    .exists()
            }
        }
    }

    private fun registerCopyIcons(
        project: Project,
        targetResourcesDir: Provider<Directory>,
    ) {
        project.tasks.register("copyIcons", DeployZipResource::class.java) { task ->
            val dirName = "icons"

            task.resourceFileName.set("$dirName.zip")
            task.destinationDirectoryProperty.set(targetResourcesDir)
            // The bundled icons exist only to back the bundled manifest.json, which
            // references them: when the project brings its own manifest they would just be
            // dead files. An existing icons directory is never overwritten either way.
            task.onlyIf {
                val destinationDir =
                    task.destinationDirectoryProperty
                        .get()
                        .asFile
                !destinationDir.resolve(dirName).exists() &&
                    !destinationDir.resolve("manifest.json").exists()
            }
        }
    }

    private fun registerAddNecessaryHtmlTags(
        project: Project,
        targetResourcesDir: Provider<Directory>,
    ) {
        project.tasks.register("addNecessaryHtmlTags", AddNecessaryHtmlTags::class.java) { task ->
            task.indexHtml.convention(targetResourcesDir.map { it.file("index.html") })
            task.mustRunAfter(
                "copyWorkboxConfigForWasm",
                "copyWorkboxConfigForJs",
                "copyResisterServiceWorkerJs",
                "copyManifestJson",
                "copyIcons",
            )
        }
    }

    /**
     * The resources directory the plugin reads index.html from and writes its generated
     * files into: the first candidate source set whose resources contain index.html.
     *
     * Resolved lazily (and only for tasks that are actually scheduled) so that projects
     * are configurable even before an index.html exists.
     */
    private fun targetResourcesDir(project: Project): Provider<Directory> {
        val projectDir = project.layout.projectDirectory
        return project.provider {
            projectDir.dir(resolveTargetResourcesDirPath(projectDir.asFile))
        }
    }

    private fun addExecutionOrderOfTasks(project: Project) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) { kmpExt ->
            kmpExt.sourceSets.matching { it.name == "wasmJsMain" }.configureEach { sourceSet ->
                sourceSet.resources.srcDirs(project.tasks.named("initComposePwaForWasm"))
            }
            kmpExt.sourceSets.matching { it.name == "jsMain" }.configureEach { sourceSet ->
                sourceSet.resources.srcDirs(project.tasks.named("initComposePwaForJs"))
            }
        }
    }
}
