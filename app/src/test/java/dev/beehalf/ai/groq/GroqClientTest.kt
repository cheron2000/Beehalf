package dev.beehalf.ai.groq

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GroqClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: GroqClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = GroqClient(
            http = OkHttpClient(),
            baseUrl = server.url("/openai/v1").toString().removeSuffix("/")
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun validateReturnsSortedModelIdsAndUsesBearerAuth() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"z-model"},{"id":"a-model"}]}"""))

        assertEquals(listOf("a-model", "z-model"), client.validate("test-key"))

        val request = server.takeRequest()
        assertEquals("/openai/v1/models", request.path)
        assertEquals("Bearer test-key", request.getHeader("Authorization"))
    }

    @Test
    fun completePostsMessagesAndReturnsAssistantContent() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"choices":[{"message":{"role":"assistant","content":"Hello from Beehalf"}}]}"""
            )
        )

        val answer = client.complete(
            apiKey = "test-key",
            model = "test-model",
            messages = listOf(ChatMessage("user", "Hello"))
        )

        assertEquals("Hello from Beehalf", answer)
        val request = server.takeRequest()
        assertEquals("/openai/v1/chat/completions", request.path)
        assertEquals("Bearer test-key", request.getHeader("Authorization"))
        val body = request.body.readUtf8()
        assert(body.contains("\"model\":\"test-model\""))
        assert(body.contains("\"content\":\"Hello\""))
    }

    @Test
    fun validateRejectsBlankApiKeyBeforeNetworkRequest() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { client.validate(" ") }
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun completeRejectsEmptyMessagesBeforeNetworkRequest() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { client.complete("key", "model", emptyList()) }
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun nonSuccessfulResponseDoesNotExposeResponseBody() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("sensitive provider payload"))
        val error = assertThrows(IOException::class.java) {
            runBlocking { client.validate("test-key") }
        }
        assertEquals("Groq returned HTTP 401", error.message)
    }

    @Test
    fun chatMessageRejectsUnsupportedRoles() {
        assertThrows(IllegalArgumentException::class.java) {
            ChatMessage("tool", "run something")
        }
    }
}
