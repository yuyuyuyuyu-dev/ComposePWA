package dev.yuyuyuyuyu.tasks.shared

import org.gradle.api.GradleException
import java.io.File

/** Returns the first candidate directory that contains an index.html, or null if none does. */
internal fun findTargetResourcesDirPath(
    projectDir: File,
    candidateDirPaths: List<String>,
): String? = candidateDirPaths.firstOrNull { File(projectDir, "$it/index.html").isFile }

/**
 * Like [findTargetResourcesDirPath], but fails the build when no candidate contains an
 * index.html, listing the searched locations.
 */
internal fun resolveTargetResourcesDirPath(
    projectDir: File,
    candidateDirPaths: List<String>,
): String =
    findTargetResourcesDirPath(projectDir, candidateDirPaths)
        ?: throw GradleException(
            "ComposePWA needs your web app's index.html, but could not find it. Searched:\n" +
                candidateDirPaths.joinToString("\n") { "  - $it/index.html" },
        )
