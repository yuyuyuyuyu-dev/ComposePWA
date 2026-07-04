package dev.yuyuyuyuyu.tasks.shared

import org.gradle.api.GradleException
import java.io.File

/**
 * Resources directories searched for the web app's index.html.
 *
 * A Compose Multiplatform web app can keep index.html in any of these source sets'
 * resources — the build merges each of them into the final bundle — so projects differ
 * in where they put it, and a project can even have one page per target (one index.html
 * in wasmJsMain and another in jsMain). The plugin reads index.html from, and places its
 * web assets next to, every directory that contains one, so each page's assets end up in
 * that page's resource bundle.
 */
internal val candidateTargetResourcesDirPaths: List<String> =
    listOf("webMain", "wasmJsMain", "jsMain", "commonMain").map { "src/$it/resources" }

/** Returns every candidate directory that contains an index.html. */
internal fun findTargetResourcesDirPaths(projectDir: File): List<String> =
    candidateTargetResourcesDirPaths.filter { File(projectDir, "$it/index.html").isFile }

/**
 * Like [findTargetResourcesDirPaths], but fails the build when there is no index.html
 * anywhere, listing the searched locations.
 */
internal fun resolveTargetResourcesDirPaths(projectDir: File): List<String> =
    findTargetResourcesDirPaths(projectDir).ifEmpty {
        throw GradleException(
            "ComposePWA needs your web app's index.html, but could not find it. Searched:\n" +
                candidateTargetResourcesDirPaths.joinToString("\n") { "  - $it/index.html" },
        )
    }
