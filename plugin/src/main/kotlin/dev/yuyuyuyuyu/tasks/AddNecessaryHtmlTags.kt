package dev.yuyuyuyuyu.tasks

import dev.yuyuyuyuyu.tasks.shared.TARGET_RESOURCES_DIR_PATH
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jsoup.Jsoup
import javax.inject.Inject

@DisableCachingByDefault(because = "Not worth caching")
abstract class AddNecessaryHtmlTags : DefaultTask() {
    @get:Inject
    abstract val objects: ObjectFactory

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    val indexHtml: RegularFileProperty =
        objects.fileProperty().convention(
            project.layout.projectDirectory.file("$TARGET_RESOURCES_DIR_PATH/index.html"),
        )

    @TaskAction
    fun initComposePwa() {
        val indexHtmlFile = indexHtml.asFile.get()

        val original = indexHtmlFile.readText(Charsets.UTF_8)
        val updated = ensureNecessaryHtmlTags(original)

        // Only rewrite when a tag was actually added. Leaving an already-complete file untouched
        // keeps it byte-for-byte as the user (or their formatter) left it, so repeated builds no
        // longer fight Prettier, dprint, Biome, and friends.
        if (updated != original) {
            indexHtmlFile.writeText(updated, Charsets.UTF_8)
        }
    }
}

/**
 * Returns [html] with the service-worker `<script>` and the manifest `<link>` guaranteed to be
 * present in the document `<head>`.
 *
 * Jsoup is used only to DETECT whether each tag already exists, so detection is independent of how
 * the file happens to be formatted. Any missing tag is then spliced in as plain text immediately
 * before `</head>`, leaving the rest of the document exactly as it was rather than re-serializing
 * the whole thing. When both tags are already present the very same [html] instance is returned,
 * which lets the caller skip rewriting the file altogether.
 */
internal fun ensureNecessaryHtmlTags(html: String): String {
    val missingTags = missingNecessaryHeadTags(html)
    val closeHeadIndex = html.indexOf("</head>", ignoreCase = true)
    if (missingTags.isEmpty() || closeHeadIndex < 0) return html

    val lineStart = html.lastIndexOf('\n', closeHeadIndex - 1) + 1
    val lineEnding = if (html.contains("\r\n")) "\r\n" else "\n"
    val indent = headContentIndent(html, lineStart)
    val insertion = missingTags.joinToString(separator = "") { "$indent$it$lineEnding" }

    return html.substring(0, lineStart) + insertion + html.substring(lineStart)
}

private fun missingNecessaryHeadTags(html: String): List<String> {
    val head = Jsoup.parse(html).head()
    return buildList {
        if (head.selectFirst("script[src=registerServiceWorker.js][type=application/javascript]") == null) {
            add("""<script type="application/javascript" src="registerServiceWorker.js"></script>""")
        }
        if (head.selectFirst("link[rel=manifest][href=manifest.json]") == null) {
            add("""<link rel="manifest" href="manifest.json">""")
        }
    }
}

/** Indentation of the last non-blank line before `</head>`, used to align the inserted tags. */
private fun headContentIndent(
    html: String,
    headCloseLineStart: Int,
): String {
    val lastContentLine =
        html
            .take(headCloseLineStart)
            .lineSequence()
            .lastOrNull { it.isNotBlank() }
            .orEmpty()
    return lastContentLine.takeWhile { it == ' ' || it == '\t' }
}
