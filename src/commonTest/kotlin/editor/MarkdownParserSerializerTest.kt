package editor

import editor.model.Block
import editor.model.Inline
import editor.parser.MarkdownParser
import editor.serializer.MarkdownSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkdownParserSerializerTest {
    @Test
    fun parseBlockTypesAndInlineStyles() {
        val input = """
            # Hello **World**
            
            > quoted
            
            - [x] done
            - [ ] todo
            
            - item one
            - item two
            
            1. first
            2. second
            
            ```kotlin
            println("ok")
            ```
            
            ---
            
            plain with [link](https://example.com) and `code`
        """.trimIndent()

        val doc = MarkdownParser.parse(input)
        assertTrue(doc.blocks.isNotEmpty())

        val heading = assertIs<Block.Heading>(doc.blocks[0])
        assertEquals(1, heading.level)
        assertIs<Inline.Bold>(heading.inlines[1])

        assertIs<Block.Blockquote>(doc.blocks[1])
        assertIs<Block.TaskList>(doc.blocks[2])
        assertIs<Block.UnorderedList>(doc.blocks[3])
        assertIs<Block.OrderedList>(doc.blocks[4])
        assertIs<Block.CodeBlock>(doc.blocks[5])
        assertIs<Block.ThematicBreak>(doc.blocks[6])
        assertIs<Block.Paragraph>(doc.blocks[7])
    }

    @Test
    fun roundTripPreservesStructure() {
        val input = """
            ## Title
            
            paragraph **bold** and *italic* and ~~gone~~ and [site](https://a.com)
            
            - item
            - item2
        """.trimIndent()

        val parsed = MarkdownParser.parse(input)
        val output = MarkdownSerializer.serialize(parsed)
        val reparsed = MarkdownParser.parse(output)

        assertEquals(parsed, reparsed)
    }
}
