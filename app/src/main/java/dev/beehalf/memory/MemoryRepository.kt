package dev.beehalf.memory

import dev.beehalf.data.local.BeehalfDao
import dev.beehalf.data.local.MemoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

enum class MemoryCategory(val storageValue: String, val displayName: String) {
    WORKING("working", "Working"),
    EPISODIC("episodic", "Episodic"),
    SEMANTIC("semantic", "Semantic"),
    PROCEDURAL("procedural", "Procedural"),
    KNOWLEDGE("knowledge", "Knowledge");

    companion object {
        fun fromStorage(value: String): MemoryCategory =
            entries.firstOrNull { it.storageValue == value } ?: KNOWLEDGE
    }
}

data class MemoryDraft(
    val content: String,
    val category: MemoryCategory = MemoryCategory.SEMANTIC,
    val source: String = "user",
    val confidence: Float = 1f,
    val pinned: Boolean = false
)

/**
 * Owns memory validation and lifecycle operations. The DAO remains a persistence detail.
 * User-created memories are explicitly sourced as "user"; extracted memories must provide
 * their originating conversation or document as the source.
 */
class MemoryRepository(
    private val dao: BeehalfDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    fun observeMemories(): Flow<List<MemoryEntity>> = dao.observeMemories()

    fun observeMemories(category: MemoryCategory): Flow<List<MemoryEntity>> =
        dao.observeMemoriesByCategory(category.storageValue)

    suspend fun getForContext(limit: Int = 12): List<MemoryEntity> {
        require(limit > 0) { "Memory limit must be positive" }
        return dao.getMemoriesForContext(limit)
    }

    suspend fun save(draft: MemoryDraft, existingId: String? = null): MemoryEntity {
        val content = draft.content.trim()
        require(content.isNotBlank()) { "Memory content cannot be blank" }
        require(draft.source.isNotBlank()) { "Memory source cannot be blank" }
        require(draft.confidence in 0f..1f) { "Confidence must be between 0 and 1" }

        val now = clock()
        val existing = existingId?.let { dao.getMemory(it) }
        val memory = MemoryEntity(
            id = existing?.id ?: newId(),
            category = draft.category.storageValue,
            content = content,
            source = draft.source.trim(),
            confidence = draft.confidence,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            pinned = draft.pinned
        )
        dao.upsertMemory(memory)
        return memory
    }

    suspend fun setPinned(memoryId: String, pinned: Boolean) {
        val memory = dao.getMemory(memoryId) ?: return
        dao.upsertMemory(memory.copy(pinned = pinned, updatedAt = clock()))
    }

    suspend fun delete(memoryId: String) {
        dao.deleteMemory(memoryId)
    }

    suspend fun applyRetention(cutoffTimestamp: Long): Int {
        require(cutoffTimestamp >= 0) { "Retention cutoff cannot be negative" }
        return dao.deleteUnpinnedMemoriesBefore(cutoffTimestamp)
    }
}
