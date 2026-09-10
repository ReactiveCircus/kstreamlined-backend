package io.github.reactivecircus.kstreamlined.backend.tldr

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode

object TldrInputExtractor {
    private val headingTags = setOf("h1", "h2", "h3", "h4", "h5", "h6")
    private val listTags = setOf("ul", "ol")
    private val blockTags = headingTags + listTags + setOf(
        "p", "figcaption", "pre", "blockquote", "div", "section", "article", "figure",
        "main", "header", "footer", "table", "thead", "tbody", "tfoot", "tr", "td", "th",
        "dl", "dt", "dd", "details", "summary", "hr",
    )
    private val whitespace = Regex("[\\s\\u00a0]+")
    private val horizontalWhitespace = Regex("[ \\t\\u00a0]+")
    private val backticks = Regex("`+")
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

        return renderChildren(body).joinToString("\n\n") { it.text }
    }

    private fun renderChildren(parent: Element): List<Block> {
        val output = mutableListOf<Block>()
        val inline = StringBuilder()
        parent.childNodes().forEach { renderNode(it, output, inline) }
        flushInline(output, inline)
        return output
    }

    private fun renderNode(node: Node, output: MutableList<Block>, inline: StringBuilder) {
        when (node) {
            is TextNode -> inline.append(node.getWholeText().replace(whitespace, " "))

            is Element -> when (node.tagName().lowercase()) {
                in blockTags -> {
                    flushInline(output, inline)
                    output.addAll(renderBlock(node))
                }

                "code" -> inline.append(renderInlineCode(node))

                "br" -> inline.append('\n')

                "img" -> inline.append(node.attr("alt").replace(whitespace, " "))

                else -> node.childNodes().forEach { renderNode(it, output, inline) }
            }
        }
    }

    private fun flushInline(output: MutableList<Block>, inline: StringBuilder) {
        val text = inline.toString().lineSequence()
            .joinToString("\n") { it.replace(horizontalWhitespace, " ").trim() }
            .trim()
        if (text.isNotBlank()) output.add(Block(text))
        inline.clear()
    }

    private fun renderBlock(element: Element): List<Block> {
        val tag = element.tagName().lowercase()
        return when (tag) {
            in headingTags -> {
                val text = renderChildren(element).joinToString(" ") { it.text }
                if (text.isBlank()) emptyList() else listOf(Block("${"#".repeat(tag.substring(1).toInt())} $text"))
            }

            "pre" -> listOfNotNull(renderCodeBlock(element)?.let { Block(it) })

            in listTags -> {
                val text = renderList(element)
                if (text.isBlank()) emptyList() else listOf(Block(text, isList = true))
            }

            "blockquote" -> {
                val text = renderChildren(element).joinToString("\n") { it.text }
                if (text.isBlank()) emptyList() else listOf(Block(text.lineSequence().joinToString("\n") { "> $it" }))
            }

            "hr" -> emptyList()

            else -> renderChildren(element)
        }
    }

    private fun renderCodeBlock(element: Element): String? {
        val code = element.wholeText().trim('\n', '\r')
        if (code.isBlank()) return null

        val fence = "`".repeat(backtickDelimiterLength(code).coerceAtLeast(3))
        return "$fence${detectLanguage(element)}\n$code\n$fence"
    }

    private fun renderInlineCode(element: Element): String {
        val code = element.text().trim()
        if (code.isBlank()) return ""
        val fence = "`".repeat(backtickDelimiterLength(code))
        val padding = if (code.startsWith('`') || code.endsWith('`')) " " else ""
        return "$fence$padding$code$padding$fence"
    }

    private fun backtickDelimiterLength(code: String): Int {
        return (backticks.findAll(code).maxOfOrNull { it.value.length } ?: 0) + 1
    }

    private fun detectLanguage(element: Element): String {
        val hints = buildString {
            listOfNotNull(element, element.selectFirst("code")).forEach { node ->
                append(node.className()).append(' ')
                append(node.attr("data-enlighter-language")).append(' ')
                append(node.attr("data-language")).append(' ')
                append(node.attr("lang")).append(' ')
            }
        }.lowercase()

        return codeLanguagePatterns.entries
            .firstOrNull { (_, pattern) -> pattern.containsMatchIn(hints) }
            ?.key
            .orEmpty()
    }

    private fun renderList(list: Element): String {
        val ordered = list.tagName().equals("ol", ignoreCase = true)

        return list.children()
            .filter { it.tagName().equals("li", ignoreCase = true) }
            .mapIndexedNotNull { index, item ->
                val blocks = renderChildren(item)
                if (blocks.isEmpty()) return@mapIndexedNotNull null
                val marker = if (ordered) "${index + 1}. " else "- "
                renderListItem(blocks, marker)
            }
            .joinToString("\n")
    }

    private fun renderListItem(blocks: List<Block>, marker: String): String {
        val indent = " ".repeat(marker.length)
        return buildString {
            blocks.forEachIndexed { index, block ->
                if (index == 0) {
                    append(marker)
                    if (block.isList) {
                        setLength(length - 1)
                        append('\n').append(indent)
                    }
                } else {
                    append(if (block.isList) "\n" else "\n\n").append(indent)
                }
                val indented = block.text.lineSequence()
                    .joinToString("\n") { if (it.isBlank()) "" else "$indent$it" }
                append(indented.removePrefix(indent))
            }
        }
    }

    private class Block(val text: String, val isList: Boolean = false)
}

private const val NonContentSelector =
    "script,style,noscript,iframe,svg,form,button,nav,aside,template,[hidden],[aria-hidden=true]"
