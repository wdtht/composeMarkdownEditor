package editor.model

data class DocumentModel(
    val blocks: List<Block>
)

sealed interface Block {
    data class Paragraph(val inlines: List<Inline>) : Block
    data class Heading(val level: Int, val inlines: List<Inline>) : Block
    data class Blockquote(val blocks: List<Block>) : Block
    data class CodeBlock(val language: String?, val code: String) : Block
    data class UnorderedList(val items: List<ListItem>) : Block
    data class OrderedList(val items: List<ListItem>) : Block
    data class TaskList(val items: List<TaskItem>) : Block
    data object ThematicBreak : Block
}

data class ListItem(val inlines: List<Inline>)
data class TaskItem(val checked: Boolean, val inlines: List<Inline>)

sealed interface Inline {
    data class Text(val value: String) : Inline
    data class Bold(val children: List<Inline>) : Inline
    data class Italic(val children: List<Inline>) : Inline
    data class Strikethrough(val children: List<Inline>) : Inline
    data class InlineCode(val value: String) : Inline
    data class Link(val text: List<Inline>, val url: String) : Inline
}
