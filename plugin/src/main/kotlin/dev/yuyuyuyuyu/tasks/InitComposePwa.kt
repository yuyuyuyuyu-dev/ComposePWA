package dev.yuyuyuyuyu.tasks

import dev.yuyuyuyuyu.tasks.shared.resolveTargetResourcesDirPath
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Stages every file one web target's PWA needs: the workbox config in the project
 * directory, and registerServiceWorker.js, manifest.json, icons/, plus the required
 * tags next to that target's index.html.
 *
 * All steps run sequentially in this one action — instead of one task per file — so
 * ordering constraints (the icons decision must observe the manifest state from before
 * the bundled manifest is installed) are plain statement order, not inter-task rules.
 */
@DisableCachingByDefault(because = "Not worth caching")
abstract class InitComposePwa : DefaultTask() {
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** Resources directories searched for index.html, in order. */
    @get:Input
    abstract val candidateResourcesDirPaths: ListProperty<String>

    @get:Input
    abstract val workboxConfigFileName: Property<String>

    @TaskAction
    fun stagePwaFiles() {
        val projectDir = projectDirectory.get().asFile
        val resourcesDir =
            File(projectDir, resolveTargetResourcesDirPath(projectDir, candidateResourcesDirPaths.get()))

        copyBundledFileIfMissing(workboxConfigFileName.get(), projectDir)
        copyBundledFileIfMissing("registerServiceWorker.js", resourcesDir)

        // The bundled icons exist solely to back the bundled manifest.json, which
        // references them: when a manifest.json is already there (its contents are never
        // inspected), the bundled manifest is not installed, so the icons are not either.
        if (!resourcesDir.resolve("manifest.json").exists()) {
            unzipBundledArchiveIfMissing("icons.zip", targetDirName = "icons", destDir = resourcesDir)
            copyBundledFileIfMissing("manifest.json", resourcesDir)
        }

        ensureNecessaryHtmlTagsIn(resourcesDir.resolve("index.html"))
    }

    private fun bundledResource(fileName: String): URL =
        this::class.java.classLoader.getResource(fileName)
            ?: throw GradleException("Error: $fileName is not found.")

    private fun copyBundledFileIfMissing(
        fileName: String,
        destDir: File,
    ) {
        val destFile = File(destDir, fileName)
        // A file the project already has wins over the bundled one.
        if (destFile.exists()) return
        destFile.parentFile?.mkdirs()
        bundledResource(fileName).openStream().use { inputStream ->
            Files.copy(inputStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun unzipBundledArchiveIfMissing(
        archiveFileName: String,
        targetDirName: String,
        destDir: File,
    ) {
        // An existing directory is never overwritten.
        if (destDir.resolve(targetDirName).exists()) return
        bundledResource(archiveFileName).openStream().use { inputStream ->
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

    private fun ensureNecessaryHtmlTagsIn(indexHtmlFile: File) {
        val original = indexHtmlFile.readText(Charsets.UTF_8)
        val updated = ensureNecessaryHtmlTags(original)

        // Only write when a tag was actually added; an already-complete file is left
        // byte-for-byte untouched so repeated builds don't fight the user's formatter.
        if (updated != original) {
            indexHtmlFile.writeText(updated, Charsets.UTF_8)
        }
    }
}
