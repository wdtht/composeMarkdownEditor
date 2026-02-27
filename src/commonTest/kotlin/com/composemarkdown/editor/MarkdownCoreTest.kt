package com.composemarkdown.editor

import com.composemarkdown.editor.model.Block
import com.composemarkdown.editor.model.Inline
import com.composemarkdown.editor.parser.MarkdownParser
import com.composemarkdown.editor.serializer.MarkdownSerializer
import com.composemarkdown.editor.state.MarkdownEditorState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkdownCoreTest {
    @Test
    fun parseHeadingAndParagraph() {
        val doc = MarkdownParser.parse("# Title\n\nHello **World**")
        assertEquals(2, doc.blocks.size)
        assertIs<Block.Heading>(doc.blocks[0])
        assertIs<Block.Paragraph>(doc.blocks[1])
    }

    @Test
    fun parseTaskList() {
        val doc = MarkdownParser.parse("- [ ] todo\n- [x] done")
        val list = doc.blocks.single() as Block.TaskList
        assertEquals(false, list.items[0].checked)
        assertEquals(true, list.items[1].checked)
    }

    @Test
    fun roundTrip() {
        val markdown = "## Hi\n\n- item\n- **bold**"
        val doc = MarkdownParser.parse(markdown)
        val out = MarkdownSerializer.serialize(doc)
        assertTrue(out.contains("## Hi"))
        assertTrue(out.contains("- item"))
    }

    @Test
    fun stateUndoRedo() {
        val state = MarkdownEditorState("hello")
        state.insertBlock(Block.Paragraph(listOf(Inline.Text("world"))))
        assertEquals(2, state.document.blocks.size)
        assertTrue(state.undo())
        assertEquals(1, state.document.blocks.size)
        assertTrue(state.redo())
        assertEquals(2, state.document.blocks.size)
    }
}
