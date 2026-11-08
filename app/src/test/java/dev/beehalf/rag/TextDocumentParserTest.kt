package dev.beehalf.rag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class TextDocumentParserTest {
    @Test
    fun supportsTxtAndMarkdownByExtensionOrMimeType() {
        assertTrue(TextDocumentParser.supports("notes.txt", "application/octet-stream"))
        assertTrue(TextDocumentParser.supports("README.md", "application/octet-stream"))
        assertTrue(TextDocumentParser.supports("notes", "text/plain"))
        assertTrue(TextDocumentParser.supports("notes", "text/markdown"))
        assertFalse(TextDocumentParser.supports("archive.pdf", "application/pdf"))
    }

    @Test
    fun preservesMarkdownStructureAndNormalizesLineEndings() {
        val source = "# Heading\r\n\r\n- one\r- two\r\n\n```python\nprint(1)\n```"
        val parsed = TextDocumentParser.parse(
            "guide.md",
            "text/markdown",
            ByteArrayInputStream(source.toByteArray(Charsets.UTF_8))
        )
        assertEquals("# Heading\n\n- one\n- two\n\n```python\nprint(1)\n```", parsed)
    }

    @Test
    fun removesUtf8BomAndSupportsUtf16Bom() {
        val utf8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "hello".toByteArray(Charsets.UTF_8)
        assertEquals(
            "hello",
            TextDocumentParser.parse("a.txt", "text/plain", ByteArrayInputStream(utf8))
        )

        val utf16 = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
            "hello".toByteArray(Charsets.UTF_16LE)
        assertEquals(
            "hello",
            TextDocumentParser.parse("a.txt", "text/plain", ByteArrayInputStream(utf16))
        )
    }

    @Test
    fun rejectsUnsupportedTypesAndBinaryContent() {
        assertThrows(IllegalArgumentException::class.java) {
            TextDocumentParser.parse("file.pdf", "application/pdf", ByteArrayInputStream(byteArrayOf()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            TextDocumentParser.parse("file.txt", "text/plain", ByteArrayInputStream(byteArrayOf(65, 0, 66)))
        }
    }

    @Test
    fun rejectsFilesLargerThanConfiguredLimit() {
        val oversized = ByteArray(TextDocumentParser.MAX_DOCUMENT_BYTES + 1) { 65 }
        assertThrows(IllegalArgumentException::class.java) {
            TextDocumentParser.parse("large.txt", "text/plain", ByteArrayInputStream(oversized))
        }
    }
}
