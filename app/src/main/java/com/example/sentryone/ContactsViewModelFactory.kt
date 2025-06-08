package com.example.sentryone

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sentryone.viewModels.ContactsViewModel

class ContactsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory  {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}