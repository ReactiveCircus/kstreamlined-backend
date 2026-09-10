package io.github.reactivecircus.kstreamlined.backend.tldr

import kotlin.test.Test
import kotlin.test.assertEquals

class TldrInputExtractorTest {
    @Test
    fun `preserves plain text and adjacent inline nodes`() {
        assertEquals("Plain text & entities", TldrInputExtractor.extract("Plain text &amp; entities"))
        assertEquals(
            "Use `Flow` with care.",
            TldrInputExtractor.extract("Use <code>Flow</code> <strong>with</strong> care."),
        )
    }

    @Test
    fun `preserves mixed content in document order through wrappers`() {
        val html = """
            <div>Important <em>caveat</em><p>Details</p>After <span>the paragraph</span></div>
            <section><span>Before<div>Wrapped block</div>After</span></section>
        """.trimIndent()

        assertEquals(
            "Important caveat\n\nDetails\n\nAfter the paragraph\n\nBefore\n\nWrapped block\n\nAfter",
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `preserves wrapped nested lists and following text`() {
        val html = """
            <ul><li>Parent<div><ul><li>Child</li></ul></div>After child</li></ul>
        """.trimIndent()

        assertEquals("- Parent\n  - Child\n\n  After child", TldrInputExtractor.extract(html))
    }

    @Test
    fun `preserves paragraphs code blocks and quotes inside list items`() {
        val html = """
            <ol><li><p>Run this:</p><pre data-enlighter-language="kotlin">fun main() {
                println("Hello")
            }</pre><p>Check the result.</p><blockquote><p>Keep the indentation.</p></blockquote>
            <ul><li>Then continue.</li></ul></li></ol>
        """.trimIndent()

        assertEquals(
            """
            1. Run this:

               ```kotlin
               fun main() {
                   println("Hello")
               }
               ```

               Check the result.

               > Keep the indentation.
               - Then continue.
            """.trimIndent(),
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `retains list-only items and omits empty items`() {
        assertEquals(
            "-\n  - Child",
            TldrInputExtractor.extract("<ul><li> </li><li><div><ul><li>Child</li></ul></div></li></ul>"),
        )
    }

    @Test
    fun `indents ordered list continuation by marker width`() {
        val html = "<ol>" + (1..10).joinToString("") { "<li>Item $it<p>Details</p></li>" } + "</ol>"
        val expected = (1..10).joinToString("\n") {
            "$it. Item $it\n\n${" ".repeat(it.toString().length + 2)}Details"
        }

        assertEquals(expected, TldrInputExtractor.extract(html))
    }

    @Test
    fun `preserves line breaks in paragraphs lists and captions`() {
        val html = """
            <p><br>First<br>Second<br></p>
            <ul><li>Step<br>Detail</li></ul>
            <figure><figcaption>Caption<br><em>Source</em></figcaption></figure>
        """.trimIndent()

        assertEquals("First\nSecond\n\n- Step\n  Detail\n\nCaption\nSource", TldrInputExtractor.extract(html))
    }

    @Test
    fun `retains descriptive image alt text but ignores decorative images`() {
        val html = """
            <figure><img src="screenshot.png" alt="AI agent uses the MCP server"><figcaption>Demo</figcaption></figure>
            <p>Try <img alt="Kotlin &amp; Java"> today.</p><img alt=""><img src="decoration.png">
        """.trimIndent()

        assertEquals("AI agent uses the MCP server\n\nDemo\n\nTry Kotlin & Java today.", TldrInputExtractor.extract(html))
    }

    @Test
    fun `renders feed Enlighter blocks without inferring language from unrelated attributes`() {
        val html = """
            <pre class="EnlighterJSRAW" data-enlighter-language="generic" data-enlighter-title="Kotlin publishing">product:
              type: lib
              platforms: [jvm, android, iosArm64, iosSimulatorArm64, wasmJs]</pre>
            <pre class="EnlighterJSRAW EnlighterJSRAW" data-enlighter-language="json">{"enabled": true}</pre>
        """.trimIndent()

        assertEquals(
            """
            ```
            product:
              type: lib
              platforms: [jvm, android, iosArm64, iosSimulatorArm64, wasmJs]
            ```

            ```json
            {"enabled": true}
            ```
            """.trimIndent(),
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `uses code delimiters that do not collide with source backticks`() {
        assertEquals(
            "`` `name` ``\n\n````\n```kotlin\nval x = 1\n```\n````",
            TldrInputExtractor.extract("<code>`name`</code><pre>```kotlin\nval x = 1\n```</pre>"),
        )
    }

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
