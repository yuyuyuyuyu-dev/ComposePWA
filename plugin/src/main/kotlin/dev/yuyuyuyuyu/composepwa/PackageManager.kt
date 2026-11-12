package dev.yuyuyuyuyu.composepwa

import groovy.json.JsonException
import groovy.json.JsonSlurper
import java.io.File

internal enum class PackageManager {
    Npm,
    Pnpm,
}

internal fun detectPackageManager(projectDir: File): PackageManager {
    val packageJson = findNpmProjectDir(projectDir.canonicalFile)?.let(::readPackageJson)

    return if (packageJson != null && declaresPnpm(packageJson)) PackageManager.Pnpm else PackageManager.Npm
}

internal fun readPackageJson(dir: File): Map<*, *>? {
    val packageJson = File(dir, "package.json")

    return try {
        packageJson.takeIf { it.isFile }?.let { JsonSlurper().parseText(it.readText(Charsets.UTF_8)) as? Map<*, *> }
    } catch (_: JsonException) {
        null
    }
}

private fun declaresPnpm(packageJson: Map<*, *>): Boolean {
    val devEngines = packageJson["devEngines"] as? Map<*, *>
    val declaredPackageManagers =
        when (val packageManager = devEngines?.get("packageManager")) {
            is Map<*, *> -> listOf(packageManager)
            is List<*> -> packageManager.filterIsInstance<Map<*, *>>()
            else -> emptyList()
        }

    return declaredPackageManagers.any { it["name"] == "pnpm" }
}
