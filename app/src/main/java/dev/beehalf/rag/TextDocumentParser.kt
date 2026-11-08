package dev.beehalf.rag

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Bounded, on-device extraction for plain-text and Markdown documents.
 * Markdown is intentionally preserved as source text so later chunking can
 * retain headings, lists, links, and code fences.
 */
object TextDocumentParser {
    const val MAX_DOCUMENT_BYTES: Int = 2 * 1024 * 1024
    private const val UTF8_BOM = "\u00EF\u00BB\u00BF"

    fun supports(displayName: String, mimeType: String): Boolean {
        val extension = displayName.substringAfterLast('.', "").lowercase()
        return extension == "txt" || extension == "md" || extension == "markdown" ||
            mimeType.equals("text/plain", ignoreCase = true) ||
            mimeType.equals("text/markdown", ignoreCase = true) ||
            mimeType.equals("text/x-markdown", ignoreCase = true)
    }

    fun parse(displayName: String, mimeType: String, input: InputStream): String {
        require(supports(displayName, mimeType)) { "Only TXT and Markdown files are supported in this stage." }
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_DOCUMENT_BYTES) {
                "Document exceeds the 2 MB text-import limit."
            }
            output.write(buffer, 0, count)
        }

        val bytes = output.toByteArray()
        val decoded = when {
            bytes.size >= 3 && bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
                String(bytes.copyOfRange(3, bytes.size), Charsets.UTF_8)
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                String(bytes.copyOfRange(2, bytes.size), Charsets.UTF_16LE)
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                String(bytes.copyOfRange(2, bytes.size), Charsets.UTF_16BE)
            else -> String(bytes, Charsets.UTF_8)
        }

        require('\u0000' !in decoded) { "The selected file does not appear to be plain text." }
        return decoded.removePrefix(UTF8_BOM).replace("\r\n", "\n").replace('\r', '\n')
    }
}
