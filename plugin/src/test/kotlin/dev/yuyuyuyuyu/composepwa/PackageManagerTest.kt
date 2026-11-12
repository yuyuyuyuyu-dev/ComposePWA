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

    private fun workspaceRoot(vararg patterns: String): String {
        val workspaces = patterns.joinToString { "\"$it\"" }
        return """{ "workspaces": [$workspaces], "devEngines": { "packageManager": { "name": "pnpm" } } }"""
    }

    @Test
    fun `should use npm when no package json or node_modules folder exists up to the file system root`() {
        // Arrange
        val projectDir = workspace.newFolder("repo", "webApp")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use pnpm when the project's package json declares pnpm in devEngines`() {
        // Arrange
        val projectDir = workspace.newFolder("repo")
        writePackageJson(projectDir, pnpmDevEngines)

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should use pnpm when a parent folder's package json declares pnpm in devEngines`() {
        // Arrange
        val rootDir = workspace.newFolder("repo")
        writePackageJson(rootDir, pnpmDevEngines)
        val projectDir = File(rootDir, "apps/webApp").apply { mkdirs() }

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should look above the Gradle root project like npm does`() {
        // Arrange
        writePackageJson(workspace.root, pnpmDevEngines)
        val projectDir = workspace.newFolder("repo", "webApp")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should only read the nearest package json`() {
        // Arrange
        val rootDir = workspace.newFolder("repo")
        writePackageJson(rootDir, pnpmDevEngines)
        val projectDir = File(rootDir, "webApp")
        writePackageJson(projectDir, """{ "name": "web-app" }""")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should stop at a node_modules folder even without a package json`() {
        // Arrange
        writePackageJson(workspace.root, pnpmDevEngines)
        workspace.newFolder("repo", "node_modules")
        val projectDir = workspace.newFolder("repo", "webApp")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use pnpm when the nearest package json is a workspace of a root that declares pnpm`() {
        // Arrange
        val rootDir = workspace.newFolder("repo")
        writePackageJson(rootDir, workspaceRoot("apps/*"))
        val projectDir = File(rootDir, "apps/webApp")
        writePackageJson(projectDir, """{ "name": "web-app" }""")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should use pnpm when the workspaces are declared under packages`() {
        // Arrange
        val rootDir = workspace.newFolder("repo")
        writePackageJson(
            rootDir,
            """{ "workspaces": { "packages": ["webApp"] }, "devEngines": { "packageManager": { "name": "pnpm" } } }""",
        )
        val projectDir = File(rootDir, "webApp")
        writePackageJson(projectDir, """{ "name": "web-app" }""")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should use npm when the root's workspaces do not include the project`() {
        // Arrange
        val rootDir = workspace.newFolder("repo")
        writePackageJson(rootDir, workspaceRoot("libs/*"))
        val projectDir = File(rootDir, "apps/webApp")
        writePackageJson(projectDir, """{ "name": "web-app" }""")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use npm when a negated workspace pattern excludes the project`() {
        // Arrange
        val rootDir = workspace.newFolder("repo")
        writePackageJson(rootDir, workspaceRoot("apps/*", "!apps/web*"))
        val projectDir = File(rootDir, "apps/webApp")
        writePackageJson(projectDir, """{ "name": "web-app" }""")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use pnpm when a later workspace pattern brings back a negated project`() {
        // Arrange
        val rootDir = workspace.newFolder("repo")
        writePackageJson(rootDir, workspaceRoot("apps/*", "!apps/web*", "apps/webApp"))
        val projectDir = File(rootDir, "apps/webApp")
        writePackageJson(projectDir, """{ "name": "web-app" }""")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should use pnpm when a globstar workspace pattern matches a nested project`() {
        // Arrange
        val rootDir = workspace.newFolder("repo")
        writePackageJson(rootDir, workspaceRoot("apps/**"))
        val projectDir = File(rootDir, "apps/web/webApp")
        writePackageJson(projectDir, """{ "name": "web-app" }""")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should use pnpm when a brace workspace pattern matches the project`() {
        // Arrange
        val rootDir = workspace.newFolder("repo")
        writePackageJson(rootDir, workspaceRoot("{apps,libs}/*"))
        val projectDir = File(rootDir, "apps/webApp")
        writePackageJson(projectDir, """{ "name": "web-app" }""")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should use npm when a wildcard workspace pattern would have to match a hidden folder`() {
        // Arrange
        val rootDir = workspace.newFolder("repo")
        writePackageJson(rootDir, workspaceRoot("*/webApp"))
        val projectDir = File(rootDir, ".apps/webApp")
        writePackageJson(projectDir, """{ "name": "web-app" }""")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use npm when the nearest folder has only node_modules inside a workspace root`() {
        // Arrange
        val rootDir = workspace.newFolder("repo")
        writePackageJson(rootDir, workspaceRoot("apps/*"))
        val projectDir = File(rootDir, "apps/webApp")
        File(projectDir, "node_modules").mkdirs()

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use npm when only the packageManager field declares pnpm`() {
        // Arrange
        val projectDir = workspace.newFolder("repo")
        writePackageJson(projectDir, """{ "packageManager": "pnpm@12.18.0" }""")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use npm when devEngines declares npm`() {
        // Arrange
        val projectDir = workspace.newFolder("repo")
        writePackageJson(projectDir, """{ "devEngines": { "packageManager": { "name": "npm" } } }""")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }

    @Test
    fun `should use pnpm when devEngines lists pnpm among several package managers`() {
        // Arrange
        val projectDir = workspace.newFolder("repo")
        writePackageJson(
            projectDir,
            """{ "devEngines": { "packageManager": [{ "name": "yarn" }, { "name": "pnpm" }] } }""",
        )

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Pnpm, packageManager)
    }

    @Test
    fun `should use npm when the package json is not valid JSON`() {
        // Arrange
        val projectDir = workspace.newFolder("repo")
        writePackageJson(projectDir, "{ not json")

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        assertEquals(PackageManager.Npm, packageManager)
    }
}
