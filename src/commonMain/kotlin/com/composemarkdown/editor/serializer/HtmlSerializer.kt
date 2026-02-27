package com.composemarkdown.editor.serializer

import com.composemarkdown.editor.model.Block
import com.composemarkdown.editor.model.DocumentModel
import com.composemarkdown.editor.model.Inline

object HtmlSerializer {
    fun serialize(document: DocumentModel): String = document.blocks.joinToString("\n") { serializeBlock(it) }

    private fun serializeBlock(block: Block): String = when (block) {
        is Block.Paragraph -> "<p>${serializeInlines(block.inlines)}</p>"
        is Block.Heading -> "<h${block.level}>${serializeInlines(block.inlines)}</h${block.level}>"
        is Block.CodeBlock -> "<pre><code>${escape(block.content)}</code></pre>"
        is Block.MathBlock -> "<div class=\"math\">${escape(block.content)}</div>"
        is Block.MermaidBlock -> "<pre class=\"mermaid\">${escape(block.content)}</pre>"
        is Block.BlockQuote -> "<blockquote>${serialize(DocumentModel(block.blocks))}</blockquote>"
        is Block.UnorderedList -> "<ul>${block.items.joinToString("") { "<li>${serializeInlines(it.inlines)}</li>" }}</ul>"
        is Block.OrderedList -> "<ol start=\"${block.start}\">${block.items.joinToString("") { "<li>${serializeInlines(it.inlines)}</li>" }}</ol>"
        is Block.TaskList -> "<ul>${block.items.joinToString("") { "<li><input type=\"checkbox\" ${if (it.checked) "checked" else ""} disabled />${serializeInlines(it.inlines)}</li>" }}</ul>"
        is Block.Table -> {
            val head = block.headers.joinToString("") { "<th>${escape(it)}</th>" }
            val rows = block.rows.joinToString("") { row -> "<tr>${row.joinToString("") { "<td>${escape(it)}</td>" }}</tr>" }
            "<table><thead><tr>$head</tr></thead><tbody>$rows</tbody></table>"
        }
        is Block.ImageBlock -> "<img alt=\"${escape(block.alt)}\" src=\"${escape(block.url)}\" />"
        is Block.FootnoteDefinition -> "<div class=\"footnote\" id=\"fn-${escape(block.id)}\">${serialize(DocumentModel(block.blocks))}</div>"
        is Block.DetailsBlock -> "<details><summary>${escape(block.summary)}</summary>${serialize(DocumentModel(block.blocks))}</details>"
        Block.ThematicBreak -> "<hr />"
    }

    private fun serializeInlines(inlines: List<Inline>): String = inlines.joinToString("") {
        when (it) {
            is Inline.Text -> escape(it.content)
            is Inline.Bold -> "<strong>${serializeInlines(it.children)}</strong>"
            is Inline.Italic -> "<em>${serializeInlines(it.children)}</em>"
            is Inline.Strikethrough -> "<del>${serializeInlines(it.children)}</del>"
            is Inline.InlineCode -> "<code>${escape(it.code)}</code>"
            is Inline.Link -> "<a href=\"${escape(it.url)}\">${serializeInlines(it.children)}</a>"
            is Inline.Image -> "<img alt=\"${escape(it.alt)}\" src=\"${escape(it.url)}\" />"
            is Inline.InlineMath -> "<span class=\"math\">${escape(it.formula)}</span>"
            is Inline.Highlight -> "<mark>${serializeInlines(it.children)}</mark>"
            is Inline.Superscript -> "<sup>${serializeInlines(it.children)}</sup>"
            is Inline.Subscript -> "<sub>${serializeInlines(it.children)}</sub>"
            is Inline.FootnoteReference -> "<sup><a href=\"#fn-${escape(it.id)}\">${escape(it.id)}</a></sup>"
        }
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
