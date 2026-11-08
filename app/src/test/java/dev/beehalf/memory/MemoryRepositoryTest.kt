package dev.beehalf.memory

import dev.beehalf.data.local.ActionProposalEntity
import dev.beehalf.data.local.BeehalfDao
import dev.beehalf.data.local.ConversationEntity
import dev.beehalf.data.local.DocumentEntity
import dev.beehalf.data.local.MemoryEntity
import dev.beehalf.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRepositoryTest {
    @Test
    fun saveTrimsContentAndPreservesCreationTimeOnEdit() = runBlocking {
        val dao = FakeMemoryDao()
        var now = 100L
        val repository = MemoryRepository(dao, clock = { now }, newId = { "memory-1" })

        val created = repository.save(MemoryDraft("  User prefers Python  "))
        now = 200L
        val updated = repository.save(
            MemoryDraft("User prefers Python and Kotlin", category = MemoryCategory.SEMANTIC),
            existingId = created.id
        )

        assertEquals("User prefers Python", created.content)
        assertEquals(100L, updated.createdAt)
        assertEquals(200L, updated.updatedAt)
        assertEquals(created.id, updated.id)
    }

    @Test
    fun pinnedMemoriesAreKeptByRetentionAndUnpinnedMemoriesAreRemoved() = runBlocking {
        val dao = FakeMemoryDao()
        val repository = MemoryRepository(dao, clock = { 100L }, newId = { "memory-${dao.memories.size}" })
        val oldUnpinned = repository.save(MemoryDraft("Temporary note"))
        val oldPinned = repository.save(MemoryDraft("Important note", pinned = true))

        val removed = repository.applyRetention(cutoffTimestamp = 101L)

        assertEquals(1, removed)
        assertFalse(dao.memories.containsKey(oldUnpinned.id))
        assertTrue(dao.memories.containsKey(oldPinned.id))
    }

    @Test
    fun rejectsBlankMemoryAndInvalidConfidence() = runBlocking {
        val repository = MemoryRepository(FakeMemoryDao())
        assertTrue(runCatching { repository.save(MemoryDraft("  ")) }.isFailure)
        assertTrue(runCatching { repository.save(MemoryDraft("Valid", confidence = 1.5f)) }.isFailure)
    }
}

private class FakeMemoryDao : BeehalfDao {
    val memories = linkedMapOf<String, MemoryEntity>()
    private val memoryFlow = MutableStateFlow<List<MemoryEntity>>(emptyList())

    override suspend fun insertActionProposal(proposal: ActionProposalEntity) = Unit
    override fun observeActionProposals(): Flow<List<ActionProposalEntity>> = MutableStateFlow(emptyList())
    override suspend fun expireActionProposals(now: Long): Int = 0
    override suspend fun decideActionProposal(id: String, status: String, decidedAt: Long): Int = 0
    override suspend fun upsertConversation(conversation: ConversationEntity) = Unit
    override suspend fun insertMessage(message: MessageEntity) = Unit
    override suspend fun upsertDocument(document: DocumentEntity) = Unit
    override fun observeDocuments(): Flow<List<DocumentEntity>> = MutableStateFlow(emptyList())
    override suspend fun getDocumentByUri(uri: String): DocumentEntity? = null
    override suspend fun deleteDocument(documentId: String) = Unit
    override suspend fun upsertMemory(memory: MemoryEntity) {
        memories[memory.id] = memory
        memoryFlow.value = memories.values.sortedWith(compareByDescending<MemoryEntity> { it.pinned }.thenByDescending { it.updatedAt })
    }
    override fun observeConversations(): Flow<List<ConversationEntity>> = MutableStateFlow(emptyList())
    override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = MutableStateFlow(emptyList())
    override suspend fun getRecentMessages(conversationId: String, limit: Int): List<MessageEntity> = emptyList()
    override suspend fun getMessagesForSummary(conversationId: String, afterExclusive: Long, beforeExclusive: Long): List<MessageEntity> = emptyList()
    override suspend fun countMessages(conversationId: String): Int = 0
    override fun observeMemories(): Flow<List<MemoryEntity>> = memoryFlow
    override fun observeMemoriesByCategory(category: String): Flow<List<MemoryEntity>> =
        MutableStateFlow(memories.values.filter { it.category == category })
    override suspend fun getMemory(memoryId: String): MemoryEntity? = memories[memoryId]
    override suspend fun getMemoriesForContext(limit: Int): List<MemoryEntity> =
        memories.values.sortedWith(compareByDescending<MemoryEntity> { it.pinned }.thenByDescending { it.updatedAt }).take(limit)
    override suspend fun getMemoriesByCategory(category: String, limit: Int): List<MemoryEntity> =
        memories.values.filter { it.category == category }.take(limit)
    override suspend fun deleteUnpinnedMemoriesBefore(cutoff: Long): Int {
        val ids = memories.values.filter { !it.pinned && it.updatedAt < cutoff }.map { it.id }
        ids.forEach { memories.remove(it) }
        memoryFlow.value = memories.values.toList()
        return ids.size
    }
    override suspend fun deleteMessagesForConversation(conversationId: String) = Unit
    override suspend fun deleteConversation(conversationId: String) = Unit
    override suspend fun deleteMemory(memoryId: String) {
        memories.remove(memoryId)
        memoryFlow.value = memories.values.toList()
    }
    override suspend fun updateConversationAfterMessage(conversationId: String, updatedAt: Long, title: String) = Unit
}
