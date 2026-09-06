package io.github.reactivecircus.kstreamlined.backend.tldr

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element

object TldrInputExtractor {
    private val headingTags = setOf("h1", "h2", "h3", "h4", "h5", "h6")
    private val paragraphTags = setOf("p", "figcaption")
    private val listTags = setOf("ul", "ol")
    private val skippedTags = setOf("br", "hr")
    private val codeLanguages = listOf(
        "kotlin",
        "java",
        "xml",
        "groovy",
        "gradle",
        "bash",
        "shell",
        "json",
        "yaml",
    )
    private val codeLanguagePatterns = codeLanguages.associateWith { language ->
        Regex("""(?<![a-z0-9])${Regex.escape(language)}(?![a-z0-9])""")
    }

    fun extract(html: String): String {
        val body = Ksoup.parseBodyFragment(html).body()
        body.select(NonContentSelector).remove()

        return buildList {
            renderChildren(body, this)
        }.filter(String::isNotBlank)
            .joinToString("\n\n")
            .trim()
    }

    private fun renderChildren(parent: Element, output: MutableList<String>) {
        parent.children().forEach { renderBlock(it, output) }
    }

    private fun renderBlock(element: Element, output: MutableList<String>) {
        val tag = element.tagName().lowercase()
        val rendered = when {
            tag in headingTags -> renderHeading(element, tag)

            tag in paragraphTags -> inlineText(element)

            tag == "pre" -> renderCodeBlock(element)

            tag in listTags -> renderList(element)

            tag == "blockquote" -> renderBlockquote(element)

            tag in skippedTags -> null

            hasBlockDescendant(element) -> {
                renderChildren(element, output)
                null
            }

            else -> inlineText(element)
        }
        rendered?.takeIf(String::isNotBlank)?.let(output::add)
    }

    private fun renderHeading(element: Element, tag: String): String? {
        val level = tag.substring(1).toInt()
        return inlineText(element)
            .takeIf(String::isNotBlank)
            ?.let { "${"#".repeat(level)} $it" }
    }

    private fun renderBlockquote(element: Element): String {
        val nested = buildList {
            renderChildren(element, this)
        }.ifEmpty {
            listOf(inlineText(element))
        }

        return nested.filter(String::isNotBlank)
            .joinToString("\n")
            .lineSequence()
            .joinToString("\n") { "> $it" }
    }

    private fun renderCodeBlock(element: Element): String? {
        val code = element.wholeText().trim('\n', '\r').trimEnd()
        if (code.isBlank()) return null

        return "```${detectLanguage(element)}\n$code\n```"
    }

    private fun detectLanguage(element: Element): String {
        val hints = buildString {
            element.attributes().forEach { append(it.value).append(' ') }
            element.selectFirst("code")?.attributes()?.forEach { append(it.value).append(' ') }
        }.lowercase()

        return codeLanguagePatterns.entries
            .firstOrNull { (_, pattern) -> pattern.containsMatchIn(hints) }
            ?.key
            .orEmpty()
    }

    private fun renderList(list: Element, depth: Int = 0): String {
        val ordered = list.tagName().equals("ol", ignoreCase = true)
        val indent = "  ".repeat(depth)

        return buildList {
            list.children()
                .filter { it.tagName().equals("li", ignoreCase = true) }
                .forEachIndexed { index, item ->
                    val nestedLists = item.children()
                        .filter { it.tagName().lowercase() in listTags }
                    val itemWithoutNestedLists = item.clone()
                        .also { it.select("ul,ol").remove() }
                    val marker = if (ordered) "${index + 1}. " else "- "

                    inlineText(itemWithoutNestedLists)
                        .takeIf(String::isNotBlank)
                        ?.let { add("$indent$marker$it") }

                    nestedLists.mapTo(this) { renderList(it, depth + 1) }
                }
        }.filter(String::isNotBlank)
            .joinToString("\n")
    }

    private fun hasBlockDescendant(element: Element): Boolean {
        return element.selectFirst(BlockSelector) != null
    }

    private fun inlineText(element: Element): String {
        val clone = element.clone()
        clone.select("code").forEach { code ->
            code.text()
                .takeIf(String::isNotBlank)
                ?.let { code.text("`$it`") }
        }
        return clone.text().trim()
    }
}

private const val NonContentSelector =
    "script,style,noscript,iframe,svg,form,button,nav,aside,template,[hidden],[aria-hidden=true]"

private const val BlockSelector =
    "h1,h2,h3,h4,h5,h6,p,pre,ul,ol,blockquote,div,section,article,figure,table"
