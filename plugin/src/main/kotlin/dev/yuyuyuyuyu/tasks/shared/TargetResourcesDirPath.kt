package dev.yuyuyuyuyu.tasks.shared

import org.gradle.api.GradleException
import java.io.File

/**
 * Resources directories searched for the web app's index.html, in priority order.
 *
 * Projects disagree on where index.html lives: the official IDE templates use `webMain`,
 * single-web-target projects use `wasmJsMain` or `jsMain`, and Compose-Multiplatform-Wizard
 * projects use `commonMain`. The plugin reads index.html from — and places its web assets
 * next to — whichever directory actually contains the file, so that everything ends up in
 * the same resource bundle.
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
            "ComposePWA could not find your web app's index.html. Searched:\n" +
                candidateTargetResourcesDirPaths.joinToString("\n") { "  - $it/index.html" },
        )
