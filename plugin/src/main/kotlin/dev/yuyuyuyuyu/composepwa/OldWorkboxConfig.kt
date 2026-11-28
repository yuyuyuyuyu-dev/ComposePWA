package dev.yuyuyuyuyu.composepwa

import org.gradle.api.GradleException
import java.io.File

internal fun failIfOldWorkboxConfigExists(
    projectDir: File,
    oldFileName: String,
    newFileName: String,
) {
    if (!File(projectDir, oldFileName).exists()) return
    throw GradleException(
        "ComposePWA now reads the Workbox config from $newFileName, so it no longer reads $oldFileName.\n" +
            "Rename $oldFileName to $newFileName to keep your settings, or delete $oldFileName if you never edited it.",
    )
}
