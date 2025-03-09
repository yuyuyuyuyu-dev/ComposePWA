package dev.yuyuyuyuyu

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.github.gradle.node.npm.task.NpxTask
import org.gradle.kotlin.dsl.register

@Suppress("unused")
class ComposeMultiplatformPwaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.github.node-gradle.node")

        project.tasks.register<NpxTask>("buildPWA") {
            dependsOn("clean", "npmInstall", "wasmJsBrowserDistribution")

            command.set("workbox-cli")
            args.set(listOf("generateSW", "workbox-config.js"))
        }
    }
}
