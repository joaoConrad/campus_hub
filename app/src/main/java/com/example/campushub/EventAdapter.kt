package com.example.campushub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.compareTo

class EventAdapter(
    private val onClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private var eventos: List<Event> = emptyList()

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvData: TextView = view.findViewById(R.id.tvData)
        val tvLocal: TextView = view.findViewById(R.id.tvLocal)
        val tvVagas: TextView = view.findViewById(R.id.tvVagas)
    }

    fun atualizar(novos: List<Event>) {
        eventos = novos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val evento = eventos[position]
        val formato = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))

        holder.tvTitulo.text = evento.titulo
        holder.tvData.text = evento.dataHora?.toDate()?.let { formato.format(it) } ?: "Data a definir"
        holder.tvLocal.text = evento.local
        holder.tvVagas.text = if (evento.vagas > 0) "${evento.vagas} vagas" else "Esgotado"

        holder.itemView.setOnClickListener { onClick(evento) }
    }

    override fun getItemCount() = eventos.size
}