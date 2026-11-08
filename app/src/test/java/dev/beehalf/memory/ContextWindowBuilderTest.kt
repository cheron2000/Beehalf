package dev.beehalf.memory

import dev.beehalf.data.local.MessageEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ContextWindowBuilderTest {
    @Test
    fun keepsMessagesChronologicalAndWithinConfiguredCount() {
        val messages = (1..5).map {
            MessageEntity("id$it", "conversation", "user", "message $it", it.toLong())
        }
        val result = ContextWindowBuilder(ContextWindowConfig(maxMessages = 3, maxApproxTokens = 100))
            .build(messages)

        assertEquals(listOf("message 3", "message 4", "message 5"), result.map { it.content })
    }

    @Test
    fun keepsNewestMessagesWhenTokenBudgetIsReached() {
        val messages = listOf(
            MessageEntity("1", "c", "user", "a".repeat(40), 1),
            MessageEntity("2", "c", "assistant", "b".repeat(40), 2),
            MessageEntity("3", "c", "user", "c".repeat(40), 3)
        )
        val result = ContextWindowBuilder(ContextWindowConfig(maxMessages = 10, maxApproxTokens = 12))
            .build(messages)

        assertEquals(listOf("c".repeat(40)), result.map { it.content })
    }

    @Test
    fun trimsSingleOversizedNewestMessageToBudget() {
        val message = MessageEntity("1", "c", "user", "x".repeat(100), 1)
        val result = ContextWindowBuilder(ContextWindowConfig(maxMessages = 10, maxApproxTokens = 5))
            .build(listOf(message))

        assertEquals(20, result.single().content.length)
    }
}
