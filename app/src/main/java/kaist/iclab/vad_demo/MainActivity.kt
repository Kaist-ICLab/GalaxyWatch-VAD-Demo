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
import kaist.iclab.vad_demo.core.model.ModelInterface
import kaist.iclab.vad_demo.core.model.VADModel
import kaist.iclab.vad_demo.core.preprocess.TarsosDSPMFCCPreprocessor
import kaist.iclab.vad_demo.presentation.VADApp
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val audioCollector: AudioCollector by inject()
    private val mfccPreprocessor: TarsosDSPMFCCPreprocessor by inject()
    private val vadModel: ModelInterface by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            VADApp() // ViewModel handles when to start processing
        }
    }

    /**private fun startAudioProcessing() {
        // Check if RECORD_AUDIO permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            // Safe to call audioCollector.start() now
            audioCollector.start()

            val audioStream = audioCollector.audioPipedInputStream
            if (audioStream != null) {
                mfccPreprocessor.init(audioStream)
                mfccPreprocessor.start()
            }
            vadModel.start()
        } else {
            // Permission is not granted, so request it or handle accordingly
            // For example, you could call requestPermissions(...) here
        }
    }**/






//    private val audioCollector: AudioCollector by inject() // Inject AudioCollector from Koin
//    private val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (isGranted) {
//                startAudioProcessing()
//            } else {
//                // TODO: Show a message or UI indicating that permission is required for VAD
//            }
//        }
//
//
//
//    private fun checkAndRequestPermissions() {
//        val permissionState = ContextCompat.checkSelfPermission(
//            this, Manifest.permission.RECORD_AUDIO
//        )
//
//        Log.d("MainActivity", "Microphone permission state: $permissionState")
//
//        when (permissionState) {
//            PackageManager.PERMISSION_GRANTED -> {
//                Log.d("MainActivity", "Microphone permission GRANTED. Starting audio processing.")
//            }
//            PackageManager.PERMISSION_DENIED -> {
//                Log.e("MainActivity", "Microphone permission DENIED.")
//                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
//                    showPermissionRationaleDialog()
//                } else {
//                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
//                }
//            }
//        }
//    }
//
//    /**
//     * Show a dialog explaining why permission is required and direct the user to settings.
//     */
//    private fun showPermissionRationaleDialog() {
//        AlertDialog.Builder(this)
//            .setTitle("Microphone Permission Needed")
//            .setMessage("This app requires microphone access for Voice Activity Detection (VAD). Please grant permission.")
//            .setPositiveButton("OK") { _, _ ->
//                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
//            }
//            .setNegativeButton("Cancel") { dialog, _ ->
//                dialog.dismiss()
//            }
//            .show()
//    }
//
//
//
//    private fun startAudioProcessing() {
//        audioCollector.start() // Start real-time audio collection
//    }
}
