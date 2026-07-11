package dev.yuyuyuyuyu.composepwa

import com.github.gradle.node.npm.task.NpxTask
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

        // Gradle may run the two targets' tasks in parallel under the configuration
        // cache, but they share state Gradle cannot see: both init tasks can write to the
        // same resources directory (webMain/commonMain layouts), and both workbox tasks
        // install workbox-cli into the same npx cache entry, corrupting it
        // (TAR_ENTRY_ERROR). Everything here is cheap, so serialize the js pipeline
        // after the wasm one instead of coordinating concurrent writes.
        project.tasks.named(WebTarget.Js.initTaskName).configure { task ->
            task.mustRunAfter(WebTarget.Wasm.initTaskName)
        }
        project.tasks.named(WebTarget.Js.buildAsPwaTaskName).configure { task ->
            task.mustRunAfter(WebTarget.Wasm.buildAsPwaTaskName)
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
                "Deploys the ${target.name} target's PWA assets and tags its index.html."
            task.projectDirectory.set(project.layout.projectDirectory)
            task.candidateResourcesDirPaths.set(target.candidateResourcesDirPaths)
            task.ownResourcesDirPath.set(target.ownResourcesDirPath)
            task.siblingResourcesDirPath.set(target.sibling.ownResourcesDirPath)
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

    // Makes each web target's processResources depend on its init task, so the files it
    // deploys take part in that target's resource merge.
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
