package dev.yuyuyuyuyu.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@DisableCachingByDefault(because = "Not worth caching")
abstract class DeployResourceFile : DefaultTask() {
    @get:Input
    abstract val resourceFileName: Property<String>

    @get:Internal
    abstract val destinationFiles: ConfigurableFileCollection

    @Suppress("NewApi")
    @TaskAction
    fun execute() {
        val fileName = resourceFileName.get()

        val resourceUrl =
            this::class.java.classLoader.getResource(fileName)
                ?: throw GradleException("Error: $fileName is not found.")
        destinationFiles.forEach { destFile ->
            // A file the project already has wins over the bundled one.
            if (destFile.exists()) return@forEach
            resourceUrl.openStream().use { inputStream ->
                Files.copy(inputStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
