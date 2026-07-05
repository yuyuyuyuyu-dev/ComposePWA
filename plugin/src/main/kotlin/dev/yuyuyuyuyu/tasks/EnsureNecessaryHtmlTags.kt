package dev.yuyuyuyuyu.tasks

import org.jsoup.Jsoup

/**
 * Ensures the service-worker `<script>` and manifest `<link>` exist in the `<head>` of [html],
 * without reformatting the rest of the file.
 *
 * That constraint is the whole point. Tags are detected with Jsoup (so the check is independent of
 * formatting), but a missing one is inserted as raw text rather than via `Document.outerHtml()`,
 * which would re-serialize the entire document in Jsoup's style and fight the user's formatter.
 * When nothing is missing the same [html] instance is returned, so the caller can skip the write.
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
