package dev.beehalf.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationEventPolicyTest {
    private val now = 10_000L

    @Test
    fun validUserCommandIsAcceptedOnlyForRouting() {
        val result = AutomationEventPolicy.evaluate(
            envelope(
                source = AutomationEventSource.USER_INTERFACE,
                event = AutomationEvent.UserCommand("command-1")
            )
        )

        assertEquals(EventDecision.ACCEPTED_FOR_ROUTING, result.decision)
        assertTrue(result.reason.contains("No action has been authorized or executed"))
    }

    @Test
    fun notificationFromWrongSourceIsRejected() {
        val result = AutomationEventPolicy.evaluate(
            envelope(
                source = AutomationEventSource.USER_INTERFACE,
                event = AutomationEvent.NotificationReceived(
                    packageName = "com.example.chat",
                    notificationKey = "key-1",
                    postedAtEpochMillis = now - 100
                )
            )
        )

        assertEquals(EventDecision.REJECTED, result.decision)
    }

    @Test
    fun validNotificationCarriesMetadataWithoutMessageBody() {
        val event = AutomationEvent.NotificationReceived(
            packageName = "com.example.chat",
            notificationKey = "key-1",
            postedAtEpochMillis = now - 100
        )
        val result = AutomationEventPolicy.evaluate(
            envelope(AutomationEventSource.ANDROID_NOTIFICATION, event)
        )

        assertEquals(EventDecision.ACCEPTED_FOR_ROUTING, result.decision)
    }

    @Test
    fun malformedNotificationPackageIsRejected() {
        val result = AutomationEventPolicy.evaluate(
            envelope(
                AutomationEventSource.ANDROID_NOTIFICATION,
                AutomationEvent.NotificationReceived(
                    packageName = "not a package",
                    notificationKey = "key-1",
                    postedAtEpochMillis = now - 100
                )
            )
        )

        assertEquals(EventDecision.REJECTED, result.decision)
    }

    @Test
    fun futureEventTimestampIsRejected() {
        val result = AutomationEventPolicy.evaluate(
            AutomationEventEnvelope(
                eventId = "event-1",
                occurredAtEpochMillis = now + 1,
                source = AutomationEventSource.USER_INTERFACE,
                event = AutomationEvent.UserCommand("command-1")
            ),
            nowEpochMillis = now
        )

        assertEquals(EventDecision.REJECTED, result.decision)
    }

    @Test
    fun reminderMustComeFromScheduledWork() {
        val result = AutomationEventPolicy.evaluate(
            envelope(
                AutomationEventSource.ANDROID_NOTIFICATION,
                AutomationEvent.ReminderTriggered("reminder-1")
            )
        )

        assertEquals(EventDecision.REJECTED, result.decision)
    }

    @Test
    fun validReminderIsAcceptedForRouting() {
        val result = AutomationEventPolicy.evaluate(
            envelope(
                AutomationEventSource.SCHEDULED_WORK,
                AutomationEvent.ReminderTriggered("reminder-1")
            )
        )

        assertEquals(EventDecision.ACCEPTED_FOR_ROUTING, result.decision)
    }

    private fun envelope(
        source: AutomationEventSource,
        event: AutomationEvent
    ) = AutomationEventEnvelope(
        eventId = "event-1",
        occurredAtEpochMillis = now,
        source = source,
        event = event
    )
}
