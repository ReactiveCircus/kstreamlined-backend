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

    @Test
    fun `preserves absolute HTTP links and surrounding inline content`() {
        val html = """
            <p>Read <a href="https://kotlinlang.org/docs/flow.html">the docs</a>,
            then <a href="http://example.com/guide">this guide</a>.</p>
            <p><a href=" HTTPS://example.com/CaseSensitive ">Uppercase scheme</a></p>
        """.trimIndent()

        assertEquals(
            "Read [the docs](<https://kotlinlang.org/docs/flow.html>), " +
                "then [this guide](<http://example.com/guide>).\n\n" +
                "[Uppercase scheme](<HTTPS://example.com/CaseSensitive>)",
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `preserves inline code in link labels without escaping its contents`() {
        val html = """
            <p>Use <a href="https://example.com/api"><strong>the <code>List&lt;T&gt;</code> API</strong></a>.</p>
            <p><a href="https://example.com/code"><code>`[value]`</code></a></p>
        """.trimIndent()

        assertEquals(
            "Use [the `List<T>` API](<https://example.com/api>).\n\n" +
                "[`` `[value]` ``](<https://example.com/code>)",
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `escapes Markdown in link label text and image alt text`() {
        val html = """
            <a href="https://example.com">[value] \ * _ ` &lt;T&gt; &amp;copy; !</a>
            <p><a href="https://example.com/diagram"><img src="diagram.png" alt="[Flow] &amp; State"></a></p>
        """.trimIndent()

        assertEquals(
            """[\[value\] \\ \* \_ \` \<T\> \&copy; \!](<https://example.com>)""" +
                "\n\n" + """[\[Flow\] \& State](<https://example.com/diagram>)""",
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `preserves URL query fragments parentheses and literal entities`() {
        val html = """
            <a href="https://example.com/api/Map_(type)?a=1&amp;b=2#usage">API</a>
            <p><a href="https://example.com/?literal=&amp;copy;&amp;encoded=%26">Entities</a></p>
        """.trimIndent()

        assertEquals(
            "[API](<https://example.com/api/Map_(type)?a=1&amp;b=2#usage>)\n\n" +
                "[Entities](<https://example.com/?literal=&amp;copy;&amp;encoded=%26>)",
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `keeps relative and fragment links as visible text without resolving a base`() {
        val html = """
            <base href="https://example.com/article/">
            <p><a href="#installation">Installation</a></p>
            <p><a href="/docs">Root</a></p>
            <p><a href="../guide">Parent</a></p>
            <p><a href="another-post">Sibling</a></p>
            <p><a href="?page=2">Query</a></p>
            <p><a href="//example.com/docs">Scheme-relative</a></p>
        """.trimIndent()

        assertEquals(
            "Installation\n\nRoot\n\nParent\n\nSibling\n\nQuery\n\nScheme-relative",
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `keeps malformed unsupported and missing destinations as visible text`() {
        val destinations = listOf(
            "",
            " ",
            "mailto:someone@example.com",
            "javascript:alert(1)",
            "data:text/html,hello",
            "ftp://example.com/file",
            "https://",
            "https:///path",
            "https:example.com",
            "https://example.com/bad%escape",
            "https://example.com/has space",
            "https://[broken",
        )

        destinations.forEach { href ->
            assertEquals(
                "Read `Flow` & [details].",
                TldrInputExtractor.extract("""Read <a href="$href"><code>Flow</code> &amp; [details]</a>."""),
                "href=$href",
            )
        }
        assertEquals("Named anchor", TldrInputExtractor.extract("""<a id="section">Named anchor</a>"""))
    }

    @Test
    fun `preserves block structure within linked content`() {
        val html = """
            <div>Before<a href="https://example.com/article">
                <h2>Article</h2><p>Details with <code>Flow</code>.</p>
                <ul><li>First</li><li>Second</li></ul>
            </a>After</div>
        """.trimIndent()

        assertEquals(
            "Before\n\n## Article\n\nDetails with `Flow`.\n\n- First\n- Second\n\n" +
                "[Link](<https://example.com/article>)\n\nAfter",
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `preserves links inside headings lists and blockquotes`() {
        val html = """
            <h2><a href="https://example.com/api">API</a></h2>
            <ul><li>Use <a href="https://example.com/flow"><code>Flow</code></a></li></ul>
            <blockquote><p>Read <a href="https://example.com/guide">the guide</a>.</p></blockquote>
        """.trimIndent()

        assertEquals(
            "## [API](<https://example.com/api>)\n\n" +
                "- Use [`Flow`](<https://example.com/flow>)\n\n" +
                "> Read [the guide](<https://example.com/guide>).",
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `preserves label spacing and prevents blank lines from breaking links`() {
        val html = """
            <p>Before<a href="https://example.com"> label </a>after.</p>
            <p><a href="https://example.com">First<br><br>Second</a></p>
        """.trimIndent()

        assertEquals(
            "Before[ label ](<https://example.com>)after.\n\n[First Second](<https://example.com>)",
            TldrInputExtractor.extract(html),
        )
    }

    @Test
    fun `omits empty links and links in removed content`() {
        val html = """
            <p>A<a href="https://example.com"> </a>B</p>
            <a href="https://example.com"></a>
            <a href="https://example.com"><img src="decorative.png" alt=""></a>
            <a href="https://example.com"><p> </p></a>
            <a hidden href="https://example.com">Hidden</a>
            <nav><a href="https://example.com">Navigation</a></nav>
        """.trimIndent()

        assertEquals("A B", TldrInputExtractor.extract(html))
    }
}
