package com.example.sentryone.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.sentryone.Database.ContactDatabase
import com.example.sentryone.Database.EmergencyContact
import kotlinx.coroutines.launch

class ContactsViewModel (application: Application) : AndroidViewModel(application) {
    private val dao = ContactDatabase.getDatabase(application).contactDao()

    fun insert(contact: EmergencyContact) = viewModelScope.launch {
        Log.d("TAGviewmodel",   "insert: in contact viewmodel the issue must be here ")
        dao.insert(contact)
        Log.d("TAGvieemodel", "insert: the num is inserted in db ")
    }

    fun delete(contact: EmergencyContact) = viewModelScope.launch{
        dao.deleteItem(contact)
    }

    val allContacts: LiveData<List<EmergencyContact>> = dao.getAll()
}