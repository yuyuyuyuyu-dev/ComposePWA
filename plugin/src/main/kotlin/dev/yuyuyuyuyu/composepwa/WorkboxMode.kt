package dev.yuyuyuyuyu.composepwa

enum class WorkboxMode(
    internal val command: String,
) {
    GenerateSW("generateSW"),
    InjectManifest("injectManifest"),
}
