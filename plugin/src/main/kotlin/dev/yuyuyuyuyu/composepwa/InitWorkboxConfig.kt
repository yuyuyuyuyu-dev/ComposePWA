package dev.yuyuyuyuyu.composepwa

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files

@DisableCachingByDefault(because = "Not worth caching")
abstract class InitWorkboxConfig : DefaultTask() {
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    @get:Input
    abstract val workboxConfigFileName: Property<String>

    @TaskAction
    fun createWorkboxConfigIfMissing() {
        val fileName = workboxConfigFileName.get()
        val destFile = projectDirectory.file(fileName).get().asFile
        if (destFile.exists()) return

        val bundledConfig =
            this::class.java.classLoader.getResource(fileName)
                ?: throw GradleException("Error: $fileName is not found.")
        bundledConfig.openStream().use { inputStream ->
            Files.copy(inputStream, destFile.toPath())
        }
    }
}
