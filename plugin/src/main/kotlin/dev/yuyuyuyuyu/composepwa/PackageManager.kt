package dev.yuyuyuyuyu.composepwa

import groovy.json.JsonException
import groovy.json.JsonSlurper
import java.io.File

internal enum class PackageManager {
    Npm,
    Pnpm,
}

internal fun detectPackageManager(
    projectDir: File,
    rootDir: File,
): PackageManager {
    val packageJson =
        generateSequence(projectDir.canonicalFile) { dir ->
            dir.parentFile.takeIf { dir != rootDir.canonicalFile }
        }.map { File(it, "package.json") }
            .firstOrNull { it.isFile }
            ?: return PackageManager.Npm

    return if (declaresPnpm(packageJson.readText(Charsets.UTF_8))) PackageManager.Pnpm else PackageManager.Npm
}

private fun declaresPnpm(packageJsonText: String): Boolean {
    val packageJson =
        try {
            JsonSlurper().parseText(packageJsonText)
        } catch (_: JsonException) {
            return false
        }

    val devEngines = (packageJson as? Map<*, *>)?.get("devEngines") as? Map<*, *>
    val declaredPackageManagers =
        when (val packageManager = devEngines?.get("packageManager")) {
            is Map<*, *> -> listOf(packageManager)
            is List<*> -> packageManager.filterIsInstance<Map<*, *>>()
            else -> emptyList()
        }

    return declaredPackageManagers.any { it["name"] == "pnpm" }
}
