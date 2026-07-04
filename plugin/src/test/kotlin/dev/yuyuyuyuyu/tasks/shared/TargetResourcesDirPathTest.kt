package dev.yuyuyuyuyu.tasks.shared

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TargetResourcesDirPathTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    private fun createIndexHtml(sourceSetName: String): File {
        val resourcesDir = projectDir.newFolder("src", sourceSetName, "resources")
        return File(resourcesDir, "index.html").apply { writeText("<html></html>") }
    }

    @Test
    fun findsIndexHtmlInWebMain() {
        // Arrange: the official IDE templates put index.html in webMain.
        createIndexHtml("webMain")

        // Act & Assert
        assertEquals("src/webMain/resources", findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun findsIndexHtmlInWasmJsMain() {
        // Arrange: wasmJs-only projects put index.html in wasmJsMain.
        createIndexHtml("wasmJsMain")

        // Act & Assert
        assertEquals("src/wasmJsMain/resources", findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun findsIndexHtmlInJsMain() {
        // Arrange: js-only projects put index.html in jsMain.
        createIndexHtml("jsMain")

        // Act & Assert
        assertEquals("src/jsMain/resources", findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun findsIndexHtmlInCommonMain() {
        // Arrange: Compose-Multiplatform-Wizard projects put index.html in commonMain (issue #27).
        createIndexHtml("commonMain")

        // Act & Assert
        assertEquals("src/commonMain/resources", findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun prefersWebMainWhenSeveralSourceSetsContainIndexHtml() {
        // Arrange
        createIndexHtml("commonMain")
        createIndexHtml("webMain")

        // Act & Assert
        assertEquals("src/webMain/resources", findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun returnsNullWhenNoIndexHtmlExists() {
        // Arrange: resources directory exists but holds no index.html.
        projectDir.newFolder("src", "webMain", "resources")

        // Act & Assert
        assertNull(findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun ignoresDirectoriesNamedIndexHtml() {
        // Arrange: only a directory named index.html, not a file.
        projectDir.newFolder("src", "webMain", "resources", "index.html")

        // Act & Assert
        assertNull(findTargetResourcesDirPath(projectDir.root))
    }
}
