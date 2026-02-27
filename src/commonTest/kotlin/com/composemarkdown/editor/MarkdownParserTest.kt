package com.composemarkdown.editor

import com.composemarkdown.editor.model.Block
import com.composemarkdown.editor.model.Inline
import com.composemarkdown.editor.model.TableAlignment
import com.composemarkdown.editor.parser.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkdownParserTest {
    @Test
    fun parse_atxAndSetextHeadings() {
        val doc = MarkdownParser.parse("# H1\n\nHello\n---")
        assertIs<Block.Heading>(doc.blocks[0])
        assertEquals(1, (doc.blocks[0] as Block.Heading).level)
        assertIs<Block.Heading>(doc.blocks[1])
        assertEquals(2, (doc.blocks[1] as Block.Heading).level)
    }

    @Test
    fun parse_codeBlockWithLanguage() {
        val doc = MarkdownParser.parse("```kotlin\nval x = 1\n```")
        val block = doc.blocks.single() as Block.CodeBlock
        assertEquals("kotlin", block.language)
        assertEquals("val x = 1", block.content)
    }

    @Test
    fun parse_tableAndAlignments() {
        val md = "| A | B | C |\n| :-- | :-: | --: |\n| 1 | 2 | 3 |"
        val doc = MarkdownParser.parse(md)
        val table = doc.blocks.single() as Block.Table
        assertEquals(listOf("A", "B", "C"), table.headers)
        assertEquals(listOf(TableAlignment.LEFT, TableAlignment.CENTER, TableAlignment.RIGHT), table.alignments)
        assertEquals(listOf("1", "2", "3"), table.rows.single())
    }

    @Test
    fun parse_blockImage() {
        val doc = MarkdownParser.parse("![logo](https://img/logo.png)")
        val image = doc.blocks.single() as Block.ImageBlock
        assertEquals("logo", image.alt)
        assertEquals("https://img/logo.png", image.url)
    }

    @Test
    fun parse_inlineStylesAndAutolink() {
        val inlines = MarkdownParser.parseInlines("**bold** *italic* ~~del~~ `code` see https://example.com")
        assertTrue(inlines.any { it is Inline.Bold })
        assertTrue(inlines.any { it is Inline.Italic })
        assertTrue(inlines.any { it is Inline.Strikethrough })
        assertTrue(inlines.any { it is Inline.InlineCode })
        assertTrue(inlines.any { it is Inline.Link && it.url == "https://example.com" })
    }

    @Test
    fun parse_nestedBlockquote() {
        val doc = MarkdownParser.parse("> # Title\n> - item")
        val quote = doc.blocks.single() as Block.BlockQuote
        assertEquals(2, quote.blocks.size)
        assertIs<Block.Heading>(quote.blocks[0])
        assertIs<Block.UnorderedList>(quote.blocks[1])
    }
}
