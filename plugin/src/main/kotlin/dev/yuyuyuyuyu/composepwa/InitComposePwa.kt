package dev.yuyuyuyuyu.composepwa

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
 * Deploys every file one web target's PWA needs: the workbox config in the project
 * directory, and registerServiceWorker.js, manifest.json, and icons/ into the target's
 * resources directories, following four rules:
 *
 * 1. A file already present in two searched directories fails the build with a report
 *    naming every copy — Gradle merges those directories into one page and cannot pick one.
 * 2. A file the project already has is used as is, wherever it lives.
 * 3. A file the sibling target keeps in its own resources directory is mirrored into this
 *    target's own resources directory — copying it into a directory both targets merge
 *    would recreate the very duplicate rule 1 rejects.
 * 4. A file the project has nowhere is created next to index.html, the one location the
 *    project itself has chosen for its page.
 *
 * All steps run sequentially in this one action — instead of one task per file — so
 * ordering constraints (the conflict scan must precede every copy; the icons decision
 * must observe the manifest decision) are plain statement order, not inter-task rules.
 */
@DisableCachingByDefault(because = "Not worth caching")
abstract class InitComposePwa : DefaultTask() {
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** Resources directories searched for index.html and existing PWA assets, in order. */
    @get:Input
    abstract val candidateResourcesDirPaths: ListProperty<String>

    /** This target's own source-set resources directory; where sibling-mirrored files go. */
    @get:Input
    abstract val ownResourcesDirPath: Property<String>

    /** The sibling target's own resources directory; a copy there signals a per-target convention. */
    @get:Input
    abstract val siblingResourcesDirPath: Property<String>

    @get:Input
    abstract val workboxConfigFileName: Property<String>

    @TaskAction
    fun deployPwaFiles() {
        val projectDir = projectDirectory.get().asFile
        val candidateDirPaths = candidateResourcesDirPaths.get()
        val indexHtmlDir = File(projectDir, resolveTargetResourcesDirPath(projectDir, candidateDirPaths))

        copyBundledFileIfMissing(workboxConfigFileName.get(), projectDir)

        val conflicts = findPwaAssetConflicts(projectDir, candidateDirPaths, bundledPwaAssets())
        if (conflicts.isNotEmpty()) {
            throw GradleException(pwaAssetConflictMessage(conflicts))
        }

        val candidateDirs = candidateDirPaths.map { File(projectDir, it) }

        deployDir("registerServiceWorker.js", candidateDirs, indexHtmlDir)?.let { destDir ->
            copyBundledFileIfMissing("registerServiceWorker.js", destDir)
        }

        // The bundled icons exist solely to back the bundled manifest.json, which
        // references them: when the project keeps a manifest.json anywhere (its contents
        // are never inspected), the bundled manifest is not copied, so the icons are not
        // either.
        deployDir("manifest.json", candidateDirs, indexHtmlDir)?.let { destDir ->
            if (candidateDirs.none { it.resolve("icons").exists() }) {
                unzipBundledArchiveIfMissing("icons.zip", targetDirName = "icons", destDir = destDir)
            }
            copyBundledFileIfMissing("manifest.json", destDir)
        }

        ensureNecessaryHtmlTagsIn(indexHtmlDir.resolve("index.html"))
    }

    /**
     * Where to create the named bundled file, or null when the project already has it
     * (rules 2-4; rule 1 has already rejected multiple copies).
     */
    private fun deployDir(
        fileName: String,
        candidateDirs: List<File>,
        indexHtmlDir: File,
    ): File? {
        val projectDir = projectDirectory.get().asFile
        return when {
            candidateDirs.any { it.resolve(fileName).exists() } -> null
            File(projectDir, siblingResourcesDirPath.get()).resolve(fileName).exists() ->
                File(projectDir, ownResourcesDirPath.get())
            else -> indexHtmlDir
        }
    }

    /** Every file this task deploys into resources directories, keyed by its merged relative path. */
    private fun bundledPwaAssets(): Map<String, ByteArray> {
        val assets = linkedMapOf<String, ByteArray>()
        listOf("registerServiceWorker.js", "manifest.json").forEach { fileName ->
            assets[fileName] = bundledResource(fileName).openStream().use { it.readBytes() }
        }
        forEachBundledZipEntry("icons.zip") { entry, zis ->
            if (!entry.isDirectory) {
                assets[entry.name] = zis.readBytes()
            }
        }
        return assets
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
        forEachBundledZipEntry(archiveFileName) { entry, zis ->
            writeEntry(zis, entry, destDir)
        }
    }

    private fun forEachBundledZipEntry(
        archiveFileName: String,
        action: (ZipEntry, ZipInputStream) -> Unit,
    ) {
        bundledResource(archiveFileName).openStream().use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    action(entry, zis)
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
