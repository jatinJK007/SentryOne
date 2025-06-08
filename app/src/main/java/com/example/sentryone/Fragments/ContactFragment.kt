package com.example.sentryone.Fragments

import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.sentryone.Database.EmergencyContact
import com.example.sentryone.databinding.FragmentContactBinding
import com.example.sentryone.viewModels.ContactsViewModel
import android.Manifest
import android.util.Log
import androidx.fragment.app.viewModels
import com.example.sentryone.ContactsViewModelFactory


class ContactFragment : Fragment() {

    private var _binding : FragmentContactBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactsViewModel by viewModels{ ContactsViewModelFactory(requireActivity().application) }
    private lateinit var contactSuggestions: List<Pair<String, String>> // name, phone


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding= FragmentContactBinding.inflate(inflater,container,false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        checkContactPermission()

        binding.btnAddContact.setOnClickListener {
            // Check if contactSuggestions has been initialized before using it
            if (!::contactSuggestions.isInitialized) {
                Toast.makeText(requireContext(), "Contacts not loaded yet. Please try again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val entered = binding.contactAutoComplete.text.toString()
            Log.d("TAG", "onViewCreated: in contact frag num is entered")
            val match = contactSuggestions.find { it.second.contains(entered) }
            if (match != null) {
                val contact = EmergencyContact(name = match.first, phoneNumber = match.second, type = "Friend")
                Log.d("TAG", "onViewCreated: contact is $contact and contact is selected")
                viewModel.insert(contact)
                Toast.makeText(requireContext(), "Contact saved!", Toast.LENGTH_SHORT).show()
                binding.contactAutoComplete.setText("")
            } else {
                Toast.makeText(requireContext(), "No matching contact found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadContactsFromSystem() {
        Log.d("ContactLoaded", "loadContactsFromSystem: contact is loaded from system")
        val suggestions = mutableListOf<Pair<String, String>>()
        val cursor = requireActivity().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                suggestions.add(Pair(name, number))
            }
        }

        contactSuggestions = suggestions
        val numbers = suggestions.map { it.second }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, numbers)
        binding.contactAutoComplete.setAdapter(adapter)
    }

    private fun checkContactPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 101)
        } else {
            loadContactsFromSystem()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadContactsFromSystem()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}