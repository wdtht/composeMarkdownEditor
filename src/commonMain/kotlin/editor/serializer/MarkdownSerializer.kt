package editor.serializer

import editor.model.Block
import editor.model.DocumentModel
import editor.model.Inline

object MarkdownSerializer {
    fun serialize(document: DocumentModel): String {
        return document.blocks.joinToString("\n\n") { block ->
            serializeBlock(block)
        }
    }

    private fun serializeBlock(block: Block): String = when (block) {
        is Block.Paragraph -> serializeInline(block.inlines)
        is Block.Heading -> "${"#".repeat(block.level)} ${serializeInline(block.inlines)}"
        is Block.Blockquote -> block.blocks.joinToString("\n") { "> ${serializeBlock(it)}" }
        is Block.CodeBlock -> {
            val fence = buildString {
                append("```")
                if (!block.language.isNullOrBlank()) append(block.language)
            }
            "$fence\n${block.code}\n```"
        }
        is Block.UnorderedList -> block.items.joinToString("\n") { "- ${serializeInline(it.inlines)}" }
        is Block.OrderedList -> block.items.mapIndexed { idx, item ->
            "${idx + 1}. ${serializeInline(item.inlines)}"
        }.joinToString("\n")
        is Block.TaskList -> block.items.joinToString("\n") {
            "- [${if (it.checked) "x" else " "}] ${serializeInline(it.inlines)}"
        }
        Block.ThematicBreak -> "---"
    }

    private fun serializeInline(inlines: List<Inline>): String {
        return inlines.joinToString("") { inline ->
            when (inline) {
                is Inline.Text -> inline.value
                is Inline.Bold -> "**${serializeInline(inline.children)}**"
                is Inline.Italic -> "*${serializeInline(inline.children)}*"
                is Inline.Strikethrough -> "~~${serializeInline(inline.children)}~~"
                is Inline.InlineCode -> "`${inline.value}`"
                is Inline.Link -> "[${serializeInline(inline.text)}](${inline.url})"
            }
        }
    }
}
