package dev.yuyuyuyuyu.composepwa

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class PackageManagerMatchesNpmTest(
    @Suppress("unused") private val layoutName: String,
    private val createLayout: (File) -> File,
) {
    @get:Rule
    val workspace = TemporaryFolder()

    @Test
    fun `should read the package json from the same folder as npm`() {
        // Arrange
        val projectDir = createLayout(workspace.root).canonicalFile

        // Act
        val folder = findNpmProjectDir(projectDir) ?: projectDir

        // Assert
        assertEquals(File(runNpmPrefix(projectDir, "--force").output.trim()).canonicalFile, folder)
    }

    @Test
    fun `should use pnpm exactly when npm refuses to run`() {
        // Arrange
        val projectDir = createLayout(workspace.root)

        // Act
        val packageManager = detectPackageManager(projectDir)

        // Assert
        val npm = runNpmPrefix(projectDir)
        val npmRefuses = npm.exitCode != 0 && "EBADDEVENGINES" in npm.output
        assertEquals(npmRefuses, packageManager == PackageManager.Pnpm, npm.output)
    }

    private class NpmResult(
        val exitCode: Int,
        val output: String,
    )

    private fun runNpmPrefix(
        dir: File,
        vararg options: String,
    ): NpmResult {
        val errorFile = File.createTempFile("npm", ".log").apply { deleteOnExit() }
        val process =
            ProcessBuilder(listOf("npm", "prefix", *options))
                .directory(dir)
                .redirectError(errorFile)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(NPM_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "npm prefix timed out in $dir" }
        val exitCode = process.exitValue()

        return NpmResult(exitCode, if (exitCode == 0) output else errorFile.readText())
    }

    companion object {
        private const val NPM_TIMEOUT_SECONDS = 60L
        private const val PNPM = """{ "devEngines": { "packageManager": { "name": "pnpm", "onFail": "error" } } }"""
        private const val PLAIN = """{ "name": "web-app" }"""

        private fun File.withPackageJson(content: String): File =
            apply {
                mkdirs()
                File(this, "package.json").writeText(content)
            }

        private fun File.dir(path: String): File = File(this, path).apply { mkdirs() }

        private fun workspaceRoot(vararg patterns: String): String {
            val workspaces = patterns.joinToString { "\"$it\"" }
            return """{ "workspaces": [$workspaces], "devEngines": { "packageManager": { "name": "pnpm" } } }"""
        }

        private fun layout(
            name: String,
            createLayout: (File) -> File,
        ): Array<Any> = arrayOf(name, createLayout)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun layouts(): List<Array<Any>> = searchLayouts() + workspaceLayouts() + devEnginesLayouts()

        private fun searchLayouts(): List<Array<Any>> =
            listOf(
                layout("no package json") { it.dir("repo/webApp") },
                layout("project declares pnpm") { it.dir("repo").withPackageJson(PNPM) },
                layout("parent declares pnpm") {
                    it.dir("repo").withPackageJson(PNPM)
                    it.dir("repo/apps/webApp")
                },
                layout("package json above the Gradle root") {
                    it.withPackageJson(PNPM)
                    it.dir("repo/webApp")
                },
                layout("nearest package json without devEngines") {
                    it.dir("repo").withPackageJson(PNPM)
                    it.dir("repo/webApp").withPackageJson(PLAIN)
                },
                layout("node_modules folder without package json") {
                    it.withPackageJson(PNPM)
                    it.dir("repo/node_modules")
                    it.dir("repo/webApp")
                },
            )

        private fun workspaceLayouts(): List<Array<Any>> =
            listOf(
                layout("workspace member") {
                    it.dir("repo").withPackageJson(workspaceRoot("apps/*"))
                    it.dir("repo/apps/webApp").withPackageJson(PLAIN)
                },
                layout("workspaces under packages") {
                    val devEngines = """"devEngines": { "packageManager": { "name": "pnpm" } }"""
                    it.dir("repo").withPackageJson("""{ "workspaces": { "packages": ["webApp"] }, $devEngines }""")
                    it.dir("repo/webApp").withPackageJson(PLAIN)
                },
                layout("not a workspace member") {
                    it.dir("repo").withPackageJson(workspaceRoot("libs/*"))
                    it.dir("repo/apps/webApp").withPackageJson(PLAIN)
                },
                layout("negated workspace pattern") {
                    it.dir("repo").withPackageJson(workspaceRoot("apps/*", "!apps/web*"))
                    it.dir("repo/apps/webApp").withPackageJson(PLAIN)
                },
                layout("workspace pattern after a negated one") {
                    it.dir("repo").withPackageJson(workspaceRoot("apps/*", "!apps/web*", "apps/webApp"))
                    it.dir("repo/apps/webApp").withPackageJson(PLAIN)
                },
                layout("globstar workspace pattern") {
                    it.dir("repo").withPackageJson(workspaceRoot("apps/**"))
                    it.dir("repo/apps/web/webApp").withPackageJson(PLAIN)
                },
                layout("brace workspace pattern") {
                    it.dir("repo").withPackageJson(workspaceRoot("{apps,libs}/*"))
                    it.dir("repo/apps/webApp").withPackageJson(PLAIN)
                },
                layout("numeric sequence workspace pattern") {
                    it.dir("repo").withPackageJson(workspaceRoot("app{1..3}"))
                    it.dir("repo/app2").withPackageJson(PLAIN)
                },
                layout("character class workspace pattern") {
                    it.dir("repo").withPackageJson(workspaceRoot("app[!13]"))
                    it.dir("repo/app2").withPackageJson(PLAIN)
                },
                layout("wildcard over a hidden folder") {
                    it.dir("repo").withPackageJson(workspaceRoot("*/webApp"))
                    it.dir("repo/.apps/webApp").withPackageJson(PLAIN)
                },
                layout("workspace pattern inside node_modules") {
                    it.dir("repo").withPackageJson(workspaceRoot("node_modules/*"))
                    it.dir("repo/node_modules/webApp").withPackageJson(PLAIN)
                },
                layout("node_modules only inside a workspace root") {
                    it.dir("repo").withPackageJson(workspaceRoot("apps/*"))
                    it.dir("repo/apps/webApp/node_modules")
                    it.dir("repo/apps/webApp")
                },
            )

        private fun devEnginesLayouts(): List<Array<Any>> =
            listOf(
                layout("packageManager field only") {
                    it.dir("repo").withPackageJson("""{ "packageManager": "pnpm@12.18.0" }""")
                },
                layout("devEngines declares npm") {
                    it.dir("repo").withPackageJson("""{ "devEngines": { "packageManager": { "name": "npm" } } }""")
                },
                layout("devEngines lists pnpm among several") {
                    it.dir("repo").withPackageJson(
                        """{ "devEngines": { "packageManager": [{ "name": "yarn" }, { "name": "pnpm" }] } }""",
                    )
                },
            )
    }
}
