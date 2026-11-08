package dev.beehalf.automation

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

class ActionApprovalRepositoryTest {
    @Test
    fun enqueueRequiresValidPolicyAndStartsPending() = runBlocking {
        val dao = ApprovalFakeDao()
        val repository = ActionApprovalRepository(dao, clock = { 1_000L })
        val saved = repository.enqueue(
            ActionProposal("proposal-1", AutomationAction.OpenApp("com.example.app"), "Open Example", 900L)
        )
        assertEquals("pending", saved.status)
        assertEquals("com.example.app", saved.target)
        assertEquals(900L + ActionApprovalRepository.DEFAULT_EXPIRY_MILLIS, saved.expiresAt)
        assertTrue(runCatching {
            repository.enqueue(ActionProposal("bad", AutomationAction.OpenWebPage("http://bad.example"), "Bad URL", 900L))
        }.isFailure)
    }

    @Test
    fun approvalAndRejectionAreSingleUseDecisionsOnly() = runBlocking {
        val dao = ApprovalFakeDao()
        val repository = ActionApprovalRepository(dao, clock = { 1_000L })
        repository.enqueue(ActionProposal("approve-me", AutomationAction.OpenApp("com.example.app"), "Open app", 900L))
        repository.enqueue(ActionProposal("reject-me", AutomationAction.OpenWebPage("https://example.com"), "Open site", 900L))

        assertTrue(repository.approve("approve-me"))
        assertFalse(repository.approve("approve-me"))
        assertTrue(repository.reject("reject-me"))
        assertEquals("approved", dao.items.value.first { it.id == "approve-me" }.status)
        assertEquals("rejected", dao.items.value.first { it.id == "reject-me" }.status)
    }

    @Test
    fun staleProposalExpiresAndCannotBeApproved() = runBlocking {
        val dao = ApprovalFakeDao()
        var now = 1_000L
        val repository = ActionApprovalRepository(dao, clock = { now }, expiryMillis = 100L)
        repository.enqueue(ActionProposal("stale", AutomationAction.OpenApp("com.example.app"), "Open app", 1_000L))
        now = 1_100L
        assertFalse(repository.approve("stale"))
        assertEquals("expired", dao.items.value.single().status)
    }
}

private class ApprovalFakeDao : BeehalfDao {
    val items = MutableStateFlow<List<ActionProposalEntity>>(emptyList())
    override suspend fun insertActionProposal(proposal: ActionProposalEntity) {
        require(items.value.none { it.id == proposal.id })
        items.value = items.value + proposal
    }
    override fun observeActionProposals(): Flow<List<ActionProposalEntity>> = items
    override suspend fun expireActionProposals(now: Long): Int {
        var changed = 0
        items.value = items.value.map {
            if (it.status == "pending" && it.expiresAt <= now) {
                changed++
                it.copy(status = "expired", decidedAt = now)
            } else it
        }
        return changed
    }
    override suspend fun decideActionProposal(id: String, status: String, decidedAt: Long): Int {
        val current = items.value.firstOrNull { it.id == id && it.status == "pending" && it.expiresAt > decidedAt } ?: return 0
        items.value = items.value.map { if (it.id == current.id) it.copy(status = status, decidedAt = decidedAt) else it }
        return 1
    }
    override suspend fun upsertConversation(conversation: ConversationEntity) = Unit
    override suspend fun insertMessage(message: MessageEntity) = Unit
    override suspend fun upsertDocument(document: DocumentEntity) = Unit
    override fun observeDocuments(): Flow<List<DocumentEntity>> = MutableStateFlow(emptyList())
    override suspend fun getDocumentByUri(uri: String): DocumentEntity? = null
    override suspend fun deleteDocument(documentId: String) = Unit
    override suspend fun upsertMemory(memory: MemoryEntity) = Unit
    override fun observeConversations(): Flow<List<ConversationEntity>> = MutableStateFlow(emptyList())
    override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> = MutableStateFlow(emptyList())
    override suspend fun getRecentMessages(conversationId: String, limit: Int): List<MessageEntity> = emptyList()
    override suspend fun getMessagesForSummary(conversationId: String, afterExclusive: Long, beforeExclusive: Long): List<MessageEntity> = emptyList()
    override suspend fun countMessages(conversationId: String): Int = 0
    override fun observeMemories(): Flow<List<MemoryEntity>> = MutableStateFlow(emptyList())
    override fun observeMemoriesByCategory(category: String): Flow<List<MemoryEntity>> = MutableStateFlow(emptyList())
    override suspend fun getMemory(memoryId: String): MemoryEntity? = null
    override suspend fun getMemoriesForContext(limit: Int): List<MemoryEntity> = emptyList()
    override suspend fun getMemoriesByCategory(category: String, limit: Int): List<MemoryEntity> = emptyList()
    override suspend fun deleteUnpinnedMemoriesBefore(cutoff: Long): Int = 0
    override suspend fun deleteMessagesForConversation(conversationId: String) = Unit
    override suspend fun deleteConversation(conversationId: String) = Unit
    override suspend fun deleteMemory(memoryId: String) = Unit
    override suspend fun updateConversationAfterMessage(conversationId: String, updatedAt: Long, title: String) = Unit
}
