package dev.yuyuyuyuyu.composepwa

import java.io.File

internal fun findNpmProjectDir(startDir: File): File? {
    val nearestDir =
        generateSequence(startDir) { it.parentFile }
            .firstOrNull { hasPackageJson(it) || File(it, "node_modules").isDirectory }

    val workspaceRootDir =
        nearestDir?.let { dir ->
            generateSequence(dir.parentFile) { it.parentFile }
                .firstOrNull { hasPackageJson(it) && isWorkspaceOf(it, dir) }
        }

    return workspaceRootDir ?: nearestDir
}

private fun hasPackageJson(dir: File): Boolean = File(dir, "package.json").isFile

private fun isWorkspaceOf(
    rootDir: File,
    candidateDir: File,
): Boolean {
    val relativePath = candidateDir.relativeTo(rootDir).invariantSeparatorsPath
    val declaredPatterns = workspacePatterns(rootDir)

    return declaredPatterns != null &&
        hasPackageJson(candidateDir) &&
        "node_modules" !in relativePath.split('/') &&
        matchesWorkspacePatterns(relativePath, declaredPatterns)
}

private fun workspacePatterns(rootDir: File): List<String>? {
    val workspaces = readPackageJson(rootDir)?.get("workspaces")
    val declaration = (workspaces as? Map<*, *>)?.get("packages") ?: workspaces

    return (declaration as? List<*>)?.filterIsInstance<String>()
}

private fun matchesWorkspacePatterns(
    relativePath: String,
    declaredPatterns: List<String>,
): Boolean {
    val patterns = mutableListOf<String>()
    val negatedPatterns = mutableListOf<String>()

    declaredPatterns.forEach { declaredPattern ->
        val exclamationMarks = declaredPattern.takeWhile { it == '!' }.length
        val pattern = declaredPattern.drop(exclamationMarks).replace(Regex("""^\.?/+"""), "")

        if (exclamationMarks % 2 == 1) {
            negatedPatterns += pattern
        } else {
            negatedPatterns.removeAll { matchesGlob(pattern, it) }
            patterns += pattern
        }
    }
    negatedPatterns.forEach { negatedPattern ->
        patterns.removeAll { matchesGlob(it, negatedPattern) }
    }

    return patterns.any { matchesGlob(relativePath, it) } &&
        negatedPatterns.none { matchesGlob(relativePath, it) }
}
