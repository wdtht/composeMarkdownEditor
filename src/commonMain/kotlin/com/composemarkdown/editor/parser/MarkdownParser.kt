package com.composemarkdown.editor.parser

import com.composemarkdown.editor.model.Block
import com.composemarkdown.editor.model.DocumentModel
import com.composemarkdown.editor.model.Inline
import com.composemarkdown.editor.model.ListItem
import com.composemarkdown.editor.model.TableAlignment
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

            val trimmed = line.trimStart()

            if (trimmed == "$$") {
                i++
                val mathLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim() != "$$") {
                    mathLines += lines[i]
                    i++
                }
                if (i < lines.size) i++
                blocks += Block.MathBlock(mathLines.joinToString("\n"))
                continue
            }

            if (trimmed.startsWith("<details>")) {
                i++
                val summaryLine = lines.getOrNull(i)?.trim().orEmpty()
                val summary = Regex("<summary>(.*?)</summary>").find(summaryLine)?.groupValues?.get(1).orEmpty()
                if (summaryLine.startsWith("<summary>")) i++
                val bodyLines = mutableListOf<String>()
                while (i < lines.size && !lines[i].trim().startsWith("</details>")) {
                    bodyLines += lines[i]
                    i++
                }
                if (i < lines.size) i++
                blocks += Block.DetailsBlock(summary, parse(bodyLines.joinToString("\n")).blocks)
                continue
            }

            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                val fenceMarker = if (trimmed.startsWith("~~~")) "~~~" else "```"
                val language = trimmed.removePrefix(fenceMarker).trim().ifBlank { null }
                i++
                val codeLines = mutableListOf<String>()
                while (i < lines.size && !lines[i].trimStart().startsWith(fenceMarker)) {
                    codeLines += lines[i]
                    i++
                }
                if (i < lines.size) i++
                val content = codeLines.joinToString("\n")
                blocks += if (language == "mermaid") Block.MermaidBlock(content) else Block.CodeBlock(language, content)
                continue
            }

            val atxHeading = Regex("^(#{1,6})\\s+(.+?)\\s*#*$").matchEntire(line)
            if (atxHeading != null) {
                blocks += Block.Heading(atxHeading.groupValues[1].length, parseInlines(atxHeading.groupValues[2]))
                i++
                continue
            }

            if (i + 1 < lines.size) {
                val next = lines[i + 1].trim()
                if (line.isNotBlank() && next.matches(Regex("^=+$"))) {
                    blocks += Block.Heading(1, parseInlines(line.trim()))
                    i += 2
                    continue
                }
                if (line.isNotBlank() && next.matches(Regex("^-+$")) && !line.contains('|')) {
                    blocks += Block.Heading(2, parseInlines(line.trim()))
                    i += 2
                    continue
                }
            }

            if (line.trim().matches(Regex("^((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})$"))) {
                blocks += Block.ThematicBreak
                i++
                continue
            }

            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                    quoteLines += lines[i].trimStart().removePrefix(">").trimStart()
                    i++
                }
                blocks += Block.BlockQuote(parse(quoteLines.joinToString("\n")).blocks)
                continue
            }

            val footnoteDef = Regex("^\\[\\^([^]]+)]:(.*)$").matchEntire(line.trim())
            if (footnoteDef != null) {
                val id = footnoteDef.groupValues[1]
                val first = footnoteDef.groupValues[2].trimStart()
                i++
                val body = mutableListOf<String>()
                if (first.isNotBlank()) body += first
                while (i < lines.size && (lines[i].startsWith("    ") || lines[i].startsWith("\t"))) {
                    body += lines[i].trimStart()
                    i++
                }
                blocks += Block.FootnoteDefinition(id, parse(body.joinToString("\n")).blocks.ifEmpty {
                    listOf(Block.Paragraph(parseInlines("")))
                })
                continue
            }

            val blockImage = Regex("^!\\[(.*)]\\((.+)\\)\\s*$").matchEntire(line.trim())
            if (blockImage != null) {
                blocks += Block.ImageBlock(blockImage.groupValues[1], blockImage.groupValues[2])
                i++
                continue
            }

            if (isTableHeader(lines, i)) {
                val headers = splitTableLine(lines[i])
                val alignments = normalizeAlignments(headers.size, parseAlignments(splitTableLine(lines[i + 1])))
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].contains('|') && lines[i].isNotBlank()) {
                    rows += normalizeRow(headers.size, splitTableLine(lines[i]))
                    i++
                }
                blocks += Block.Table(headers, alignments, rows)
                continue
            }

            if (trimmed.matches(Regex("^[-*] \\[([ xX])] .*$"))) {
                val items = mutableListOf<TaskItem>()
                while (i < lines.size && lines[i].trimStart().matches(Regex("^[-*] \\[([ xX])] .*$"))) {
                    val raw = lines[i].trimStart()
                    items += TaskItem(raw[3].lowercaseChar() == 'x', parseInlines(raw.substring(6)))
                    i++
                }
                blocks += Block.TaskList(items)
                continue
            }

            if (trimmed.matches(Regex("^[-*+] .*$"))) {
                val items = mutableListOf<ListItem>()
                while (i < lines.size && lines[i].trimStart().matches(Regex("^[-*+] .*$"))) {
                    items += ListItem(parseInlines(lines[i].trimStart().substring(2)))
                    i++
                }
                blocks += Block.UnorderedList(items)
                continue
            }

            if (trimmed.matches(Regex("^\\d+\\. .*$"))) {
                val items = mutableListOf<ListItem>()
                val start = trimmed.substringBefore('.').toIntOrNull() ?: 1
                while (i < lines.size && lines[i].trimStart().matches(Regex("^\\d+\\. .*$"))) {
                    items += ListItem(parseInlines(lines[i].trimStart().substringAfter(". ")))
                    i++
                }
                blocks += Block.OrderedList(start, items)
                continue
            }

            val para = mutableListOf<String>()
            while (i < lines.size && lines[i].isNotBlank() && !isBlockStarter(lines, i)) {
                para += lines[i]
                i++
            }
            blocks += Block.Paragraph(parseInlines(para.joinToString(" ")))
        }

        return DocumentModel(blocks)
    }

    private fun isTableHeader(lines: List<String>, index: Int): Boolean {
        if (index + 1 >= lines.size) return false
        val header = lines[index]
        val separator = lines[index + 1].trim()
        if (!header.contains('|')) return false
        return separator.matches(Regex("^\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)+\\|?$"))
    }

    private fun splitTableLine(line: String): List<String> {
        val content = line.trim().trim('|')
        if (content.isEmpty()) return emptyList()

        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (ch in content) {
            if (escaped) {
                current.append(ch)
                escaped = false
                continue
            }
            when (ch) {
                '\\' -> escaped = true
                '|' -> {
                    cells += current.toString().trim()
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        if (escaped) current.append('\\')
        cells += current.toString().trim()
        return cells
    }

    private fun parseAlignments(tokens: List<String>): List<TableAlignment> = tokens.map {
        when {
            it.startsWith(":") && it.endsWith(":") -> TableAlignment.CENTER
            it.startsWith(":") -> TableAlignment.LEFT
            it.endsWith(":") -> TableAlignment.RIGHT
            else -> TableAlignment.NONE
        }
    }

    private fun normalizeAlignments(expectedSize: Int, parsed: List<TableAlignment>): List<TableAlignment> {
        if (expectedSize <= 0) return emptyList()
        if (parsed.size == expectedSize) return parsed
        return List(expectedSize) { idx -> parsed.getOrElse(idx) { TableAlignment.NONE } }
    }

    private fun normalizeRow(expectedSize: Int, row: List<String>): List<String> {
        if (expectedSize <= 0) return emptyList()
        if (row.size == expectedSize) return row
        return List(expectedSize) { idx -> row.getOrElse(idx) { "" } }
    }

    private fun isBlockStarter(lines: List<String>, index: Int): Boolean {
        val t = lines[index].trimStart()
        return t.startsWith("#") ||
            t.startsWith(">") ||
            t.startsWith("```") ||
            t.startsWith("~~~") ||
            t == "$$" ||
            t.startsWith("<details>") ||
            t.matches(Regex("^[-*+] .*$")) ||
            t.matches(Regex("^\\d+\\. .*$")) ||
            t.matches(Regex("^[-*] \\[([ xX])] .*$")) ||
            t.matches(Regex("^((\\*\\s*){3,}|(-\\s*){3,}|(_\\s*){3,})$")) ||
            t.matches(Regex("^!\\[.*]\\(.+\\)$")) ||
            t.matches(Regex("^\\[\\^[^]]+]:.*$")) ||
            isTableHeader(lines, index)
    }

    fun parseInlines(text: String): List<Inline> {
        if (text.isEmpty()) return emptyList()
        val out = mutableListOf<Inline>()
        var i = 0

        while (i < text.length) {
            fun findClose(marker: String, from: Int): Int = text.indexOf(marker, from)

            when {
                text.startsWith("==", i) -> {
                    val end = findClose("==", i + 2)
                    if (end > i + 2) {
                        out += Inline.Highlight(parseInlines(text.substring(i + 2, end)))
                        i = end + 2
                    } else {
                        out += Inline.Text("==")
                        i += 2
                    }
                }
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
                text.startsWith("[^", i) -> {
                    val end = text.indexOf(']', i + 2)
                    if (end > i + 2) {
                        out += Inline.FootnoteReference(text.substring(i + 2, end))
                        i = end + 1
                    } else {
                        out += Inline.Text("[")
                        i++
                    }
                }
                text.startsWith("**", i) || text.startsWith("__", i) -> {
                    val marker = text.substring(i, i + 2)
                    val end = findClose(marker, i + 2)
                    if (end > i + 2) {
                        out += Inline.Bold(parseInlines(text.substring(i + 2, end)))
                        i = end + 2
                    } else {
                        out += Inline.Text(marker)
                        i += 2
                    }
                }
                text.startsWith("~~", i) -> {
                    val end = findClose("~~", i + 2)
                    if (end > i + 2) {
                        out += Inline.Strikethrough(parseInlines(text.substring(i + 2, end)))
                        i = end + 2
                    } else {
                        out += Inline.Text("~~")
                        i += 2
                    }
                }
                text[i] == '$' -> {
                    val end = text.indexOf('$', i + 1)
                    if (end > i + 1) {
                        out += Inline.InlineMath(text.substring(i + 1, end))
                        i = end + 1
                    } else {
                        out += Inline.Text("$")
                        i++
                    }
                }
                text[i] == '^' -> {
                    val end = text.indexOf('^', i + 1)
                    if (end > i + 1) {
                        out += Inline.Superscript(parseInlines(text.substring(i + 1, end)))
                        i = end + 1
                    } else {
                        out += Inline.Text("^")
                        i++
                    }
                }
                text[i] == '~' && !text.startsWith("~~", i) -> {
                    val end = text.indexOf('~', i + 1)
                    if (end > i + 1) {
                        out += Inline.Subscript(parseInlines(text.substring(i + 1, end)))
                        i = end + 1
                    } else {
                        out += Inline.Text("~")
                        i++
                    }
                }
                text[i] == '*' || text[i] == '_' -> {
                    val marker = text[i].toString()
                    val end = findClose(marker, i + 1)
                    if (end > i + 1) {
                        out += Inline.Italic(parseInlines(text.substring(i + 1, end)))
                        i = end + 1
                    } else {
                        out += Inline.Text(marker)
                        i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        out += Inline.InlineCode(text.substring(i + 1, end))
                        i = end + 1
                    } else {
                        out += Inline.Text("`")
                        i++
                    }
                }
                text[i] == '[' -> {
                    val closeText = text.indexOf(']', i + 1)
                    val openUrl = if (closeText >= 0) text.indexOf('(', closeText) else -1
                    val closeUrl = if (openUrl >= 0) text.indexOf(')', openUrl) else -1
                    if (closeText > 0 && openUrl == closeText + 1 && closeUrl > openUrl) {
                        out += Inline.Link(parseInlines(text.substring(i + 1, closeText)), text.substring(openUrl + 1, closeUrl))
                        i = closeUrl + 1
                    } else {
                        out += Inline.Text("[")
                        i++
                    }
                }
                else -> {
                    val nextSpecial = listOf("==", "![", "[^", "**", "__", "~~", "*", "_", "`", "[", "$", "^", "~")
                        .map { token -> text.indexOf(token, i).takeIf { it >= 0 } ?: text.length }
                        .minOrNull() ?: text.length
                    val chunkEnd = if (nextSpecial == i) i + 1 else nextSpecial
                    out += Inline.Text(text.substring(i, chunkEnd))
                    i = chunkEnd
                }
            }
        }

        return autoLink(mergeText(out))
    }

    private fun autoLink(nodes: List<Inline>): List<Inline> {
        val urlRegex = Regex("https?://[^\\s)]+")
        val trailing = setOf('.', ',', ';', '!', '?', ':')
        val out = mutableListOf<Inline>()
        for (node in nodes) {
            if (node !is Inline.Text) {
                out += node
                continue
            }
            var cursor = 0
            for (match in urlRegex.findAll(node.content)) {
                if (match.range.first > cursor) {
                    out += Inline.Text(node.content.substring(cursor, match.range.first))
                }
                var url = match.value
                while (url.isNotEmpty() && url.last() in trailing) {
                    url = url.dropLast(1)
                }
                out += Inline.Link(listOf(Inline.Text(url)), url)
                val consumedEnd = match.range.first + url.length
                if (consumedEnd <= match.range.last + 1) {
                    out += Inline.Text(node.content.substring(consumedEnd, match.range.last + 1))
                }
                cursor = match.range.last + 1
            }
            if (cursor < node.content.length) {
                out += Inline.Text(node.content.substring(cursor))
            }
        }
        return out
    }

    private fun mergeText(nodes: List<Inline>): List<Inline> {
        if (nodes.isEmpty()) return nodes
        val out = mutableListOf<Inline>()
        var currentText: String? = null
        fun flush() {
            currentText?.takeIf { it.isNotEmpty() }?.let { out += Inline.Text(it) }
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
