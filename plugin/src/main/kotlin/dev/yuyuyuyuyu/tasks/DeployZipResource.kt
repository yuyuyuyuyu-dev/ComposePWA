package dev.yuyuyuyuyu.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@DisableCachingByDefault(because = "Not worth caching")
abstract class DeployZipResource : DefaultTask() {
    @get:Input
    abstract val resourceFileName: Property<String>

    @get:Internal
    abstract val destinationDirectories: ConfigurableFileCollection

    /** Relative paths whose presence makes this task skip that destination directory. */
    @get:Input
    abstract val skipWhenExisting: ListProperty<String>

    @TaskAction
    fun execute() {
        val fileName = resourceFileName.get()

        val resourceUrl =
            this::class.java.classLoader.getResource(fileName)
                ?: throw GradleException("Error: $fileName is not found.")

        destinationDirectories.forEach { destDir ->
            if (skipWhenExisting.get().any { destDir.resolve(it).exists() }) return@forEach
            unzipInto(resourceUrl, destDir)
        }
    }

    private fun unzipInto(
        resourceUrl: URL,
        destDir: File,
    ) {
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
