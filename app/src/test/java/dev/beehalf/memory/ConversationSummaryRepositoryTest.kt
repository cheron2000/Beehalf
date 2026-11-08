package dev.beehalf.memory

import dev.beehalf.ai.groq.GroqClient
import dev.beehalf.data.local.BeehalfDao
import dev.beehalf.data.local.ConversationEntity
import dev.beehalf.data.local.MemoryEntity
import dev.beehalf.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationSummaryRepositoryTest {
    @Test
    fun doesNotCallProviderUntilSummaryThresholdIsReached() = runBlocking {
        val dao = FakeSummaryDao((1L..5L).map { message(it) })
        val repository = ConversationSummaryRepository(
            dao = dao,
            summaryThreshold = 4,
            recentWindow = 3
        )

        assertFalse(repository.refresh("test-key", "conversation-1"))
        assertTrue(dao.memories.isEmpty())
    }

    @Test
    fun storesSummaryAndSourceWatermarkAfterThreshold() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """{"choices":[{"message":{"content":"Project uses Kotlin and Room. Next task is local RAG."}}]}"""
            ).setHeader("Content-Type", "application/json")
        )
        server.start()
        try {
            val messages = (1L..16L).map { message(it) }
            val dao = FakeSummaryDao(messages)
            val repository = ConversationSummaryRepository(
                dao = dao,
                groqClient = GroqClient(OkHttpClient(), server.url("/").toString().removeSuffix("/")),
                clock = { 100L },
                summaryThreshold = 4,
                recentWindow = 4
            )

            assertTrue(repository.refresh("test-key", "conversation-1"))
            val saved = dao.memories["working-summary:conversation-1"]
            assertEquals("Project uses Kotlin and Room. Next task is local RAG.", saved?.content)
            assertEquals("conversation:conversation-1|through=12", saved?.source)
            assertTrue(saved?.pinned == true)
        } finally {
            server.shutdown()
        }
    }

    private fun message(timestamp: Long) = MessageEntity(
        id = "message-$timestamp",
        conversationId = "conversation-1",
        role = if (timestamp % 2L == 0L) "assistant" else "user",
        content = "Message $timestamp",
        createdAt = timestamp
    )
}

private class FakeSummaryDao(
    private val messages: List<MessageEntity>
) : BeehalfDao {
    val memories = linkedMapOf<String, MemoryEntity>()
    override suspend fun upsertConversation(conversation: ConversationEntity) = Unit
    override suspend fun insertMessage(message: MessageEntity) = Unit
    override suspend fun upsertMemory(memory: MemoryEntity) { memories[memory.id] = memory }
    override fun observeConversations(): Flow<List<ConversationEntity>> = MutableStateFlow(emptyList())
    override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = MutableStateFlow(emptyList())
    override suspend fun getRecentMessages(conversationId: String, limit: Int): List<MessageEntity> =
        messages.filter { it.conversationId == conversationId }.sortedByDescending { it.createdAt }.take(limit)
    override suspend fun getMessagesForSummary(conversationId: String, afterExclusive: Long, beforeExclusive: Long): List<MessageEntity> =
        messages.filter { it.conversationId == conversationId && it.createdAt > afterExclusive && it.createdAt < beforeExclusive }.sortedBy { it.createdAt }
    override suspend fun countMessages(conversationId: String): Int = messages.count { it.conversationId == conversationId }
    override fun observeMemories(): Flow<List<MemoryEntity>> = MutableStateFlow(memories.values.toList())
    override fun observeMemoriesByCategory(category: String): Flow<List<MemoryEntity>> =
        MutableStateFlow(memories.values.filter { it.category == category })
    override suspend fun getMemory(memoryId: String): MemoryEntity? = memories[memoryId]
    override suspend fun getMemoriesForContext(limit: Int): List<MemoryEntity> = memories.values.take(limit)
    override suspend fun getMemoriesByCategory(category: String, limit: Int): List<MemoryEntity> =
        memories.values.filter { it.category == category }.take(limit)
    override suspend fun deleteUnpinnedMemoriesBefore(cutoff: Long): Int = 0
    override suspend fun deleteMessagesForConversation(conversationId: String) = Unit
    override suspend fun deleteConversation(conversationId: String) = Unit
    override suspend fun deleteMemory(memoryId: String) { memories.remove(memoryId) }
    override suspend fun updateConversationAfterMessage(conversationId: String, updatedAt: Long, title: String) = Unit
}
