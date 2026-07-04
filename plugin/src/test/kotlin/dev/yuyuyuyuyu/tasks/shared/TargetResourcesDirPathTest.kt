package dev.yuyuyuyuyu.tasks.shared

import org.gradle.api.GradleException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TargetResourcesDirPathTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    private fun createIndexHtml(sourceSetName: String): File {
        val resourcesDir = projectDir.newFolder("src", sourceSetName, "resources")
        return File(resourcesDir, "index.html").apply { writeText("<html></html>") }
    }

    @Test
    fun `finds index html in webMain`() {
        // Arrange
        createIndexHtml("webMain")

        // Act & Assert
        assertEquals("src/webMain/resources", findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun `finds index html in wasmJsMain`() {
        // Arrange
        createIndexHtml("wasmJsMain")

        // Act & Assert
        assertEquals("src/wasmJsMain/resources", findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun `finds index html in jsMain`() {
        // Arrange
        createIndexHtml("jsMain")

        // Act & Assert
        assertEquals("src/jsMain/resources", findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun `finds index html in commonMain`() {
        // Arrange: the layout reported in issue #27.
        createIndexHtml("commonMain")

        // Act & Assert
        assertEquals("src/commonMain/resources", findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun `prefers webMain when several source sets contain index html`() {
        // Arrange
        createIndexHtml("commonMain")
        createIndexHtml("webMain")

        // Act & Assert
        assertEquals("src/webMain/resources", findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun `returns null when no index html exists`() {
        // Arrange: a resources directory exists but holds no index.html.
        projectDir.newFolder("src", "webMain", "resources")

        // Act & Assert
        assertNull(findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun `ignores a directory named index html`() {
        // Arrange: only a directory named index.html, not a file.
        projectDir.newFolder("src", "webMain", "resources", "index.html")

        // Act & Assert
        assertNull(findTargetResourcesDirPath(projectDir.root))
    }

    @Test
    fun `failure message lists every searched location`() {
        // Arrange: no index.html anywhere.

        // Act
        val exception =
            assertFailsWith<GradleException> {
                resolveTargetResourcesDirPath(projectDir.root)
            }

        // Assert
        val message = exception.message.orEmpty()
        assertContains(message, "needs your web app's index.html")
        assertContains(message, "src/webMain/resources/index.html")
        assertContains(message, "src/wasmJsMain/resources/index.html")
        assertContains(message, "src/jsMain/resources/index.html")
        assertContains(message, "src/commonMain/resources/index.html")
    }
}
