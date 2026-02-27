package com.composemarkdown.editor.parser

import com.composemarkdown.editor.model.Block
import com.composemarkdown.editor.model.DocumentModel
import com.composemarkdown.editor.model.Inline
import com.composemarkdown.editor.model.ListItem
import com.composemarkdown.editor.model.TaskItem

object MarkdownParser {
    fun parse(markdown: String): DocumentModel {
        if (markdown.isBlank()) return DocumentModel()
        val lines = markdown.replace("\r\n", "\n").split("\n")
        val blocks = mutableListOf<Block>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                i++
                continue
            }

            val fence = line.trimStart()
            if (fence.startsWith("```")) {
                val language = fence.removePrefix("```").trim().ifBlank { null }
                i++
                val codeLines = mutableListOf<String>()
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeLines += lines[i]
                    i++
                }
                if (i < lines.size) i++
                blocks += Block.CodeBlock(language, codeLines.joinToString("\n"))
                continue
            }

            val heading = Regex("^(#{1,6})\\s+(.+)$").matchEntire(line)
            if (heading != null) {
                val level = heading.groupValues[1].length
                val text = heading.groupValues[2]
                blocks += Block.Heading(level, parseInlines(text))
                i++
                continue
            }

            if (line.trim().matches(Regex("^((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})$"))) {
                blocks += Block.ThematicBreak
                i++
                continue
            }

            if (line.trimStart().startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                    quoteLines += lines[i].trimStart().removePrefix(">").trimStart()
                    i++
                }
                blocks += Block.BlockQuote(parse(quoteLines.joinToString("\n")).blocks)
                continue
            }

            if (line.trimStart().matches(Regex("^[-*] \\[([ xX])] .*$"))) {
                val items = mutableListOf<TaskItem>()
                while (i < lines.size && lines[i].trimStart().matches(Regex("^[-*] \\[([ xX])] .*$"))) {
                    val raw = lines[i].trimStart()
                    val checked = raw[3].lowercaseChar() == 'x'
                    val content = raw.substring(6)
                    items += TaskItem(checked, parseInlines(content))
                    i++
                }
                blocks += Block.TaskList(items)
                continue
            }

            if (line.trimStart().matches(Regex("^[-*+] .*$"))) {
                val items = mutableListOf<ListItem>()
                while (i < lines.size && lines[i].trimStart().matches(Regex("^[-*+] .*$"))) {
                    val content = lines[i].trimStart().substring(2)
                    items += ListItem(parseInlines(content))
                    i++
                }
                blocks += Block.UnorderedList(items)
                continue
            }

            if (line.trimStart().matches(Regex("^\\d+\\. .*$"))) {
                val items = mutableListOf<ListItem>()
                var start = line.trimStart().substringBefore('.').toIntOrNull() ?: 1
                while (i < lines.size && lines[i].trimStart().matches(Regex("^\\d+\\. .*$"))) {
                    val content = lines[i].trimStart().substringAfter(". ")
                    items += ListItem(parseInlines(content))
                    i++
                }
                blocks += Block.OrderedList(start, items)
                continue
            }

            val para = mutableListOf<String>()
            while (i < lines.size && lines[i].isNotBlank() && !isBlockStarter(lines[i])) {
                para += lines[i]
                i++
            }
            blocks += Block.Paragraph(parseInlines(para.joinToString(" ")))
        }

        return DocumentModel(blocks)
    }

    private fun isBlockStarter(line: String): Boolean {
        val t = line.trimStart()
        return t.startsWith("#") ||
            t.startsWith(">") ||
            t.startsWith("```") ||
            t.matches(Regex("^[-*+] .*$")) ||
            t.matches(Regex("^\\d+\\. .*$")) ||
            t.matches(Regex("^[-*] \\[([ xX])] .*$")) ||
            t.matches(Regex("^((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})$"))
    }

    fun parseInlines(text: String): List<Inline> {
        if (text.isEmpty()) return emptyList()
        val out = mutableListOf<Inline>()
        var i = 0
        while (i < text.length) {
            fun findClose(marker: String, from: Int): Int = text.indexOf(marker, from)

            when {
                text.startsWith("![", i) -> {
                    val closeAlt = text.indexOf(']', i + 2)
                    val openUrl = if (closeAlt >= 0) text.indexOf('(', closeAlt) else -1
                    val closeUrl = if (openUrl >= 0) text.indexOf(')', openUrl) else -1
                    if (closeAlt > 0 && openUrl == closeAlt + 1 && closeUrl > openUrl) {
                        out += Inline.Image(text.substring(i + 2, closeAlt), text.substring(openUrl + 1, closeUrl))
                        i = closeUrl + 1
                    } else {
                        out += Inline.Text(text[i].toString())
                        i++
                    }
                }
                text.startsWith("**", i) -> {
                    val end = findClose("**", i + 2)
                    if (end > i + 2) {
                        out += Inline.Bold(parseInlines(text.substring(i + 2, end)))
                        i = end + 2
                    } else { out += Inline.Text("**"); i += 2 }
                }
                text.startsWith("__", i) -> {
                    val end = findClose("__", i + 2)
                    if (end > i + 2) {
                        out += Inline.Bold(parseInlines(text.substring(i + 2, end)))
                        i = end + 2
                    } else { out += Inline.Text("__"); i += 2 }
                }
                text.startsWith("~~", i) -> {
                    val end = findClose("~~", i + 2)
                    if (end > i + 2) {
                        out += Inline.Strikethrough(parseInlines(text.substring(i + 2, end)))
                        i = end + 2
                    } else { out += Inline.Text("~~"); i += 2 }
                }
                text[i] == '*' || text[i] == '_' -> {
                    val marker = text[i].toString()
                    val end = findClose(marker, i + 1)
                    if (end > i + 1) {
                        out += Inline.Italic(parseInlines(text.substring(i + 1, end)))
                        i = end + 1
                    } else { out += Inline.Text(marker); i++ }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        out += Inline.InlineCode(text.substring(i + 1, end))
                        i = end + 1
                    } else { out += Inline.Text("`"); i++ }
                }
                text[i] == '[' -> {
                    val closeText = text.indexOf(']', i + 1)
                    val openUrl = if (closeText >= 0) text.indexOf('(', closeText) else -1
                    val closeUrl = if (openUrl >= 0) text.indexOf(')', openUrl) else -1
                    if (closeText > 0 && openUrl == closeText + 1 && closeUrl > openUrl) {
                        val children = parseInlines(text.substring(i + 1, closeText))
                        out += Inline.Link(children, text.substring(openUrl + 1, closeUrl))
                        i = closeUrl + 1
                    } else { out += Inline.Text("["); i++ }
                }
                else -> {
                    val nextSpecial = listOf("![", "**", "__", "~~", "*", "_", "`", "[")
                        .map { token -> text.indexOf(token, i).takeIf { it >= 0 } ?: text.length }
                        .minOrNull() ?: text.length
                    val chunkEnd = if (nextSpecial == i) i + 1 else nextSpecial
                    out += Inline.Text(text.substring(i, chunkEnd))
                    i = chunkEnd
                }
            }
        }

        return mergeText(out).map { inline ->
            if (inline is Inline.Text && (inline.content.startsWith("http://") || inline.content.startsWith("https://"))) {
                Inline.Link(listOf(inline), inline.content)
            } else inline
        }
    }

    private fun mergeText(nodes: List<Inline>): List<Inline> {
        if (nodes.isEmpty()) return nodes
        val out = mutableListOf<Inline>()
        var currentText: String? = null
        fun flush() {
            currentText?.let { out += Inline.Text(it) }
            currentText = null
        }
        for (node in nodes) {
            if (node is Inline.Text) {
                currentText = (currentText ?: "") + node.content
            } else {
                flush()
                out += node
            }
        }
        flush()
        return out
    }
}
