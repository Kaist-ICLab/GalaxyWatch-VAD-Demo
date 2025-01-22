package data

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

class NotificationManagerImpl(private val context: Context) : core.NotificationManager {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun enableNotifications() {
        if (hasPermission()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        } else {
            throw SecurityException("Notification policy access is not granted.")
        }
    }

    override fun disableNotifications() {
        if (hasPermission()) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        } else {
            throw SecurityException("Notification policy access is not granted.")
        }
    }

    override fun onVADStateChanged(isVoiceDetected: Boolean) {
        if (isVoiceDetected) {
            disableNotifications()
        } else {
            enableNotifications()
        }
    }

    private fun hasPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun requestPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        context.startActivity(intent)
    }
}
