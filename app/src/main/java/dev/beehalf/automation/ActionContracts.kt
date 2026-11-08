package dev.beehalf.automation

/**
 * Typed, data-only actions proposed by the assistant.
 *
 * These contracts do not execute Android APIs. Every proposal must pass
 * ActionPolicy and receive explicit user approval before a future executor
 * can consider it.
 */
sealed interface AutomationAction {
    data class OpenApp(val packageName: String) : AutomationAction
    data class CreateReminder(
        val title: String,
        val triggerAtEpochMillis: Long
    ) : AutomationAction
    data class OpenWebPage(val url: String) : AutomationAction
    data class Unsupported(val description: String) : AutomationAction
}

data class ActionProposal(
    val id: String,
    val action: AutomationAction,
    val summary: String,
    val createdAtEpochMillis: Long
)

enum class ActionDecision {
    REJECTED,
    APPROVAL_REQUIRED
}

data class ActionPolicyResult(
    val decision: ActionDecision,
    val reason: String
)
