package com.composemarkdown.editor.state

import com.composemarkdown.editor.model.Block
import com.composemarkdown.editor.model.DocumentModel
import com.composemarkdown.editor.model.Inline
import com.composemarkdown.editor.model.TaskItem
import com.composemarkdown.editor.parser.MarkdownParser
import com.composemarkdown.editor.serializer.HtmlSerializer
import com.composemarkdown.editor.serializer.MarkdownSerializer

class MarkdownEditorState(initialMarkdown: String = "") {
    private val undoStack = ArrayDeque<DocumentModel>()
    private val redoStack = ArrayDeque<DocumentModel>()
    private val listeners = mutableSetOf<(DocumentModel) -> Unit>()

    var document: DocumentModel = MarkdownParser.parse(initialMarkdown)
        private set

    fun addChangeListener(listener: (DocumentModel) -> Unit) {
        listeners += listener
    }

    fun removeChangeListener(listener: (DocumentModel) -> Unit) {
        listeners -= listener
    }

    fun setMarkdown(markdown: String) {
        val parsed = MarkdownParser.parse(markdown)
        if (parsed == document) return
        pushUndo()
        updateDocument(parsed)
    }

    fun getMarkdown(): String = MarkdownSerializer.serialize(document)

    fun getHtml(): String = HtmlSerializer.serialize(document)

    fun insertBlock(block: Block) {
        pushUndo()
        updateDocument(document.copy(blocks = document.blocks + block))
    }

    fun deleteBlock(index: Int): Boolean {
        if (index !in document.blocks.indices) return false
        pushUndo()
        updateDocument(document.copy(blocks = document.blocks.toMutableList().also { it.removeAt(index) }))
        return true
    }

    fun moveBlock(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex !in document.blocks.indices || toIndex !in document.blocks.indices) return false
        if (fromIndex == toIndex) return true
        val mutable = document.blocks.toMutableList()
        val moving = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moving)
        pushUndo()
        updateDocument(document.copy(blocks = mutable))
        return true
    }

    fun transformParagraphToHeading(index: Int, level: Int): Boolean {
        val block = document.blocks.getOrNull(index) as? Block.Paragraph ?: return false
        val mutable = document.blocks.toMutableList()
        mutable[index] = Block.Heading(level.coerceIn(1, 6), block.inlines)
        pushUndo()
        updateDocument(document.copy(blocks = mutable))
        return true
    }

    fun splitParagraph(blockIndex: Int, offset: Int): Boolean {
        val block = document.blocks.getOrNull(blockIndex) as? Block.Paragraph ?: return false
        val text = MarkdownSerializer.serializeInlines(block.inlines)
        if (offset !in 0..text.length) return false
        val left = Block.Paragraph(MarkdownParser.parseInlines(text.substring(0, offset)))
        val right = Block.Paragraph(MarkdownParser.parseInlines(text.substring(offset)))
        val mutable = document.blocks.toMutableList()
        mutable[blockIndex] = left
        mutable.add(blockIndex + 1, right)
        pushUndo()
        updateDocument(document.copy(blocks = mutable))
        return true
    }

    fun mergeWithPrevious(blockIndex: Int): Boolean {
        if (blockIndex !in 1..document.blocks.lastIndex) return false
        val prev = document.blocks[blockIndex - 1] as? Block.Paragraph ?: return false
        val cur = document.blocks[blockIndex] as? Block.Paragraph ?: return false
        val leftText = MarkdownSerializer.serializeInlines(prev.inlines)
        val rightText = MarkdownSerializer.serializeInlines(cur.inlines)
        val separator = if (leftText.endsWith(" ") || rightText.startsWith(" ")) "" else " "
        val merged = Block.Paragraph(prev.inlines + Inline.Text(separator) + cur.inlines)
        val mutable = document.blocks.toMutableList()
        mutable[blockIndex - 1] = merged
        mutable.removeAt(blockIndex)
        pushUndo()
        updateDocument(document.copy(blocks = mutable))
        return true
    }

    fun toggleTaskItem(taskListBlockIndex: Int, itemIndex: Int): Boolean {
        val list = document.blocks.getOrNull(taskListBlockIndex) as? Block.TaskList ?: return false
        if (itemIndex !in list.items.indices) return false
        val items = list.items.toMutableList()
        val target = items[itemIndex]
        items[itemIndex] = TaskItem(!target.checked, target.inlines)
        val mutable = document.blocks.toMutableList()
        mutable[taskListBlockIndex] = Block.TaskList(items)
        pushUndo()
        updateDocument(document.copy(blocks = mutable))
        return true
    }

    fun undo(): Boolean {
        val prev = undoStack.removeLastOrNull() ?: return false
        redoStack.addLast(document)
        updateDocument(prev, clearRedo = false)
        return true
    }

    fun redo(): Boolean {
        val next = redoStack.removeLastOrNull() ?: return false
        undoStack.addLast(document)
        updateDocument(next, clearRedo = false)
        return true
    }

    fun toggleBold() {
        val idx = document.blocks.indexOfFirst { it is Block.Paragraph }
        if (idx < 0) return
        val para = document.blocks[idx] as Block.Paragraph
        val hasBold = para.inlines.any { it is Inline.Bold }
        val updated = if (hasBold) {
            Block.Paragraph(unwrapBold(para.inlines))
        } else {
            Block.Paragraph(listOf(Inline.Bold(para.inlines.ifEmpty { listOf(Inline.Text("")) })))
        }
        val mutable = document.blocks.toMutableList()
        mutable[idx] = updated
        pushUndo()
        updateDocument(document.copy(blocks = mutable))
    }

    private fun unwrapBold(inlines: List<Inline>): List<Inline> {
        return inlines.flatMap { if (it is Inline.Bold) it.children else listOf(it) }
    }

    private fun updateDocument(newDocument: DocumentModel, clearRedo: Boolean = true) {
        document = newDocument
        if (clearRedo) redoStack.clear()
        listeners.forEach { it(document) }
    }

    private fun pushUndo() {
        undoStack.addLast(document)
        if (undoStack.size > 100) undoStack.removeFirst()
    }
}

fun rememberMarkdownEditorState(initialMarkdown: String = ""): MarkdownEditorState {
    return MarkdownEditorState(initialMarkdown)
}
