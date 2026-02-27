package com.composemarkdown.editor

import com.composemarkdown.editor.model.Block
import com.composemarkdown.editor.model.Inline
import com.composemarkdown.editor.state.MarkdownEditorState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkdownEditorStateTest {
    @Test
    fun insertDeleteMoveAndUndoRedo() {
        val state = MarkdownEditorState("one")
        state.insertBlock(Block.Paragraph(listOf(Inline.Text("two"))))
        state.insertBlock(Block.Paragraph(listOf(Inline.Text("three"))))
        assertEquals(3, state.document.blocks.size)

        assertTrue(state.moveBlock(2, 1))
        assertTrue(state.deleteBlock(0))
        assertEquals(2, state.document.blocks.size)

        assertTrue(state.undo())
        assertEquals(3, state.document.blocks.size)
        assertTrue(state.redo())
        assertEquals(2, state.document.blocks.size)
    }

    @Test
    fun transformSplitMergeParagraph() {
        val state = MarkdownEditorState("hello world")
        assertTrue(state.transformParagraphToHeading(0, 2))
        assertIs<Block.Heading>(state.document.blocks[0])

        state.setMarkdown("hello world")
        assertTrue(state.splitParagraph(0, 5))
        assertEquals(2, state.document.blocks.size)
        assertTrue(state.mergeWithPrevious(1))
        assertEquals(1, state.document.blocks.size)
        assertEquals("hello world", state.getMarkdown())
    }

    @Test
    fun toggleTaskItemAndBold() {
        val state = MarkdownEditorState("- [ ] todo\n\ntext")
        assertTrue(state.toggleTaskItem(0, 0))
        val task = state.document.blocks[0] as Block.TaskList
        assertTrue(task.items[0].checked)

        state.toggleBold()
        val para = state.document.blocks[1] as Block.Paragraph
        assertTrue(para.inlines.first() is Inline.Bold)
        state.toggleBold()
        val unwrapped = state.document.blocks[1] as Block.Paragraph
        assertFalse(unwrapped.inlines.first() is Inline.Bold)
    }

    @Test
    fun notifyListenersOnChange() {
        val state = MarkdownEditorState("start")
        var called = 0
        val listener: (com.composemarkdown.editor.model.DocumentModel) -> Unit = { called++ }
        state.addChangeListener(listener)

        state.insertBlock(Block.ThematicBreak)
        state.setMarkdown("reset")
        state.removeChangeListener(listener)
        state.insertBlock(Block.ThematicBreak)

        assertEquals(2, called)
    }

    @Test
    fun invalidOperationsReturnFalse() {
        val state = MarkdownEditorState("text")
        assertFalse(state.deleteBlock(2))
        assertFalse(state.moveBlock(0, 3))
        assertFalse(state.transformParagraphToHeading(1, 1))
        assertFalse(state.splitParagraph(0, 99))
        assertFalse(state.mergeWithPrevious(0))
        assertFalse(state.toggleTaskItem(0, 0))
    }
}
