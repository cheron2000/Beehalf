package dev.beehalf.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BeehalfDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActionProposal(proposal: ActionProposalEntity)

    @Query("SELECT * FROM action_proposals ORDER BY createdAt DESC")
    fun observeActionProposals(): Flow<List<ActionProposalEntity>>

    @Query("UPDATE action_proposals SET status = 'expired', decidedAt = :now WHERE status = 'pending' AND expiresAt <= :now")
    suspend fun expireActionProposals(now: Long): Int

    @Query("UPDATE action_proposals SET status = :status, decidedAt = :decidedAt WHERE id = :id AND status = 'pending' AND expiresAt > :decidedAt")
    suspend fun decideActionProposal(id: String, status: String, decidedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(memory: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(document: DocumentEntity)

    @Query("SELECT * FROM documents ORDER BY importedAt DESC")
    fun observeDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE uri = :uri LIMIT 1")
    suspend fun getDocumentByUri(uri: String): DocumentEntity?

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteDocument(documentId: String)

    @Query("SELECT * FROM conversations WHERE archived = 0 ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: String, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND createdAt > :afterExclusive AND createdAt < :beforeExclusive ORDER BY createdAt ASC")
    suspend fun getMessagesForSummary(conversationId: String, afterExclusive: Long, beforeExclusive: Long): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun countMessages(conversationId: String): Int

    @Query("SELECT * FROM memories ORDER BY pinned DESC, updatedAt DESC")
    fun observeMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :memoryId LIMIT 1")
    suspend fun getMemory(memoryId: String): MemoryEntity?

    @Query("SELECT * FROM memories ORDER BY pinned DESC, updatedAt DESC LIMIT :limit")
    suspend fun getMemoriesForContext(limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY pinned DESC, updatedAt DESC LIMIT :limit")
    suspend fun getMemoriesByCategory(category: String, limit: Int): List<MemoryEntity>

    @Query("DELETE FROM memories WHERE updatedAt < :cutoff AND pinned = 0")
    suspend fun deleteUnpinnedMemoriesBefore(cutoff: Long): Int

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY pinned DESC, updatedAt DESC")
    fun observeMemoriesByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM memories WHERE id = :memoryId")
    suspend fun deleteMemory(memoryId: String)

    @Query("UPDATE conversations SET updatedAt = :updatedAt, title = CASE WHEN :title != '' THEN :title ELSE title END WHERE id = :conversationId")
    suspend fun updateConversationAfterMessage(conversationId: String, updatedAt: Long, title: String)
}
