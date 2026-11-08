package dev.beehalf.automation

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.net.Uri

enum class CapabilityStatus {
    AVAILABLE,
    UNAVAILABLE
}

data class CapabilityResult(
    val status: CapabilityStatus,
    val explanation: String
)

/**
 * Checks whether Android currently exposes a handler for a proposed action.
 *
 * This class never starts an activity, schedules an alarm, or executes an
 * action. A positive result only means the relevant Android handler exists.
 */
class AndroidCapabilityChecker(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun check(action: AutomationAction): CapabilityResult = when (action) {
        is AutomationAction.OpenApp -> checkOpenApp(action.packageName)
        is AutomationAction.CreateReminder -> checkReminder()
        is AutomationAction.OpenWebPage -> checkWebPage(action.url)
        is AutomationAction.Unsupported -> unavailable("This action type is not supported.")
    }

    private fun checkOpenApp(packageName: String): CapabilityResult {
        if (packageName.isBlank()) return unavailable("App package name is missing.")
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            available("A launchable activity is visible to Android.")
        } else {
            unavailable("No launchable activity is available or visible for this app.")
        }
    }

    private fun checkReminder(): CapabilityResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        return if (packageManager.resolveActivity(intent, 0) != null) {
            available("An alarm app can handle a user-confirmed reminder handoff.")
        } else {
            unavailable("No compatible alarm app is available.")
        }
    }

    private fun checkWebPage(url: String): CapabilityResult {
        val uri = runCatching { Uri.parse(url) }.getOrNull()
            ?: return unavailable("Web address is invalid.")
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
            return unavailable("Only absolute HTTPS web addresses are supported.")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        return if (packageManager.resolveActivity(intent, 0) != null) {
            available("An app can open this HTTPS address.")
        } else {
            unavailable("No app can handle HTTPS links.")
        }
    }

    private fun available(message: String) =
        CapabilityResult(CapabilityStatus.AVAILABLE, message)

    private fun unavailable(message: String) =
        CapabilityResult(CapabilityStatus.UNAVAILABLE, message)
}
