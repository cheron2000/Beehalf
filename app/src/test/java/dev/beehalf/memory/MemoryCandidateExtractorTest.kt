package dev.beehalf.memory

import dev.beehalf.ai.groq.GroqClient
import dev.beehalf.data.local.MessageEntity
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryCandidateExtractorTest {
    @Test
    fun parsesOnlyValidHighConfidenceCandidates() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """{"choices":[{"message":{"content":"{\"memories\":[{\"content\":\"Prefers Python for backend projects\",\"category\":\"semantic\",\"confidence\":0.92,\"rationale\":\"Repeated preference\"},{\"content\":\"Maybe likes Kotlin\",\"category\":\"semantic\",\"confidence\":0.4,\"rationale\":\"Uncertain\"},{\"content\":\"Invalid category\",\"category\":\"unknown\",\"confidence\":0.99,\"rationale\":\"\"}]}"}}]}"""
            ).setHeader("Content-Type", "application/json")
        )
        server.start()
        try {
            val client = GroqClient(
                http = OkHttpClient(),
                baseUrl = server.url("/").toString().removeSuffix("/")
            )
            val extractor = MemoryCandidateExtractor(client)
            val result = extractor.propose(
                apiKey = "test-key",
                messages = listOf(
                    MessageEntity("1", "conversation-1", "user", "I prefer Python for backend projects.", 1L)
                )
            )
            assertEquals(1, result.size)
            assertEquals("Prefers Python for backend projects", result.first().content)
            assertEquals(MemoryCategory.SEMANTIC, result.first().category)
        } finally {
            server.shutdown()
        }
    }
}
