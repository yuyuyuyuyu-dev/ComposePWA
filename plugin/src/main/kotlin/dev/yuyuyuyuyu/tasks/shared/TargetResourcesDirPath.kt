package dev.yuyuyuyuyu.tasks.shared

import org.gradle.api.GradleException
import java.io.File

/**
 * Resources directories searched for the web app's index.html, in priority order.
 *
 * A Compose Multiplatform web app can keep index.html in any of these source sets'
 * resources — the build merges each of them into the final bundle — so projects differ
 * in where they put it. The plugin reads index.html from, and places its web assets
 * next to, whichever directory actually contains the file, keeping everything in the
 * same resource bundle.
 */
internal val candidateTargetResourcesDirPaths: List<String> =
    listOf("webMain", "wasmJsMain", "jsMain", "commonMain").map { "src/$it/resources" }

/** Returns the first candidate directory that contains index.html, or null if none does. */
internal fun findTargetResourcesDirPath(projectDir: File): String? =
    candidateTargetResourcesDirPaths.firstOrNull { File(projectDir, "$it/index.html").isFile }

/** Like [findTargetResourcesDirPath], but fails the build listing the searched locations. */
internal fun resolveTargetResourcesDirPath(projectDir: File): String =
    findTargetResourcesDirPath(projectDir)
        ?: throw GradleException(
            "ComposePWA needs your web app's index.html, but could not find it. Searched:\n" +
                candidateTargetResourcesDirPaths.joinToString("\n") { "  - $it/index.html" },
        )
