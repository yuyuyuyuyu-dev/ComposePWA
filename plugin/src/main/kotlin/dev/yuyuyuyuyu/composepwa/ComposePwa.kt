package dev.yuyuyuyuyu.composepwa

import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("unused")
class ComposePwa : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.github.node-gradle.node")

        WebTarget.entries.forEach { target ->
            registerInitComposePwaTask(project, target)
            registerBuildAsPwaTask(
                project = project,
                taskName = target.buildAsPwaTaskName,
                distributionTaskName = target.browserDistributionTaskName,
                initTaskName = target.initTaskName,
                workboxConfigFileName = target.workboxConfigFileName,
            )

            project.tasks.matching { it.name == target.browserDistributionTaskName }.configureEach { task ->
                task.dependsOn(target.initTaskName)
                task.finalizedBy(target.buildAsPwaTaskName)
            }
        }

        registerCompatibilityTasks(project)

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
            task.oldWorkboxConfigFileName.set(target.oldWorkboxConfigFileName)
        }
    }

    private fun registerBuildAsPwaTask(
        project: Project,
        taskName: String,
        distributionTaskName: String,
        initTaskName: String,
        workboxConfigFileName: String,
    ) {
        val emptyNpmPrefixDir = project.layout.buildDirectory.dir(EMPTY_NPM_PREFIX_DIR_PATH)
        project.tasks.register(taskName, NpmTask::class.java) { task ->
            task.group = TASK_GROUP
            task.description = "Builds the output of $distributionTaskName as a PWA."
            task.dependsOn(distributionTaskName, initTaskName)
            task.npmCommand.set(listOf("exec"))
            task.args.set(
                emptyNpmPrefixDir.map { prefixDir ->
                    listOf(
                        "--prefix",
                        prefixDir.asFile.absolutePath,
                        "--",
                        "workbox-cli",
                        "generateSW",
                        workboxConfigFileName,
                    )
                },
            )
            task.doFirst { emptyNpmPrefixDir.get().asFile.mkdirs() }
        }
    }

    private fun registerCompatibilityTasks(project: Project) {
        project.tasks.register(COMPATIBILITY_INIT_TASK_NAME, InitWorkboxConfig::class.java) { task ->
            task.group = TASK_GROUP
            task.description = "Creates the Workbox config for the compatibility distribution."
            task.projectDirectory.set(project.layout.projectDirectory)
            task.workboxConfigFileName.set(COMPATIBILITY_WORKBOX_CONFIG_FILE_NAME)
            skipWhenCompatibilityOutputIsEmpty(project, task)
        }
        registerBuildAsPwaTask(
            project = project,
            taskName = COMPATIBILITY_BUILD_AS_PWA_TASK_NAME,
            distributionTaskName = COMPATIBILITY_DISTRIBUTION_TASK_NAME,
            initTaskName = COMPATIBILITY_INIT_TASK_NAME,
            workboxConfigFileName = COMPATIBILITY_WORKBOX_CONFIG_FILE_NAME,
        )
        project.tasks.named(COMPATIBILITY_BUILD_AS_PWA_TASK_NAME).configure { task ->
            skipWhenCompatibilityOutputIsEmpty(project, task)
        }

        project.tasks.matching { it.name == COMPATIBILITY_DISTRIBUTION_TASK_NAME }.configureEach { task ->
            task.finalizedBy(COMPATIBILITY_BUILD_AS_PWA_TASK_NAME)
            task.mustRunAfter(WebTarget.entries.map { it.buildAsPwaTaskName })
        }
    }

    private fun skipWhenCompatibilityOutputIsEmpty(
        project: Project,
        task: Task,
    ) {
        task.inputs
            .files(project.tasks.named(COMPATIBILITY_DISTRIBUTION_TASK_NAME).map { it.outputs.files.asFileTree })
            .withPropertyName("compatibilityDistribution")
            .skipWhenEmpty()
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
        const val EMPTY_NPM_PREFIX_DIR_PATH = "composepwa/empty-npm-prefix"
        const val COMPATIBILITY_DISTRIBUTION_TASK_NAME = "composeCompatibilityBrowserDistribution"
        const val COMPATIBILITY_INIT_TASK_NAME = "initComposePwaForCompatibility"
        const val COMPATIBILITY_BUILD_AS_PWA_TASK_NAME = "buildCompatibilityAsPwa"
        const val COMPATIBILITY_WORKBOX_CONFIG_FILE_NAME = "workbox-config-for-compatibility.cjs"
    }
}
