package presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import presentation.theme.VADDemoTheme

class MainActivity : ComponentActivity() {

    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Audio recording permission is required.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: VADViewModel = viewModel(factory = VADViewModelFactory(this))
            VADDemoTheme {
                VADApp(
                    viewModel = viewModel,
                    checkPermissions = { checkAndRequestPermissions() }
                )
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val audioPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!audioPermissionGranted) {
            // Request the permission
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        val notificationPermissionGranted = isNotificationPolicyAccessGranted()
        if (!notificationPermissionGranted) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Notification policy access settings are not available on this device.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to open notification policy settings.", Toast.LENGTH_SHORT).show()
            }
        }

        return audioPermissionGranted && notificationPermissionGranted
    }


    private fun isNotificationPolicyAccessGranted(): Boolean {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }
}

@Composable
fun VADApp(
    viewModel: VADViewModel,
    checkPermissions: () -> Boolean
) {
    val isRunning = viewModel.isRunning.collectAsState().value
    val isDetected = viewModel.isDetected.collectAsState().value

    VADScreen(
        isRunning = isRunning,
        isDetected = isDetected,
        onStart = {
            if (checkPermissions()) {
                viewModel.startProcessing()
            }
        },
        onStop = {
            viewModel.stopProcessing()
        }
    )
}
