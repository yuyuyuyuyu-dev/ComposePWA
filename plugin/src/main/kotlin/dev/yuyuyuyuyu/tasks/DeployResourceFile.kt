package dev.yuyuyuyuyu.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class DeployResourceFile : DefaultTask() {
    @get:Input
    abstract val resourceFileName: Property<String>

    @get:OutputFile
    abstract val destinationFileProperty: RegularFileProperty

    @Suppress("NewApi")
    @TaskAction
    fun execute() {
        val fileName = resourceFileName.get()
        val destFile = destinationFileProperty.get().asFile

        val resourceUrl = this::class.java.classLoader.getResource(fileName)
            ?: throw GradleException("Error: $fileName is not found.")
        resourceUrl.openStream().use { inputStream ->
            Files.copy(inputStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}