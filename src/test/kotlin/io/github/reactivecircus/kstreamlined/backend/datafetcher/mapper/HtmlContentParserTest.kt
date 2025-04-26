package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import kotlin.test.Test

class HtmlContentParserTest {

    @Test
    fun `tryParseHtml returns null for non-HTML content`() {
        assert(tryParseHtml("This is plain text") == null)
        assert(tryParseHtml("") == null)
        assert(tryParseHtml("   \n  \t  ") == null)
    }

    @Test
    fun `tryParseHtml returns Document for HTML content`() {
        assert(tryParseHtml("<p>This is <b>HTML</b> content</p>") != null)
        assert(
            tryParseHtml(
                """
                    <p>This is a paragraph with a <a href="https://example.com">link</a> and a list:</p>
                    <ul>
                        <li>First item</li>
                        <li>Second item with <b>bold</b> text</li>
                    </ul>
                """.trimIndent()
            ) != null
        )
        assert(tryParseHtml("<p>This is &quot;quoted&quot; text with &amp; ampersand</p>") != null)
    }

    @Test
    fun `tryParseHtml returns Document for HTML fragment`() {
        assert(tryParseHtml("Plain text followed by <p>HTML</p>") != null)
        assert(
            tryParseHtml(
                """
                    Some plain text at the start
                    <p>Followed by HTML content</p>
                    <ul>
                        <li>With a list</li>
                    </ul>
                """.trimIndent()
            ) != null
        )
    }
}
