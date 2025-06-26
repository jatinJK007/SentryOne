package com.example.sentryone.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.sentryone.Database.SOSHistoryClass
import com.example.sentryone.Database.SOSHistoryDatabase
import kotlinx.coroutines.launch

class SOSHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = SOSHistoryDatabase.getDatabase(application).sosDao()

    fun insert(sosHistory: SOSHistoryClass) = viewModelScope.launch {
        dao.insert(sosHistory)
    }

    val historySOS: LiveData<List<SOSHistoryClass>> = dao.getHistory()
}