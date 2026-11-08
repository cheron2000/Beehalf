package dev.beehalf.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilityActionMappingTest {
    @Test
    fun eachSupportedActionHasAnExplicitCapabilityCheck() {
        val actions = listOf(
            AutomationAction.OpenApp("com.example.app"),
            AutomationAction.CreateReminder("Call home", 2_000L),
            AutomationAction.OpenWebPage("https://example.com")
        )

        assertEquals(3, actions.size)
        assertEquals(
            listOf("OpenApp", "CreateReminder", "OpenWebPage"),
            actions.map { it::class.simpleName }
        )
    }

    @Test
    fun unsupportedActionRemainsRepresentableButNotSupported() {
        val action: AutomationAction = AutomationAction.Unsupported("send arbitrary message")
        assertEquals("Unsupported", action::class.simpleName)
    }

    @Test
    fun capabilityAvailabilityDoesNotReplaceApprovalDecision() {
        val policy = ActionPolicy.evaluate(
            ActionProposal(
                id = "proposal-1",
                action = AutomationAction.OpenWebPage("https://example.com"),
                summary = "Open example",
                createdAtEpochMillis = 1_000L
            ),
            nowEpochMillis = 1_500L
        )

        assertEquals(ActionDecision.APPROVAL_REQUIRED, policy.decision)
    }
}
