package core

interface NotificationManager {
    /**
     * Enables notifications for the device.
     */
    fun enableNotifications()

    /**
     * Disables notifications for the device.
     */
    fun disableNotifications()

    /**
     * Updates notification state based on changes in VAD detection.
     * @param isVoiceDetected Boolean indicating if voice is detected.
     */
    fun onVADStateChanged(isVoiceDetected: Boolean)
}
