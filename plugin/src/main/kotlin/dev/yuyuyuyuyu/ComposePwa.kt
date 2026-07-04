package dev.yuyuyuyuyu

import com.github.gradle.node.npm.task.NpxTask
import dev.yuyuyuyuyu.tasks.AddNecessaryHtmlTags
import dev.yuyuyuyuyu.tasks.DeployResourceFile
import dev.yuyuyuyuyu.tasks.DeployZipResource
import dev.yuyuyuyuyu.tasks.shared.resolveTargetResourcesDirPaths
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class ComposePwa : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.github.node-gradle.node")

        val targetResourcesDirs = targetResourcesDirs(project)

        registerCopyWorkboxConfigForWasm(project)
        registerCopyWorkboxConfigForJs(project)
        registerCopyResisterServiceWorkerJs(project, targetResourcesDirs)
        registerCopyManifestJson(project, targetResourcesDirs)
        registerCopyIcons(project, targetResourcesDirs)
        registerAddNecessaryHtmlTags(project, targetResourcesDirs)

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
            task.destinationFiles.from(project.layout.projectDirectory.file(fileName))
        }
    }

    private fun registerCopyWorkboxConfigForJs(project: Project) {
        project.tasks.register("copyWorkboxConfigForJs", DeployResourceFile::class.java) { task ->
            val fileName = "workbox-config-for-js.js"

            task.resourceFileName.set(fileName)
            task.destinationFiles.from(project.layout.projectDirectory.file(fileName))
        }
    }

    private fun registerCopyResisterServiceWorkerJs(
        project: Project,
        targetResourcesDirs: Provider<List<Directory>>,
    ) {
        project.tasks.register("copyResisterServiceWorkerJs", DeployResourceFile::class.java) { task ->
            val fileName = "registerServiceWorker.js"

            task.resourceFileName.set(fileName)
            task.destinationFiles.from(
                targetResourcesDirs.map { dirs -> dirs.map { it.file(fileName) } },
            )
        }
    }

    private fun registerCopyManifestJson(
        project: Project,
        targetResourcesDirs: Provider<List<Directory>>,
    ) {
        project.tasks.register("copyManifestJson", DeployResourceFile::class.java) { task ->
            val fileName = "manifest.json"

            task.resourceFileName.set(fileName)
            task.destinationFiles.from(
                targetResourcesDirs.map { dirs -> dirs.map { it.file(fileName) } },
            )
            // After copyIcons, so copyIcons' manifest.json checks observe the state from
            // before this task creates the bundled manifest.
            task.mustRunAfter("copyIcons")
        }
    }

    private fun registerCopyIcons(
        project: Project,
        targetResourcesDirs: Provider<List<Directory>>,
    ) {
        project.tasks.register("copyIcons", DeployZipResource::class.java) { task ->
            val dirName = "icons"

            task.resourceFileName.set("$dirName.zip")
            task.destinationDirectories.from(targetResourcesDirs)
            // The bundled icons only make sense next to the bundled manifest.json, which
            // is what references them: wherever a manifest.json already exists (its
            // contents are never inspected), the bundled manifest is skipped, so the
            // icons are skipped too.
            task.skipWhenExisting.set(listOf(dirName, "manifest.json"))
        }
    }

    private fun registerAddNecessaryHtmlTags(
        project: Project,
        targetResourcesDirs: Provider<List<Directory>>,
    ) {
        project.tasks.register("addNecessaryHtmlTags", AddNecessaryHtmlTags::class.java) { task ->
            task.indexHtmlFiles.from(
                targetResourcesDirs.map { dirs -> dirs.map { it.file("index.html") } },
            )
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
     * The resources directories the plugin reads index.html from and writes its
     * generated files into: every candidate source set whose resources contain an
     * index.html.
     *
     * Resolved lazily (and only for tasks that are actually scheduled) so that projects
     * are configurable even before an index.html exists.
     */
    private fun targetResourcesDirs(project: Project): Provider<List<Directory>> {
        val projectDir = project.layout.projectDirectory
        return project.provider {
            resolveTargetResourcesDirPaths(projectDir.asFile).map { projectDir.dir(it) }
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
