package dev.beehalf.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations", indices = [Index("updatedAt")])
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val archived: Boolean = false
)

@Entity(
    tableName = "messages",
    indices = [Index("conversationId"), Index(value = ["conversationId", "createdAt"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: Long
) {
    init {
        require(role in setOf("system", "user", "assistant")) { "Unsupported message role" }
        require(content.isNotBlank()) { "Message content cannot be blank" }
    }
}

@Entity(tableName = "memories", indices = [Index("category"), Index("updatedAt")])
data class MemoryEntity(
    @PrimaryKey val id: String,
    val category: String,
    val content: String,
    val source: String,
    val confidence: Float,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false
) {
    init {
        require(category in setOf("working", "episodic", "semantic", "procedural", "knowledge")) {
            "Unsupported memory category"
        }
        require(content.isNotBlank()) { "Memory content cannot be blank" }
        require(confidence in 0f..1f) { "Confidence must be between 0 and 1" }
    }
}
