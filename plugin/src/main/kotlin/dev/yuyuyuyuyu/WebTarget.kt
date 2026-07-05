package dev.yuyuyuyuyu

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
    val candidateResourcesDirPaths: List<String> =
        listOf("webMain", targetSourceSetName, "commonMain").map { "src/$it/resources" }
}
