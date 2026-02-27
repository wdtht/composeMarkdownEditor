package com.composemarkdown.editor

import com.composemarkdown.editor.model.Block
import com.composemarkdown.editor.model.Inline
import com.composemarkdown.editor.parser.MarkdownParser
import com.composemarkdown.editor.serializer.HtmlSerializer
import com.composemarkdown.editor.serializer.MarkdownSerializer
import com.composemarkdown.editor.state.MarkdownEditorState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkdownAdvancedFeaturesTest {
    @Test
    fun parse_mathMermaidFootnoteAndDetails() {
        val markdown = """
$$
a^2 + b^2 = c^2
$$

```mermaid
graph TD
A-->B
```

[^n1]: footnote content

<details>
<summary>More</summary>
text
</details>
""".trim()

        val doc = MarkdownParser.parse(markdown)
        assertIs<Block.MathBlock>(doc.blocks[0])
        assertIs<Block.MermaidBlock>(doc.blocks[1])
        assertIs<Block.FootnoteDefinition>(doc.blocks[2])
        assertIs<Block.DetailsBlock>(doc.blocks[3])
    }

    @Test
    fun inline_highlightMathSupSubFootnoteRef_roundTrip() {
        val input = "==hi== \$x+y\$ ^sup^ ~sub~ [^1]"
        val inlines = MarkdownParser.parseInlines(input)
        assertTrue(inlines.any { it is Inline.Highlight })
        assertTrue(inlines.any { it is Inline.InlineMath })
        assertTrue(inlines.any { it is Inline.Superscript })
        assertTrue(inlines.any { it is Inline.Subscript })
        assertTrue(inlines.any { it is Inline.FootnoteReference })

        val serialized = MarkdownSerializer.serializeInlines(inlines)
        assertEquals(input, serialized)
    }

    @Test
    fun html_export_availableFromState() {
        val state = MarkdownEditorState("# Title\n\nHello **World**")
        val html = state.getHtml()
        assertTrue(html.contains("<h1>Title</h1>"))
        assertTrue(html.contains("<strong>World</strong>"))
    }

    @Test
    fun autolink_trailingPunctuationNotInLink() {
        val inlines = MarkdownParser.parseInlines("See https://example.com.")
        val link = inlines.first { it is Inline.Link } as Inline.Link
        assertEquals("https://example.com", link.url)
        assertEquals(".", (inlines.last() as Inline.Text).content)
    }

    @Test
    fun htmlSerializer_rendersTableAndTaskList() {
        val doc = MarkdownParser.parse("| A | B |\n| --- | --- |\n| 1 | 2 |\n\n- [x] done")
        val html = HtmlSerializer.serialize(doc)
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("checkbox"))
    }


    @Test
    fun htmlSerializer_respectsTableAlignment() {
        val doc = MarkdownParser.parse("| L | C | R |\n| :-- | :-: | --: |\n| 1 | 2 | 3 |")
        val html = HtmlSerializer.serialize(doc)
        assertTrue(html.contains("<th style=\"text-align:left\">L</th>"))
        assertTrue(html.contains("<th style=\"text-align:center\">C</th>"))
        assertTrue(html.contains("<th style=\"text-align:right\">R</th>"))
        assertTrue(html.contains("<td style=\"text-align:right\">3</td>"))
    }
}
