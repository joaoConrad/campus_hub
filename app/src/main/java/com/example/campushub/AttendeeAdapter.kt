package com.example.campushub

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView

data class Attendee(val id: String, val nome: String, val checkin: Boolean)

class AttendeeAdapter(
    private val onToggle: (Attendee, Boolean) -> Unit
) : RecyclerView.Adapter<AttendeeAdapter.VH>() {

    private var lista: List<Attendee> = emptyList()

    class VH(val check: CheckBox) : RecyclerView.ViewHolder(check)

    fun atualizar(nova: List<Attendee>) {
        lista = nova
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendee, parent, false) as CheckBox
        return VH(view)
    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = lista[position]
        holder.check.setOnCheckedChangeListener(null)
        holder.check.text = a.nome
        holder.check.isChecked = a.checkin
        holder.check.setOnCheckedChangeListener { _, marcado -> onToggle(a, marcado) }
    }
}