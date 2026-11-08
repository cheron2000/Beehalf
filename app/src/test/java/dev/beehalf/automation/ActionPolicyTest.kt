package dev.beehalf.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionPolicyTest {
    private val now = 1_800_000_000_000L

    private fun proposal(action: AutomationAction) = ActionProposal(
        id = "proposal-1",
        action = action,
        summary = "Requested action",
        createdAtEpochMillis = now - 1_000L
    )

    @Test
    fun validOpenAppProposalRequiresApproval() {
        val result = ActionPolicy.evaluate(
            proposal(AutomationAction.OpenApp("com.example.calendar")),
            now
        )

        assertEquals(ActionDecision.APPROVAL_REQUIRED, result.decision)
        assertTrue(result.reason.contains("nothing has been executed"))
    }

    @Test
    fun invalidPackageNameIsRejected() {
        val result = ActionPolicy.evaluate(
            proposal(AutomationAction.OpenApp("com.example;rm")),
            now
        )

        assertEquals(ActionDecision.REJECTED, result.decision)
    }

    @Test
    fun reminderInPastIsRejected() {
        val result = ActionPolicy.evaluate(
            proposal(AutomationAction.CreateReminder("Pay bill", now - 1L)),
            now
        )

        assertEquals(ActionDecision.REJECTED, result.decision)
    }

    @Test
    fun httpsUrlRequiresApproval() {
        val result = ActionPolicy.evaluate(
            proposal(AutomationAction.OpenWebPage("https://example.com/path")),
            now
        )

        assertEquals(ActionDecision.APPROVAL_REQUIRED, result.decision)
    }

    @Test
    fun nonHttpsUrlIsRejected() {
        val result = ActionPolicy.evaluate(
            proposal(AutomationAction.OpenWebPage("javascript:alert(1)")),
            now
        )

        assertEquals(ActionDecision.REJECTED, result.decision)
    }

    @Test
    fun unsupportedActionIsRejected() {
        val result = ActionPolicy.evaluate(
            proposal(AutomationAction.Unsupported("Run arbitrary shell command")),
            now
        )

        assertEquals(ActionDecision.REJECTED, result.decision)
    }

    @Test
    fun blankProposalSummaryIsRejected() {
        val invalid = proposal(AutomationAction.OpenApp("com.example.calendar"))
            .copy(summary = " ")

        assertEquals(ActionDecision.REJECTED, ActionPolicy.evaluate(invalid, now).decision)
    }
}
