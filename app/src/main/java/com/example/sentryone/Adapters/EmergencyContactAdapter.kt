package com.example.sentryone.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sentryone.Database.EmergencyContact
import com.example.sentryone.R

class EmergencyContactAdapter(private val onDeleteClick: (EmergencyContact) -> Unit): RecyclerView.Adapter<EmergencyContactAdapter.EmergencyContactViewHolder>() {

    private var contacts: List<EmergencyContact> = emptyList()

    // ViewHolder class to hold the views for each item
    class EmergencyContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvContactName: TextView = itemView.findViewById(R.id.tvContactName)
        val tvPhoneNumber: TextView = itemView.findViewById(R.id.tvPhoneNumber)
        val btnDel : ImageButton = itemView.findViewById(R.id.btnDeleteContact)
    }

    // Called when RecyclerView needs a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencyContactViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return EmergencyContactViewHolder(itemView)
    }

    // Called by RecyclerView to display the data at the specified position
    override fun onBindViewHolder(holder: EmergencyContactViewHolder, position: Int) {
        val currentContact = contacts[position]
        holder.tvContactName.text = currentContact.name
        holder.tvPhoneNumber.text = currentContact.phoneNumber
        holder.btnDel.setOnClickListener {
            onDeleteClick(currentContact)
        }
    }

    // Returns the total number of items in the data set
    override fun getItemCount(): Int {
        return contacts.size
    }

    // Public method to update the data in the adapter
    fun submitList(newContacts: List<EmergencyContact>) {
        contacts = newContacts
        notifyDataSetChanged() // Notifies the adapter that the data set has changed
        // For better performance with large lists, consider using DiffUtil
    }

//    added via swipe to del functionality
    fun getItemAt(position: Int): EmergencyContact {
        return contacts[position]
    }}