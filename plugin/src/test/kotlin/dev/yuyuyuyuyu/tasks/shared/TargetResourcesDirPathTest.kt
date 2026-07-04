package dev.yuyuyuyuyu.tasks.shared

import org.gradle.api.GradleException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
        assertEquals(listOf("src/webMain/resources"), findTargetResourcesDirPaths(projectDir.root))
    }

    @Test
    fun `finds index html in wasmJsMain`() {
        // Arrange
        createIndexHtml("wasmJsMain")

        // Act & Assert
        assertEquals(listOf("src/wasmJsMain/resources"), findTargetResourcesDirPaths(projectDir.root))
    }

    @Test
    fun `finds index html in jsMain`() {
        // Arrange
        createIndexHtml("jsMain")

        // Act & Assert
        assertEquals(listOf("src/jsMain/resources"), findTargetResourcesDirPaths(projectDir.root))
    }

    @Test
    fun `finds index html in commonMain`() {
        // Arrange: the layout reported in issue #27.
        createIndexHtml("commonMain")

        // Act & Assert
        assertEquals(listOf("src/commonMain/resources"), findTargetResourcesDirPaths(projectDir.root))
    }

    @Test
    fun `finds every index html when a project has one page per target`() {
        // Arrange
        createIndexHtml("wasmJsMain")
        createIndexHtml("jsMain")

        // Act & Assert
        assertEquals(
            listOf("src/wasmJsMain/resources", "src/jsMain/resources"),
            findTargetResourcesDirPaths(projectDir.root),
        )
    }

    @Test
    fun `returns nothing when no index html exists`() {
        // Arrange: a resources directory exists but holds no index.html.
        projectDir.newFolder("src", "webMain", "resources")

        // Act & Assert
        assertTrue(findTargetResourcesDirPaths(projectDir.root).isEmpty())
    }

    @Test
    fun `ignores a directory named index html`() {
        // Arrange: only a directory named index.html, not a file.
        projectDir.newFolder("src", "webMain", "resources", "index.html")

        // Act & Assert
        assertTrue(findTargetResourcesDirPaths(projectDir.root).isEmpty())
    }

    @Test
    fun `failure message lists every searched location`() {
        // Arrange: no index.html anywhere.

        // Act
        val exception =
            assertFailsWith<GradleException> {
                resolveTargetResourcesDirPaths(projectDir.root)
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
