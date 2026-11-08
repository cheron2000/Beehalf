package dev.beehalf.automation

import java.net.URI

/**
 * Pure policy validation. A valid proposal is never auto-approved:
 * the only positive outcome is APPROVAL_REQUIRED.
 */
object ActionPolicy {
    private val packageNamePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    fun evaluate(
        proposal: ActionProposal,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): ActionPolicyResult {
        if (proposal.id.isBlank() || proposal.id.length > 128) {
            return rejected("Proposal ID is missing or too long.")
        }
        if (proposal.summary.isBlank() || proposal.summary.length > 240) {
            return rejected("Proposal summary must contain 1–240 characters.")
        }
        if (proposal.createdAtEpochMillis <= 0L || proposal.createdAtEpochMillis > nowEpochMillis) {
            return rejected("Proposal timestamp is invalid.")
        }

        val validationError = when (val action = proposal.action) {
            is AutomationAction.OpenApp -> {
                if (!packageNamePattern.matches(action.packageName)) {
                    "App package name is invalid."
                } else null
            }
            is AutomationAction.CreateReminder -> {
                when {
                    action.title.isBlank() || action.title.length > 160 ->
                        "Reminder title must contain 1–160 characters."
                    action.triggerAtEpochMillis <= nowEpochMillis ->
                        "Reminder time must be in the future."
                    else -> null
                }
            }
            is AutomationAction.OpenWebPage -> validateWebUrl(action.url)
            is AutomationAction.Unsupported -> "This action type is not supported."
        }

        return if (validationError != null) {
            rejected(validationError)
        } else {
            ActionPolicyResult(
                decision = ActionDecision.APPROVAL_REQUIRED,
                reason = "Valid proposal. Explicit user approval is required; nothing has been executed."
            )
        }
    }

    private fun validateWebUrl(value: String): String? {
        if (value.isBlank() || value.length > 2048) return "Web address is missing or too long."
        return runCatching { URI(value) }.fold(
            onSuccess = { uri ->
                if (uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) {
                    null
                } else {
                    "Only absolute HTTPS web addresses are allowed."
                }
            },
            onFailure = { "Web address is invalid." }
        )
    }

    private fun rejected(reason: String) =
        ActionPolicyResult(ActionDecision.REJECTED, reason)
}
