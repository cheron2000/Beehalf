package dev.beehalf.memory

import dev.beehalf.data.local.MessageEntity

data class ContextMessage(val role: String, val content: String)

data class ContextWindowConfig(
    val maxMessages: Int = 40,
    val maxApproxTokens: Int = 6000
) {
    init {
        require(maxMessages > 0)
        require(maxApproxTokens > 0)
    }
}

/**
 * Builds a bounded, chronological context window.
 * Token count is estimated conservatively from characters; the provider remains authoritative.
 */
class ContextWindowBuilder(
    private val config: ContextWindowConfig = ContextWindowConfig()
) {
    fun build(messages: List<MessageEntity>): List<ContextMessage> {
        val newestFirst = messages
            .sortedByDescending { it.createdAt }
            .take(config.maxMessages)

        var usedTokens = 0
        val selected = mutableListOf<MessageEntity>()
        for (message in newestFirst) {
            val estimatedTokens = ((message.content.length + 3) / 4).coerceAtLeast(1)
            if (selected.isNotEmpty() && usedTokens + estimatedTokens > config.maxApproxTokens) break
            if (selected.isEmpty() && estimatedTokens > config.maxApproxTokens) {
                selected += message.copy(content = message.content.take(config.maxApproxTokens * 4))
                break
            }
            selected += message
            usedTokens += estimatedTokens
        }
        return selected.asReversed().map { ContextMessage(it.role, it.content) }
    }
}
