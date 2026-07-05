package dev.yuyuyuyuyu

import com.github.gradle.node.npm.task.NpxTask
import dev.yuyuyuyuyu.tasks.InitComposePwa
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class ComposePwa : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.github.node-gradle.node")

        WebTarget.entries.forEach { target ->
            registerInitComposePwaTask(project, target)
            registerBuildAsPwaTask(project, target)

            project.tasks.matching { it.name == target.browserDistributionTaskName }.configureEach { task ->
                task.dependsOn(target.initTaskName)
                task.finalizedBy(target.buildAsPwaTaskName)
            }
        }

        // With index.html in webMain or commonMain, both init tasks stage the same
        // directory; Gradle may run them in parallel under the configuration cache, so
        // serialize them instead of coordinating concurrent writes (they are cheap).
        project.tasks.named(WebTarget.Js.initTaskName).configure { task ->
            task.mustRunAfter(WebTarget.Wasm.initTaskName)
        }

        registerInitTasksAsResources(project)
    }

    private fun registerInitComposePwaTask(
        project: Project,
        target: WebTarget,
    ) {
        project.tasks.register(target.initTaskName, InitComposePwa::class.java) { task ->
            task.group = TASK_GROUP
            task.description =
                "Copies the PWA web assets next to the ${target.name} target's index.html and tags it."
            task.projectDirectory.set(project.layout.projectDirectory)
            task.candidateResourcesDirPaths.set(target.candidateResourcesDirPaths)
            task.workboxConfigFileName.set(target.workboxConfigFileName)
        }
    }

    private fun registerBuildAsPwaTask(
        project: Project,
        target: WebTarget,
    ) {
        project.tasks.register(target.buildAsPwaTaskName, NpxTask::class.java) { task ->
            task.group = TASK_GROUP
            task.description = "Builds the ${target.name} browser distribution as a PWA."
            task.dependsOn("npmInstall", target.browserDistributionTaskName, target.initTaskName)
            task.command.set("workbox-cli")
            task.args.set(listOf("generateSW", target.workboxConfigFileName))
        }
    }

    // Makes each web target's processResources depend on its init task, so the staged
    // files take part in that target's resource merge.
    private fun registerInitTasksAsResources(project: Project) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) { kmpExt ->
            WebTarget.entries.forEach { target ->
                kmpExt.sourceSets.matching { it.name == target.targetSourceSetName }.configureEach { sourceSet ->
                    sourceSet.resources.srcDirs(project.tasks.named(target.initTaskName))
                }
            }
        }
    }

    private companion object {
        const val TASK_GROUP = "ComposePWA"
    }
}
