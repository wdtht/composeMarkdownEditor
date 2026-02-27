package editor.parser

import editor.model.Block
import editor.model.DocumentModel
import editor.model.Inline
import editor.model.ListItem
import editor.model.TaskItem

object MarkdownParser {
    private val headingRegex = Regex("^(#{1,6})\\s+(.+)$")
    private val unorderedRegex = Regex("^[-*] ")
    private val orderedRegex = Regex("^\\d+\\. ")

    fun parse(markdown: String): DocumentModel {
        val lines = markdown.replace("\r\n", "\n").split("\n")
        val blocks = mutableListOf<Block>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                i++
                continue
            }

            if (line == "---" || line == "***" || line == "___") {
                blocks += Block.ThematicBreak
                i++
                continue
            }

            if (line.startsWith("```")) {
                val lang = line.removePrefix("```").trim().ifBlank { null }
                i++
                val codeLines = mutableListOf<String>()
                while (i < lines.size && !lines[i].startsWith("```")) {
                    codeLines += lines[i]
                    i++
                }
                if (i < lines.size && lines[i].startsWith("```")) i++
                blocks += Block.CodeBlock(lang, codeLines.joinToString("\n"))
                continue
            }

            val heading = headingRegex.matchEntire(line)
            if (heading != null) {
                blocks += Block.Heading(heading.groupValues[1].length, parseInline(heading.groupValues[2]))
                i++
                continue
            }

            if (line.startsWith("> ")) {
                val quotedLines = mutableListOf<String>()
                while (i < lines.size && lines[i].startsWith("> ")) {
                    quotedLines += lines[i].removePrefix("> ")
                    i++
                }
                blocks += Block.Blockquote(parse(quotedLines.joinToString("\n")).blocks)
                continue
            }

            if (isTaskLine(line)) {
                val items = mutableListOf<TaskItem>()
                while (i < lines.size && isTaskLine(lines[i])) {
                    val raw = lines[i]
                    val checked = raw[3].equals('x', ignoreCase = true)
                    items += TaskItem(checked, parseInline(raw.substring(6)))
                    i++
                }
                blocks += Block.TaskList(items)
                continue
            }

            if (unorderedRegex.containsMatchIn(line)) {
                val items = mutableListOf<ListItem>()
                while (i < lines.size && unorderedRegex.containsMatchIn(lines[i])) {
                    items += ListItem(parseInline(lines[i].substring(2)))
                    i++
                }
                blocks += Block.UnorderedList(items)
                continue
            }

            if (orderedRegex.containsMatchIn(line)) {
                val items = mutableListOf<ListItem>()
                while (i < lines.size && orderedRegex.containsMatchIn(lines[i])) {
                    items += ListItem(parseInline(lines[i].replaceFirst(orderedRegex, "")))
                    i++
                }
                blocks += Block.OrderedList(items)
                continue
            }

            val paragraphLines = mutableListOf<String>()
            while (i < lines.size && lines[i].isNotBlank() && !isBlockStarter(lines[i])) {
                paragraphLines += lines[i]
                i++
            }
            blocks += Block.Paragraph(parseInline(paragraphLines.joinToString(" ")))
        }

        return DocumentModel(blocks)
    }

    private fun isBlockStarter(line: String): Boolean {
        return line == "---" || line == "***" || line == "___" ||
            line.startsWith("```") ||
            headingRegex.containsMatchIn(line) ||
            line.startsWith("> ") ||
            isTaskLine(line) ||
            unorderedRegex.containsMatchIn(line) ||
            orderedRegex.containsMatchIn(line)
    }


    private fun isTaskLine(line: String): Boolean {
        return line.length >= 6 &&
            line[0] == '-' && line[1] == ' ' && line[2] == '[' &&
            (line[3] == ' ' || line[3] == 'x' || line[3] == 'X') &&
            line[4] == ']' && line[5] == ' '
    }

    fun parseInline(text: String): List<Inline> {
        if (text.isEmpty()) return emptyList()
        val result = mutableListOf<Inline>()
        var index = 0

        while (index < text.length) {
            when {
                text.startsWith("**", index) -> {
                    val end = text.indexOf("**", index + 2)
                    if (end > index + 1) {
                        result += Inline.Bold(parseInline(text.substring(index + 2, end)))
                        index = end + 2
                    } else {
                        result += Inline.Text("**")
                        index += 2
                    }
                }
                text.startsWith("*", index) -> {
                    val end = text.indexOf("*", index + 1)
                    if (end > index) {
                        result += Inline.Italic(parseInline(text.substring(index + 1, end)))
                        index = end + 1
                    } else {
                        result += Inline.Text("*")
                        index += 1
                    }
                }
                text.startsWith("~~", index) -> {
                    val end = text.indexOf("~~", index + 2)
                    if (end > index + 1) {
                        result += Inline.Strikethrough(parseInline(text.substring(index + 2, end)))
                        index = end + 2
                    } else {
                        result += Inline.Text("~~")
                        index += 2
                    }
                }
                text.startsWith("`", index) -> {
                    val end = text.indexOf("`", index + 1)
                    if (end > index) {
                        result += Inline.InlineCode(text.substring(index + 1, end))
                        index = end + 1
                    } else {
                        result += Inline.Text("`")
                        index += 1
                    }
                }
                text.startsWith("[", index) -> {
                    val closeBracket = text.indexOf("](", index + 1)
                    val closeParen = if (closeBracket > index) text.indexOf(")", closeBracket + 2) else -1
                    if (closeBracket > index && closeParen > closeBracket) {
                        val label = text.substring(index + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        result += Inline.Link(parseInline(label), url)
                        index = closeParen + 1
                    } else {
                        result += Inline.Text(text[index].toString())
                        index++
                    }
                }
                else -> {
                    val next = findNextMarker(text, index)
                    result += Inline.Text(text.substring(index, next))
                    index = next
                }
            }
        }

        return mergeAdjacentText(result)
    }

    private fun findNextMarker(text: String, start: Int): Int {
        val markers = listOf("**", "*", "~~", "`", "[")
        return markers.mapNotNull { marker -> text.indexOf(marker, start).takeIf { it >= 0 } }.minOrNull() ?: text.length
    }

    private fun mergeAdjacentText(parts: List<Inline>): List<Inline> {
        val merged = mutableListOf<Inline>()
        for (part in parts) {
            val previous = merged.lastOrNull()
            if (previous is Inline.Text && part is Inline.Text) {
                merged[merged.lastIndex] = Inline.Text(previous.value + part.value)
            } else {
                merged += part
            }
        }
        return merged
    }
}
