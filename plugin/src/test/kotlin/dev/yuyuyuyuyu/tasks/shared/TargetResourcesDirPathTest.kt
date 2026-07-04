package dev.yuyuyuyuyu.tasks.shared

import dev.yuyuyuyuyu.WebTarget
import org.gradle.api.GradleException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TargetResourcesDirPathTest {
    @get:Rule
    val projectDir = TemporaryFolder()

    private fun createIndexHtml(sourceSetName: String): File {
        val resourcesDir = projectDir.newFolder("src", sourceSetName, "resources")
        return File(resourcesDir, "index.html").apply { writeText("<html></html>") }
    }

    @Test
    fun `a wasm build searches webMain then wasmJsMain then commonMain`() {
        assertEquals(
            listOf("src/webMain/resources", "src/wasmJsMain/resources", "src/commonMain/resources"),
            WebTarget.Wasm.candidateResourcesDirPaths,
        )
    }

    @Test
    fun `a js build searches webMain then jsMain then commonMain`() {
        assertEquals(
            listOf("src/webMain/resources", "src/jsMain/resources", "src/commonMain/resources"),
            WebTarget.Js.candidateResourcesDirPaths,
        )
    }

    @Test
    fun `finds index html in webMain`() {
        // Arrange
        createIndexHtml("webMain")

        // Act & Assert
        assertEquals(
            "src/webMain/resources",
            findTargetResourcesDirPath(projectDir.root, WebTarget.Wasm.candidateResourcesDirPaths),
        )
    }

    @Test
    fun `finds index html in wasmJsMain`() {
        // Arrange
        createIndexHtml("wasmJsMain")

        // Act & Assert
        assertEquals(
            "src/wasmJsMain/resources",
            findTargetResourcesDirPath(projectDir.root, WebTarget.Wasm.candidateResourcesDirPaths),
        )
    }

    @Test
    fun `finds index html in jsMain`() {
        // Arrange
        createIndexHtml("jsMain")

        // Act & Assert
        assertEquals(
            "src/jsMain/resources",
            findTargetResourcesDirPath(projectDir.root, WebTarget.Js.candidateResourcesDirPaths),
        )
    }

    @Test
    fun `finds index html in commonMain`() {
        // Arrange: the layout reported in issue #27.
        createIndexHtml("commonMain")

        // Act & Assert
        assertEquals(
            "src/commonMain/resources",
            findTargetResourcesDirPath(projectDir.root, WebTarget.Wasm.candidateResourcesDirPaths),
        )
    }

    @Test
    fun `a wasm build does not see an index html in jsMain`() {
        // Arrange
        createIndexHtml("jsMain")

        // Act & Assert
        assertNull(findTargetResourcesDirPath(projectDir.root, WebTarget.Wasm.candidateResourcesDirPaths))
    }

    @Test
    fun `ignores a directory named index html`() {
        // Arrange: only a directory named index.html, not a file.
        projectDir.newFolder("src", "webMain", "resources", "index.html")

        // Act & Assert
        assertNull(findTargetResourcesDirPath(projectDir.root, WebTarget.Wasm.candidateResourcesDirPaths))
    }

    @Test
    fun `failure message lists exactly the searched locations`() {
        // Arrange: no index.html anywhere.

        // Act
        val exception =
            assertFailsWith<GradleException> {
                resolveTargetResourcesDirPath(projectDir.root, WebTarget.Wasm.candidateResourcesDirPaths)
            }

        // Assert
        val message = exception.message.orEmpty()
        assertContains(message, "needs your web app's index.html")
        assertContains(message, "src/webMain/resources/index.html")
        assertContains(message, "src/wasmJsMain/resources/index.html")
        assertContains(message, "src/commonMain/resources/index.html")
        assertFalse("src/jsMain/resources/index.html" in message)
    }
}
