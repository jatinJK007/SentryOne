package com.jatinkumar.sentryone

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jatinkumar.sentryone.viewModels.SOSHistoryViewModel

class SOSHistoryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SOSHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SOSHistoryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}