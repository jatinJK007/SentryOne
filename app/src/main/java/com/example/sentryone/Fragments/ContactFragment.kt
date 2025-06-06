package com.example.sentryone.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.sentryone.R
import com.example.sentryone.databinding.FragmentContactBinding


class ContactFragment : Fragment() {

    private var _binding : FragmentContactBinding? = null
    private val binding get() = _binding!!

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

        val addContact = binding.addContact
        addContact.setOnClickListener {
//            appropriate action needs to be taken
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}