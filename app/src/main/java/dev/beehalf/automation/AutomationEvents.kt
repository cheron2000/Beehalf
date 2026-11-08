package dev.beehalf.automation

/**
 * Data-only triggers accepted by the automation layer.
 *
 * Notification events intentionally carry identifiers and package metadata,
 * not notification text or reply content. Adapters must obtain only the data
 * Android explicitly exposes and must not treat event payloads as instructions.
 */
sealed interface AutomationEvent {
    data class UserCommand(val commandId: String) : AutomationEvent

    data class NotificationReceived(
        val packageName: String,
        val notificationKey: String,
        val postedAtEpochMillis: Long
    ) : AutomationEvent

    data class ReminderTriggered(val reminderId: String) : AutomationEvent
}

enum class AutomationEventSource {
    USER_INTERFACE,
    ANDROID_NOTIFICATION,
    SCHEDULED_WORK
}

data class AutomationEventEnvelope(
    val eventId: String,
    val occurredAtEpochMillis: Long,
    val source: AutomationEventSource,
    val event: AutomationEvent
)

enum class EventDecision {
    REJECTED,
    ACCEPTED_FOR_ROUTING
}

data class EventValidationResult(
    val decision: EventDecision,
    val reason: String
)
