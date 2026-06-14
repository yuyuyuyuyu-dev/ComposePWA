package dev.yuyuyuyuyu.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@DisableCachingByDefault(because = "Not worth caching")
abstract class DeployZipResource : DefaultTask() {
    @get:Input
    abstract val resourceFileName: Property<String>

    @get:OutputDirectory
    abstract val destinationDirectoryProperty: DirectoryProperty

    @TaskAction
    fun execute() {
        val fileName = resourceFileName.get()
        val destDir = destinationDirectoryProperty.get().asFile

        val resourceUrl =
            this::class.java.classLoader.getResource(fileName)
                ?: throw GradleException("Error: $fileName is not found.")

        resourceUrl.openStream().use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    writeEntry(zis, entry, destDir)
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    private fun writeEntry(
        zis: ZipInputStream,
        entry: ZipEntry,
        destDir: File,
    ) {
        val outFile = File(destDir, entry.name)
        if (entry.isDirectory) {
            outFile.mkdirs()
        } else {
            outFile.parentFile?.mkdirs()
            FileOutputStream(outFile).use { fos ->
                zis.copyTo(fos)
            }
        }
    }
}
