package com.jatinkumar.sentryone.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jatinkumar.sentryone.Database.EmergencyContact
import com.jatinkumar.sentryone.R

class EmergencyContactAdapter(private val onDeleteClick: (EmergencyContact) -> Unit): RecyclerView.Adapter<EmergencyContactAdapter.EmergencyContactViewHolder>() {

    private var contacts: List<EmergencyContact> = emptyList()

    class EmergencyContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvContactName: TextView = itemView.findViewById(R.id.tvContactName)
        val tvPhoneNumber: TextView = itemView.findViewById(R.id.tvPhoneNumber)
        val btnDel : ImageButton = itemView.findViewById(R.id.btnDeleteContact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencyContactViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return EmergencyContactViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EmergencyContactViewHolder, position: Int) {
        val currentContact = contacts[position]
        holder.tvContactName.text = currentContact.name
        holder.tvPhoneNumber.text = currentContact.phoneNumber
        holder.btnDel.setOnClickListener {
            onDeleteClick(currentContact)
        }
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    fun submitList(newContacts: List<EmergencyContact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): EmergencyContact {
        return contacts[position]
    }
}