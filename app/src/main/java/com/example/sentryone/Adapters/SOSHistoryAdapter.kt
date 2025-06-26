package com.example.sentryone.Adapters

import android.view.LayoutInflater
import com.example.sentryone.R
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sentryone.Database.SOSHistoryClass

class SOSHistoryAdapter: RecyclerView.Adapter<SOSHistoryAdapter.SOSHistoryViewHolder>() {
    private var sosHistory : List<SOSHistoryClass> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SOSHistoryViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_history_rv, parent, false)
        return SOSHistoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SOSHistoryViewHolder, position: Int) {
        val currentHistory = sosHistory[position]
        holder.tvTime.text = "Time : "+currentHistory.triggerTime as CharSequence?
        holder.longitudeTv.text ="Longitude :" +currentHistory.locationLongitude as CharSequence?
        holder.latitudeTv.text = "Latitude :"+currentHistory.locationLatitude as CharSequence?
    }

    override fun getItemCount(): Int {
        return sosHistory.size
    }

    fun submitList(newSOSHistory: List<SOSHistoryClass>) {
        sosHistory = newSOSHistory
        notifyDataSetChanged() // Notifies the adapter that the data set has changed
        // For better performance with large lists, consider using DiffUtil
    }

    class SOSHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvTime: TextView = itemView.findViewById(R.id.timeTv)
        val longitudeTv: TextView = itemView.findViewById(R.id.longitudeTv)
        val latitudeTv: TextView = itemView.findViewById(R.id.latitudeTv)
    }
}