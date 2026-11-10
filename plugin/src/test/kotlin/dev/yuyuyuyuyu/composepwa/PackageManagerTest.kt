package dev.yuyuyuyuyu.composepwa

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class PackageManagerTest {
    @get:Rule
    val workspace = TemporaryFolder()

    private val pnpmDevEngines =
        """{ "devEngines": { "packageManager": { "name": "pnpm", "version": "12.18.0", "onFail": "download" } } }"""

    private fun writePackageJson(
        dir: File,
        content: String,
    ) {
        dir.mkdirs()
        File(dir, "package.json").writeText(content)
    }

    @Test
    fun `should use npm when no package json exists up to the root project`() {
        // Arrange
        val rootDir = workspace.newFolder("root")
        val projectDir = File(rootDir, "webApp").apply { mkdirs() }

        // Act
        val packageManager = detectPackageManager(projectDir, rootDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use pnpm when the project's package json declares pnpm in devEngines`() {
        // Arrange
        val rootDir = workspace.newFolder("root")
        writePackageJson(rootDir, pnpmDevEngines)

        // Act
        val packageManager = detectPackageManager(rootDir, rootDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should use pnpm when the root project's package json declares pnpm in devEngines`() {
        // Arrange
        val rootDir = workspace.newFolder("root")
        writePackageJson(rootDir, pnpmDevEngines)
        val projectDir = File(rootDir, "apps/webApp").apply { mkdirs() }

        // Act
        val packageManager = detectPackageManager(projectDir, rootDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should not look above the root project`() {
        // Arrange
        writePackageJson(workspace.root, pnpmDevEngines)
        val rootDir = workspace.newFolder("root")
        val projectDir = File(rootDir, "webApp").apply { mkdirs() }

        // Act
        val packageManager = detectPackageManager(projectDir, rootDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should only read the nearest package json`() {
        // Arrange
        val rootDir = workspace.newFolder("root")
        writePackageJson(rootDir, pnpmDevEngines)
        val projectDir = File(rootDir, "webApp")
        writePackageJson(projectDir, """{ "name": "web-app" }""")

        // Act
        val packageManager = detectPackageManager(projectDir, rootDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use npm when only the packageManager field declares pnpm`() {
        // Arrange
        val rootDir = workspace.newFolder("root")
        writePackageJson(rootDir, """{ "packageManager": "pnpm@12.18.0" }""")

        // Act
        val packageManager = detectPackageManager(rootDir, rootDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use npm when devEngines declares npm`() {
        // Arrange
        val rootDir = workspace.newFolder("root")
        writePackageJson(rootDir, """{ "devEngines": { "packageManager": { "name": "npm" } } }""")

        // Act
        val packageManager = detectPackageManager(rootDir, rootDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use pnpm when devEngines lists pnpm among several package managers`() {
        // Arrange
        val rootDir = workspace.newFolder("root")
        writePackageJson(
            rootDir,
            """{ "devEngines": { "packageManager": [{ "name": "yarn" }, { "name": "pnpm" }] } }""",
        )

        // Act
        val packageManager = detectPackageManager(rootDir, rootDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should use npm when the package json is not valid JSON`() {
        // Arrange
        val rootDir = workspace.newFolder("root")
        writePackageJson(rootDir, "{ not json")

        // Act
        val packageManager = detectPackageManager(rootDir, rootDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }
}
