package dev.yuyuyuyuyu.composepwa

/**
 * The two web targets the plugin supports, and everything that differs between them.
 *
 * Each target's build searches only the resources directories that feed that target's
 * compilation. Within one chain, Compose Multiplatform rejects a duplicated index.html
 * as duplicate resources, so the first directory that contains one is the only viable
 * location.
 */
internal enum class WebTarget(
    val targetName: String,
    private val workboxConfigSuffix: String,
) {
    Wasm("wasmJs", "for-wasm.js"),
    Js("js", "for-js.js"),
    ;

    fun workboxConfigFileName(mode: WorkboxMode): String =
        when (mode) {
            WorkboxMode.GenerateSW -> "workbox-config-$workboxConfigSuffix"
            WorkboxMode.InjectManifest -> "workbox-inject-manifest-config-$workboxConfigSuffix"
        }

    fun workboxFileNames(mode: WorkboxMode): List<String> =
        when (mode) {
            WorkboxMode.GenerateSW -> listOf(workboxConfigFileName(mode))
            WorkboxMode.InjectManifest -> listOf(workboxConfigFileName(mode), SERVICE_WORKER_SOURCE_FILE_NAME)
        }

    val targetSourceSetName: String = "${targetName}Main"
    val initTaskName: String = "initComposePwaFor$name"
    val buildAsPwaTaskName: String = "build${name}AsPwa"
    val browserDistributionTaskName: String = "${targetName}BrowserDistribution"

    /** This target's own source-set resources directory — the one the sibling target never sees. */
    val ownResourcesDirPath: String = "src/$targetSourceSetName/resources"

    val candidateResourcesDirPaths: List<String> =
        listOf("src/webMain/resources", ownResourcesDirPath, "src/commonMain/resources")

    /** The other web target; its [ownResourcesDirPath] signals a per-target file convention. */
    val sibling: WebTarget get() = if (this == Wasm) Js else Wasm

    private companion object {
        const val SERVICE_WORKER_SOURCE_FILE_NAME = "serviceWorker.js"
    }
}
