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
    val workboxConfigFileName: String,
) {
    Wasm("wasmJs", "workbox-config-for-wasm.js"),
    Js("js", "workbox-config-for-js.js"),
    ;

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
}
