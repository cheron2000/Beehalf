package dev.beehalf.automation

/**
 * Validates event shape and source consistency only.
 *
 * Acceptance means an event may be routed to a future handler. It does not
 * authorize an action, trust event text, or execute Android APIs.
 */
object AutomationEventPolicy {
    private val packageNamePattern =
        Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    fun evaluate(
        envelope: AutomationEventEnvelope,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): EventValidationResult {
        if (envelope.eventId.isBlank() || envelope.eventId.length > 128) {
            return rejected("Event ID is missing or too long.")
        }
        if (envelope.occurredAtEpochMillis <= 0L ||
            envelope.occurredAtEpochMillis > nowEpochMillis
        ) {
            return rejected("Event timestamp is invalid.")
        }

        val error = when (val event = envelope.event) {
            is AutomationEvent.UserCommand -> {
                if (envelope.source != AutomationEventSource.USER_INTERFACE) {
                    "User commands must originate from the user interface."
                } else if (event.commandId.isBlank() || event.commandId.length > 128) {
                    "Command ID is missing or too long."
                } else null
            }

            is AutomationEvent.NotificationReceived -> {
                when {
                    envelope.source != AutomationEventSource.ANDROID_NOTIFICATION ->
                        "Notification events must originate from the Android notification adapter."
                    !packageNamePattern.matches(event.packageName) ->
                        "Notification package name is invalid."
                    event.notificationKey.isBlank() || event.notificationKey.length > 512 ->
                        "Notification key is missing or too long."
                    event.postedAtEpochMillis <= 0L ||
                        event.postedAtEpochMillis > envelope.occurredAtEpochMillis ->
                        "Notification timestamp is invalid."
                    else -> null
                }
            }

            is AutomationEvent.ReminderTriggered -> {
                if (envelope.source != AutomationEventSource.SCHEDULED_WORK) {
                    "Reminder events must originate from scheduled work."
                } else if (event.reminderId.isBlank() || event.reminderId.length > 128) {
                    "Reminder ID is missing or too long."
                } else null
            }
        }

        return if (error == null) {
            EventValidationResult(
                decision = EventDecision.ACCEPTED_FOR_ROUTING,
                reason = "Event shape and source are valid. No action has been authorized or executed."
            )
        } else {
            rejected(error)
        }
    }

    private fun rejected(reason: String) =
        EventValidationResult(EventDecision.REJECTED, reason)
}
