package dev.yuyuyuyuyu

import org.gradle.api.Plugin
import org.gradle.api.Project

class ComposeMultiplatformPwaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("buildPWA") {
            println("Hello, world!")
        }
    }
}
