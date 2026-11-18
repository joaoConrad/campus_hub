package com.example.campushub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.LruCache
import android.graphics.Bitmap
import android.widget.ImageView
class EventAdapter(
    private val onClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private var eventos: List<Event> = emptyList()
    private val cache = LruCache<String, Bitmap>(30)

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvData: TextView = view.findViewById(R.id.tvData)
        val tvLocal: TextView = view.findViewById(R.id.tvLocal)
        val tvVagas: TextView = view.findViewById(R.id.tvVagas)
        val imgEvento: ImageView = view.findViewById(R.id.imgEvento)
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

        holder.tvTitulo.text = evento.titulo
        holder.tvData.text = Formatters.dateTime(evento.dataHora?.toDate())
        holder.tvLocal.text = evento.local
        holder.tvVagas.text = Formatters.seats(evento.vagas)

        val chave = "${evento.id}_${evento.imagem.length}"
        val miniatura = cache.get(chave)
            ?: ImageUtils.toBitmap(evento.imagem, 300)?.also { cache.put(chave, it) }
        if (miniatura != null) {
            holder.imgEvento.setImageBitmap(miniatura)
            holder.imgEvento.visibility = View.VISIBLE
        } else {
            holder.imgEvento.setImageDrawable(null)
            holder.imgEvento.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(evento) }
    }

    override fun getItemCount() = eventos.size
}