package dev.beehalf.memory

import dev.beehalf.data.local.BeehalfDao
import dev.beehalf.data.local.ConversationEntity
import dev.beehalf.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ConversationRepository(
    private val dao: BeehalfDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    fun observeConversations(): Flow<List<ConversationEntity>> = dao.observeConversations()

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        dao.observeMessages(conversationId)

    suspend fun getRecentMessages(conversationId: String, limit: Int = 40): List<MessageEntity> {
        require(limit > 0) { "Message limit must be positive" }
        return dao.getRecentMessages(conversationId, limit)
    }

    suspend fun createConversation(title: String = "New conversation"): String {
        val now = clock()
        val id = newId()
        dao.upsertConversation(
            ConversationEntity(id = id, title = title.trim().ifBlank { "New conversation" }, createdAt = now, updatedAt = now)
        )
        return id
    }

    suspend fun appendMessage(conversationId: String, role: String, content: String): MessageEntity {
        require(role in setOf("system", "user", "assistant")) { "Unsupported message role" }
        require(content.isNotBlank()) { "Message content cannot be blank" }
        val message = MessageEntity(
            id = newId(),
            conversationId = conversationId,
            role = role,
            content = content.trim(),
            createdAt = clock()
        )
        val isFirstMessage = dao.countMessages(conversationId) == 0
        dao.insertMessage(message)
        val title = if (isFirstMessage && role == "user") content.trim().replace(Regex("\\s+"), " ").take(36) else "New conversation"
        dao.updateConversationAfterMessage(conversationId, message.createdAt, title)
        return message
    }

    suspend fun deleteConversation(conversationId: String) {
        dao.deleteMessagesForConversation(conversationId)
        dao.deleteConversation(conversationId)
    }
}
