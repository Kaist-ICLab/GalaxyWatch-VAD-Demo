package kaist.iclab.vad_demo

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.Settings
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kaist.iclab.vad_demo.core.model.VADModel
import kaist.iclab.vad_demo.presentation.VADApp
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val audioCollector: AudioCollector by inject() // Inject AudioCollector from Koin

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startAudioProcessing()
            } else {
                // TODO: Show a message or UI indicating that permission is required for VAD
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        checkAndRequestPermissions()

        setContent {
            VADApp()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionState = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        )

        Log.d("MainActivity", "Microphone permission state: $permissionState")

        when (permissionState) {
            PackageManager.PERMISSION_GRANTED -> {
                Log.d("MainActivity", "Microphone permission GRANTED. Starting audio processing.")
            }
            PackageManager.PERMISSION_DENIED -> {
                Log.e("MainActivity", "Microphone permission DENIED.")
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    showPermissionRationaleDialog()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    private var isVADRunning = false

    private fun toggleVAD() {
        val vadModel: VADModel by inject()  // Inject VADModel

        if (isVADRunning) {
            Log.d("MainActivity", "Pausing VAD.")
            vadModel.stop()  // Call pause
        } else {
            Log.d("MainActivity", "Starting VAD.")
            vadModel.start()  // Only starts when button is pressed
        }

        isVADRunning = !isVADRunning
    }

    /**
     * Show a dialog explaining why permission is required and direct the user to settings.
     */
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Microphone Permission Needed")
            .setMessage("This app requires microphone access for Voice Activity Detection (VAD). Please grant permission.")
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Microphone permission is required for Voice Activity Detection. Please enable it in settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }



    private fun startAudioProcessing() {
        audioCollector.start() // Start real-time audio collection
    }
}
