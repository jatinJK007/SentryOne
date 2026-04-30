package com.jatinkumar.sentryone.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.jatinkumar.sentryone.Database.ContactDatabase
import com.jatinkumar.sentryone.Database.EmergencyContact
import kotlinx.coroutines.launch

class ContactsViewModel (application: Application) : AndroidViewModel(application) {
    private val dao = ContactDatabase.getDatabase(application).contactDao()

    fun insert(contact: EmergencyContact) = viewModelScope.launch {
        dao.insert(contact)
    }

    fun delete(contact: EmergencyContact) = viewModelScope.launch{
        dao.deleteItem(contact)
    }

    val allContacts: LiveData<List<EmergencyContact>> = dao.getAll()
}