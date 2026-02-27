package com.composemarkdown.editor

import com.composemarkdown.editor.model.Block
import com.composemarkdown.editor.model.DocumentModel
import com.composemarkdown.editor.model.Inline
import com.composemarkdown.editor.model.TableAlignment
import com.composemarkdown.editor.parser.MarkdownParser
import com.composemarkdown.editor.serializer.MarkdownSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownSerializerTest {
    @Test
    fun serialize_roundTripForComplexDocument() {
        val markdown = """
# Title

- [x] done
- [ ] todo

| Name | Score |
| :-- | --: |
| A | 10 |

![img](https://example.com/a.png)

---
""".trim()
        val parsed = MarkdownParser.parse(markdown)
        val serialized = MarkdownSerializer.serialize(parsed)
        val reparsed = MarkdownParser.parse(serialized)
        assertEquals(parsed, reparsed)
    }

    @Test
    fun serialize_inlineNodes() {
        val text = MarkdownSerializer.serializeInlines(
            listOf(
                Inline.Text("Hi "),
                Inline.Bold(listOf(Inline.Text("B"))),
                Inline.Italic(listOf(Inline.Text("I"))),
                Inline.Strikethrough(listOf(Inline.Text("S"))),
                Inline.InlineCode("C"),
                Inline.Link(listOf(Inline.Text("L")), "https://a"),
                Inline.Image("A", "https://img")
            )
        )
        assertTrue(text.contains("**B**"))
        assertTrue(text.contains("*I*"))
        assertTrue(text.contains("~~S~~"))
        assertTrue(text.contains("`C`"))
        assertTrue(text.contains("[L](https://a)"))
        assertTrue(text.contains("![A](https://img)"))
    }

    @Test
    fun serialize_tableBlock() {
        val doc = DocumentModel(
            listOf(
                Block.Table(
                    headers = listOf("h1", "h2"),
                    alignments = listOf(TableAlignment.LEFT, TableAlignment.RIGHT),
                    rows = listOf(listOf("a", "b"))
                )
            )
        )
        val output = MarkdownSerializer.serialize(doc)
        assertEquals("| h1 | h2 |\n| :--- | ---: |\n| a | b |", output)
    }
}
