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
import androidx.lifecycle.ViewModelProvider
import presentation.theme.VADDemoTheme

class MainActivity : ComponentActivity() {
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

        // Initialize VADViewModel with VADViewModelFactory
        val viewModel: VADViewModel = ViewModelProvider(
            this,
            VADViewModelFactory(this) // Pass the Activity context to the factory
        )[VADViewModel::class.java]

        setContent {
            VADDemoTheme {
                VADApp(
                    viewModel = viewModel,
                    checkPermission = { checkAndRequestPermission() }
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
    checkPermission: () -> Boolean
) {
    val isRunning = viewModel.isRunning.collectAsState().value
    val isDetected = viewModel.isDetected.collectAsState().value

    VADScreen(
        isRunning = isRunning,
        isDetected = isDetected,
        onStart = {
            if (checkPermission()) { // Ensure permissions are granted
                viewModel.startProcessing() // Start audio processing via ViewModel
            }
        },
        onStop = {
            viewModel.stopProcessing() // Stop audio processing via ViewModel
        }
    )
}
