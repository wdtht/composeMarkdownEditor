package com.composemarkdown.editor.serializer

import com.composemarkdown.editor.model.Block
import com.composemarkdown.editor.model.DocumentModel
import com.composemarkdown.editor.model.Inline

object MarkdownSerializer {
    fun serialize(document: DocumentModel): String {
        return document.blocks.joinToString("\n\n") { block -> serializeBlock(block) }
    }

    private fun serializeBlock(block: Block): String = when (block) {
        is Block.Paragraph -> serializeInlines(block.inlines)
        is Block.Heading -> "${"#".repeat(block.level.coerceIn(1, 6))} ${serializeInlines(block.inlines)}"
        is Block.CodeBlock -> buildString {
            append("```")
            append(block.language ?: "")
            append('\n')
            append(block.content)
            append("\n```")
        }
        is Block.BlockQuote -> serialize(DocumentModel(block.blocks))
            .lineSequence()
            .joinToString("\n") { "> $it" }
        is Block.UnorderedList -> block.items.joinToString("\n") { "- ${serializeInlines(it.inlines)}" }
        is Block.OrderedList -> block.items.mapIndexed { idx, item ->
            "${block.start + idx}. ${serializeInlines(item.inlines)}"
        }.joinToString("\n")
        is Block.TaskList -> block.items.joinToString("\n") {
            "- [${if (it.checked) "x" else " "}] ${serializeInlines(it.inlines)}"
        }
        Block.ThematicBreak -> "---"
    }

    fun serializeInlines(inlines: List<Inline>): String = inlines.joinToString(separator = "") {
        when (it) {
            is Inline.Text -> it.content
            is Inline.Bold -> "**${serializeInlines(it.children)}**"
            is Inline.Italic -> "*${serializeInlines(it.children)}*"
            is Inline.Strikethrough -> "~~${serializeInlines(it.children)}~~"
            is Inline.InlineCode -> "`${it.code}`"
            is Inline.Link -> "[${serializeInlines(it.children)}](${it.url})"
            is Inline.Image -> "![${it.alt}](${it.url})"
        }
    }
}
