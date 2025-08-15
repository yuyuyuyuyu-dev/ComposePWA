package dev.yuyuyuyuyu.tasks

import dev.yuyuyuyuyu.tasks.shared.targetResourcesDirPath
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.jsoup.Jsoup
import javax.inject.Inject

abstract class AddNecessaryHtmlTags : DefaultTask() {

    @get:Inject
    abstract val objects: ObjectFactory

    @get:InputFile
    val indexHtml: RegularFileProperty = objects.fileProperty().convention(
        project.layout.projectDirectory.file("${targetResourcesDirPath}/index.html"),
    )

    @TaskAction
    fun initComposePwa() {
        val indexHtmlFile = indexHtml.asFile.get()

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
}
