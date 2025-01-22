package presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class VADViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VADViewModel::class.java)) {
            return VADViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
