package dev.yuyuyuyuyu.composepwa

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The duplicate-asset report's contract; the plugin-level spec lives in tests.yml. */
class PwaAssetConflictsTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    private val bundledBytes = "bundled default".toByteArray()

    private fun createAsset(
        sourceSetName: String,
        relativePath: String,
        content: ByteArray = bundledBytes,
    ) {
        val file = File(projectDir.root, "src/$sourceSetName/resources/$relativePath")
        file.parentFile.mkdirs()
        file.writeBytes(content)
    }

    private fun findConflicts(assetRelativePath: String = "registerServiceWorker.js") =
        findPwaAssetConflicts(
            projectDir.root,
            WebTarget.Wasm.candidateResourcesDirPaths,
            mapOf(assetRelativePath to bundledBytes),
        )

    @Test
    fun `should report an asset present in two searched directories`() {
        // Arrange
        createAsset("webMain", "registerServiceWorker.js")
        createAsset("commonMain", "registerServiceWorker.js")

        // Act
        val conflicts = findConflicts()

        // Assert
        assertEquals(1, conflicts.size)
        assertEquals("registerServiceWorker.js", conflicts.single().relativePath)
        assertEquals(
            listOf("src/webMain/resources", "src/commonMain/resources"),
            conflicts.single().copies.map { it.dirPath },
        )
    }

    @Test
    fun `should not report an asset present in exactly one directory`() {
        // Arrange
        createAsset("webMain", "registerServiceWorker.js")

        // Act & Assert
        assertTrue(findConflicts().isEmpty())
    }

    @Test
    fun `should not look outside the searched directories`() {
        // Arrange: jsMain is not part of a wasm build.
        createAsset("webMain", "registerServiceWorker.js")
        createAsset("jsMain", "registerServiceWorker.js")

        // Act & Assert
        assertTrue(findConflicts().isEmpty())
    }

    @Test
    fun `should tell an untouched copy from a customized one`() {
        // Arrange
        createAsset("webMain", "manifest.json")
        createAsset("commonMain", "manifest.json", content = "customized by the user".toByteArray())

        // Act
        val copies = findConflicts("manifest.json").single().copies

        // Assert
        assertTrue(copies.single { it.dirPath == "src/webMain/resources" }.isBundledDefault)
        assertFalse(copies.single { it.dirPath == "src/commonMain/resources" }.isBundledDefault)
    }

    @Test
    fun `should name every copy and mark untouched ones safe to delete`() {
        // Arrange: the state left behind by ComposePWA versions up to 0.7.0-alpha01 (issue #47).
        createAsset("webMain", "registerServiceWorker.js")
        createAsset("commonMain", "registerServiceWorker.js", content = "the user's own".toByteArray())

        // Act
        val message = pwaAssetConflictMessage(findConflicts())

        // Assert
        assertContains(message, "found the same PWA asset in more than one resources directory")
        assertContains(message, "registerServiceWorker.js is in:")
        assertContains(
            message,
            "- src/webMain/resources/registerServiceWorker.js (identical to the bundled default; safe to delete)",
        )
        assertContains(message, "- src/commonMain/resources/registerServiceWorker.js\n")
        assertFalse("commonMain/resources/registerServiceWorker.js (identical" in message)
    }

    @Test
    fun `should list every conflicting asset in one report`() {
        // Arrange
        createAsset("webMain", "registerServiceWorker.js")
        createAsset("commonMain", "registerServiceWorker.js")
        createAsset("webMain", "icons/icon-192x192.png")
        createAsset("wasmJsMain", "icons/icon-192x192.png")

        // Act
        val conflicts =
            findPwaAssetConflicts(
                projectDir.root,
                WebTarget.Wasm.candidateResourcesDirPaths,
                mapOf(
                    "registerServiceWorker.js" to bundledBytes,
                    "icons/icon-192x192.png" to bundledBytes,
                ),
            )
        val message = pwaAssetConflictMessage(conflicts)

        // Assert
        assertEquals(2, conflicts.size)
        assertContains(message, "registerServiceWorker.js is in:")
        assertContains(message, "icons/icon-192x192.png is in:")
    }
}
