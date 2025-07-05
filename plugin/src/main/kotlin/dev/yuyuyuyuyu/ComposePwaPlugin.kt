package dev.yuyuyuyuyu

import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import org.jsoup.Jsoup
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Suppress("unused")
class ComposePwaPlugin : Plugin<Project> {
    private val targetResourceDir = "src/wasmJsMain/resources"

    override fun apply(project: Project) {
        project.pluginManager.apply("com.github.node-gradle.node")

        project.tasks.register<NpxTask>("buildPWA") {
            dependsOn(
                "clean",
                "npmInstall",
                "initializeComposePWA",
                "wasmJsBrowserDistribution",
            )

            command.set("workbox-cli")
            args.set(listOf("generateSW", "workbox-config.js"))
        }

        project.tasks.register("initializeComposePWA") {
            dependsOn(
                "addNecessaryTagsForIndexHtml",
                "copyWorkboxConfigJs",
                "copyResisterServiceWorkerJs",
                "copyManifestJson",
                "copyIcons",
            )
        }

        project.tasks.register("addNecessaryTagsForIndexHtml") {
            val indexHtmlFile = project.file("${targetResourceDir}/index.html")

            val html = Jsoup.parse(indexHtmlFile, "UTF-8")
            val head = html.head()

            if (head.selectFirst("script[src=registerServiceWorker.js][type=application/javascript]") == null) {
                head.appendElement("script").apply {
                    attr("type", "application/javascript")
                    attr("src", "registerServiceWorker.js")
                }
            }

            if (head.selectFirst("link[rel=manifest][href=manifest.json]") == null) {
                head.appendElement("link").apply {
                    attr("rel", "manifest")
                    attr("href", "manifest.json")
                }
            }

            indexHtmlFile.writeText(html.outerHtml(), Charsets.UTF_8)
        }

        project.tasks.register<Copy>("copyWorkboxConfigJs") {
            val fileName = "workbox-config.js"
            val destDir = "."

            val file = readResourceFile(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            from(file)
            into(destDir)
            onlyIf { !destinationDir.resolve(fileName).exists() }
        }

        project.tasks.register<Copy>("copyResisterServiceWorkerJs") {
            val fileName = "registerServiceWorker.js"
            val destDir = targetResourceDir

            val file = readResourceFile(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            from(file)
            into(destDir)
            onlyIf { !destinationDir.resolve(fileName).exists() }
        }

        project.tasks.register<Copy>("copyManifestJson") {
            val fileName = "manifest.json"
            val destDir = targetResourceDir

            val file = readResourceFile(fileName)
            if (file == null) {
                println("error: $fileName not found")
                return@register
            }

            from(file)
            into(destDir)
            onlyIf { !destinationDir.resolve(fileName).exists() }
        }

        project.tasks.register<Copy>("copyIcons") {
            val dirName = "icons"
            val destDir = targetResourceDir

            val file = readResourceFile("${dirName}.zip")
            if (file == null) {
                println("error: ${dirName}.zip not found")
                return@register
            }

            from(project.zipTree(file))
            into(destDir)
            onlyIf { !destinationDir.resolve(dirName).exists() }
        }
    }

    private fun readResourceFile(fileName: String): File? {
        val tempDir = Files.createTempDirectory("resources").toFile()
        val resourceUrl = this::class.java.classLoader.getResource(fileName)
        if (resourceUrl == null) {
            println("$fileName not found")
            return null
        }

        val tempFile = File(tempDir, File(fileName).name)
        resourceUrl.openStream().use { input ->
            Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        return tempFile
    }
}
