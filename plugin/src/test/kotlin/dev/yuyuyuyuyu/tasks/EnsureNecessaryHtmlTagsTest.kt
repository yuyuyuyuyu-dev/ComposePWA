package dev.yuyuyuyuyu.tasks

import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class EnsureNecessaryHtmlTagsTest {
    @Test
    fun returnsSameInstanceWhenBothTagsArePresent() {
        val html =
            """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="UTF-8" />
                <title>composeApp</title>
                <script type="application/javascript" src="registerServiceWorker.js"></script>
                <link rel="manifest" href="manifest.json" />
              </head>
              <body></body>
            </html>
            """.trimIndent()

        // Returning the very same instance is what lets the task skip the write entirely, leaving
        // the file byte-for-byte unchanged and out of the formatter's way.
        assertSame(html, ensureNecessaryHtmlTags(html))
    }

    @Test
    fun insertsBothMissingTagsAndPreservesExistingFormatting() {
        val html =
            """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>composeApp</title>
                <link type="text/css" rel="stylesheet" href="styles.css" />
              </head>
              <body></body>
            </html>
            """.trimIndent()

        val expected =
            """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>composeApp</title>
                <link type="text/css" rel="stylesheet" href="styles.css" />
                <script type="application/javascript" src="registerServiceWorker.js"></script>
                <link rel="manifest" href="manifest.json">
              </head>
              <body></body>
            </html>
            """.trimIndent()

        assertEquals(expected, ensureNecessaryHtmlTags(html))
    }

    @Test
    fun insertsOnlyTheMissingTag() {
        val html =
            """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="UTF-8" />
                <script type="application/javascript" src="registerServiceWorker.js"></script>
              </head>
              <body></body>
            </html>
            """.trimIndent()

        val expected =
            """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="UTF-8" />
                <script type="application/javascript" src="registerServiceWorker.js"></script>
                <link rel="manifest" href="manifest.json">
              </head>
              <body></body>
            </html>
            """.trimIndent()

        assertEquals(expected, ensureNecessaryHtmlTags(html))
    }

    @Test
    fun isIdempotent() {
        val html =
            """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="UTF-8" />
                <title>composeApp</title>
              </head>
              <body></body>
            </html>
            """.trimIndent()

        val once = ensureNecessaryHtmlTags(html)
        val twice = ensureNecessaryHtmlTags(once)

        // The second run finds both tags already present, so it is a no-op down to the instance.
        assertEquals(once, twice)
        assertSame(once, twice)
    }

    @Test
    fun insertsTagsUsingTheFilesExistingLineEndings() {
        // A CRLF document, e.g. an index.html checked out on Windows.
        val crlf =
            listOf(
                "<!doctype html>",
                "<html lang=\"en\">",
                "  <head>",
                "    <meta charset=\"UTF-8\" />",
                "  </head>",
                "  <body></body>",
                "</html>",
            ).joinToString("\r\n")

        val result = ensureNecessaryHtmlTags(crlf)

        // The inserted tags must use CRLF like the rest of the file, never a lone LF — otherwise the
        // service-worker/manifest lines would have different line endings from everything around them.
        assertFalse(
            result.replace("\r\n", "").contains('\n'),
            "an inserted tag introduced a lone LF into a CRLF document",
        )
        val script = """<script type="application/javascript" src="registerServiceWorker.js"></script>"""
        val link = """<link rel="manifest" href="manifest.json">"""
        assertContains(result, "\r\n    $script\r\n")
        assertContains(result, "\r\n    $link\r\n")
    }
}
