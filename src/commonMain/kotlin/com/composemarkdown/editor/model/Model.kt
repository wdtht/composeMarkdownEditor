package com.composemarkdown.editor.model

data class DocumentModel(
    val blocks: List<Block> = emptyList(),
)

sealed interface Block {
    data class Paragraph(val inlines: List<Inline>) : Block
    data class Heading(val level: Int, val inlines: List<Inline>) : Block
    data class CodeBlock(val language: String? = null, val content: String) : Block
    data class BlockQuote(val blocks: List<Block>) : Block
    data class UnorderedList(val items: List<ListItem>) : Block
    data class OrderedList(val start: Int = 1, val items: List<ListItem>) : Block
    data class TaskList(val items: List<TaskItem>) : Block
    data object ThematicBreak : Block
}

data class ListItem(
    val inlines: List<Inline>,
)

data class TaskItem(
    val checked: Boolean,
    val inlines: List<Inline>,
)

sealed interface Inline {
    data class Text(val content: String) : Inline
    data class Bold(val children: List<Inline>) : Inline
    data class Italic(val children: List<Inline>) : Inline
    data class Strikethrough(val children: List<Inline>) : Inline
    data class InlineCode(val code: String) : Inline
    data class Link(val children: List<Inline>, val url: String) : Inline
    data class Image(val alt: String, val url: String) : Inline
}
