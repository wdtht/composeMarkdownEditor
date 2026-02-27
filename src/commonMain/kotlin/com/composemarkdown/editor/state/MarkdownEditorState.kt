package com.composemarkdown.editor.state

import com.composemarkdown.editor.model.Block
import com.composemarkdown.editor.model.DocumentModel
import com.composemarkdown.editor.model.Inline
import com.composemarkdown.editor.parser.MarkdownParser
import com.composemarkdown.editor.serializer.MarkdownSerializer

class MarkdownEditorState(initialMarkdown: String = "") {
    private val undoStack = ArrayDeque<DocumentModel>()
    private val redoStack = ArrayDeque<DocumentModel>()

    var document: DocumentModel = MarkdownParser.parse(initialMarkdown)
        private set

    fun setMarkdown(markdown: String) {
        pushUndo()
        document = MarkdownParser.parse(markdown)
        redoStack.clear()
    }

    fun getMarkdown(): String = MarkdownSerializer.serialize(document)

    fun insertBlock(block: Block) {
        pushUndo()
        document = document.copy(blocks = document.blocks + block)
        redoStack.clear()
    }

    fun undo(): Boolean {
        val prev = undoStack.removeLastOrNull() ?: return false
        redoStack.addLast(document)
        document = prev
        return true
    }

    fun redo(): Boolean {
        val next = redoStack.removeLastOrNull() ?: return false
        undoStack.addLast(document)
        document = next
        return true
    }

    fun toggleBold() {
        pushUndo()
        val blocks = document.blocks.toMutableList()
        val idx = blocks.indexOfFirst { it is Block.Paragraph }
        if (idx >= 0) {
            val para = blocks[idx] as Block.Paragraph
            val hasBold = para.inlines.any { it is Inline.Bold }
            blocks[idx] = if (hasBold) {
                Block.Paragraph(unwrapBold(para.inlines))
            } else {
                Block.Paragraph(listOf(Inline.Bold(para.inlines.ifEmpty { listOf(Inline.Text("")) })))
            }
            document = document.copy(blocks = blocks)
            redoStack.clear()
        }
    }

    private fun unwrapBold(inlines: List<Inline>): List<Inline> {
        return inlines.flatMap {
            if (it is Inline.Bold) it.children else listOf(it)
        }
    }

    private fun pushUndo() {
        undoStack.addLast(document)
        if (undoStack.size > 100) undoStack.removeFirst()
    }
}

fun rememberMarkdownEditorState(initialMarkdown: String = ""): MarkdownEditorState {
    return MarkdownEditorState(initialMarkdown)
}
