package presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import data.AudioInputImpl
import presentation.theme.VADDemoTheme

class MainActivity : ComponentActivity() {
    private lateinit var audioInput: AudioInputImpl

    // Permission request launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permission granted! You can start recording.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied! Audio recording is required for this feature.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the AudioInputImpl with the Activity context
        audioInput = AudioInputImpl(this)

        setContent {
            val viewModel: VADViewModel = viewModel()
            VADDemoTheme {
                VADApp(
                    viewModel = viewModel,
                    checkPermission = { checkAndRequestPermission() }, // Pass permission handler
                    audioInput = audioInput // Pass AudioInputImpl to the app
                )
            }
        }
    }

    // Check and request RECORD_AUDIO permission
    private fun checkAndRequestPermission(): Boolean {
        val permissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            // Request the permission
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        return permissionGranted
    }
}

@Composable
fun VADApp(
    viewModel: VADViewModel,
    checkPermission: () -> Boolean,
    audioInput: AudioInputImpl
) {
    val isRunning = viewModel.isRunning.collectAsState().value
    val isDetected = viewModel.isDetected.collectAsState().value

    VADScreen(
        isRunning = isRunning,
        isDetected = isDetected,
        onStart = {
            if (checkPermission()) { // Ensure permissions are granted
                audioInput.startRecording() // Start recording using AudioInputImpl
                viewModel.startVAD()
            }
        },
        onStop = {
            audioInput.stopRecording() // Stop recording
            viewModel.stopVAD()
        }
    )
}
