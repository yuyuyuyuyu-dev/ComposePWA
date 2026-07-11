package dev.yuyuyuyuyu.composepwa

import java.io.File

/** One copy of a PWA asset: the resources directory holding it, and whether its bytes match the bundled default. */
internal data class PwaAssetCopy(
    val dirPath: String,
    val isBundledDefault: Boolean,
)

/** A PWA asset present in more than one of the resources directories one target merges. */
internal data class PwaAssetConflict(
    val relativePath: String,
    val copies: List<PwaAssetCopy>,
)

/**
 * Finds every bundled asset path that exists in two or more of the searched directories.
 * Such duplicates make Gradle's resource merge fail, so they must be reported before any
 * file is copied. Each found copy is compared byte-for-byte with the bundled default, so
 * the report can tell an untouched leftover from a file the user customized.
 */
internal fun findPwaAssetConflicts(
    projectDir: File,
    candidateDirPaths: List<String>,
    bundledAssets: Map<String, ByteArray>,
): List<PwaAssetConflict> =
    bundledAssets.mapNotNull { (relativePath, bundledBytes) ->
        val copies =
            candidateDirPaths
                .map { dirPath -> dirPath to File(projectDir, "$dirPath/$relativePath") }
                .filter { (_, file) -> file.isFile }
                .map { (dirPath, file) ->
                    PwaAssetCopy(
                        dirPath = dirPath,
                        isBundledDefault = file.readBytes().contentEquals(bundledBytes),
                    )
                }
        if (copies.size > 1) PwaAssetConflict(relativePath, copies) else null
    }

/** The error shown for [findPwaAssetConflicts] findings; it must name every copy and how to resolve it. */
internal fun pwaAssetConflictMessage(conflicts: List<PwaAssetConflict>): String =
    buildString {
        append("ComposePWA found the same PWA asset in more than one resources directory. ")
        append("Gradle merges those directories into one page and cannot pick one, ")
        appendLine("so keep exactly one copy of each:")
        conflicts.forEach { conflict ->
            appendLine()
            appendLine("  ${conflict.relativePath} is in:")
            conflict.copies.forEach { copy ->
                append("    - ${copy.dirPath}/${conflict.relativePath}")
                if (copy.isBundledDefault) {
                    append(" (identical to the bundled default; safe to delete)")
                }
                appendLine()
            }
        }
    }
