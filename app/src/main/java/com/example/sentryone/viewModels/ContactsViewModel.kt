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
//    we have used AndroidViewModel to get the application context
    private val dao = ContactDatabase.getDatabase(application).contactDao()
//    gets the dao from the database and let viewModel communicate with DB

//    we have launched coroutines to perform database operations as they can't be done on main thread
    fun insert(contact: EmergencyContact) = viewModelScope.launch {
        dao.insert(contact)
    }

    fun delete(contact: EmergencyContact) = viewModelScope.launch{
        dao.deleteItem(contact)
    }

    val allContacts: LiveData<List<EmergencyContact>> = dao.getAll()
//    after every changes have made the LiveData updates it and stores in a variable of list of type EmergencyContact
}