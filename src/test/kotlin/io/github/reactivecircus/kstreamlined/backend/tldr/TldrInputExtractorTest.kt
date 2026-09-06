package io.github.reactivecircus.kstreamlined.backend.tldr

import kotlin.test.Test
import kotlin.test.assertEquals

class TldrInputExtractorTest {
    @Test
    fun `extracts headings paragraphs entities and inline code`() {
        val html = """
            <h2>Kotlin &amp; Java</h2>
            <p>Call <code>flow.collect()</code> when x &lt; y.</p>
        """.trimIndent()

        assertEquals(
            """
            ## Kotlin & Java

            Call `flow.collect()` when x < y.
            """.trimIndent(),
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `removes non-content and hidden elements`() {
        val html = """
            <p>Visible introduction.</p>
            <script>trackArticle()</script>
            <nav><p>Previous article</p></nav>
            <div hidden><p>Hidden content</p></div>
            <div aria-hidden="true"><p>Also hidden</p></div>
            <p>Visible conclusion.</p>
        """.trimIndent()

        assertEquals(
            """
            Visible introduction.

            Visible conclusion.
            """.trimIndent(),
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `renders nested unordered and ordered lists`() {
        val html = """
            <ul>
              <li>Coroutines</li>
              <li>
                Flows
                <ol>
                  <li>Cold streams</li>
                  <li>Hot streams with <code>StateFlow</code></li>
                </ol>
              </li>
            </ul>
        """.trimIndent()

        assertEquals(
            """
            - Coroutines
            - Flows
              1. Cold streams
              2. Hot streams with `StateFlow`
            """.trimIndent(),
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `preserves code whitespace and detects language hints`() {
        val html = """
            <pre><code class="language-kotlin">fun main() {
                println("Hello")
            }</code></pre>
        """.trimIndent()

        assertEquals(
            """
            ```kotlin
            fun main() {
                println("Hello")
            }
            ```
            """.trimIndent(),
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `does not infer a supported language from part of another language name`() {
        val html = """
            <pre><code class="language-javascript">const greeting = "Hello"</code></pre>
        """.trimIndent()

        assertEquals(
            """
            ```
            const greeting = "Hello"
            ```
            """.trimIndent(),
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `renders blockquotes inside noisy containers`() {
        val html = """
            <article>
              <section>
                <blockquote>
                  <p>First quoted paragraph.</p>
                  <p>Second paragraph with <code>inline code</code>.</p>
                </blockquote>
              </section>
            </article>
        """.trimIndent()

        assertEquals(
            """
            > First quoted paragraph.
            > Second paragraph with `inline code`.
            """.trimIndent(),
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `omits empty structural elements`() {
        val html = """
            <h1> </h1>
            <p></p>
            <pre> 
            </pre>
            <hr>
            <p>Content</p>
        """.trimIndent()

        assertEquals("Content", TldrInputExtractor.extract(html))
    }
}
