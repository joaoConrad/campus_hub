package com.example.campushub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class CommentAdapter(
    private val uid: String,
    private val onEditar: (Comment) -> Unit,
    private val onExcluir: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private var comentarios: List<Comment> = emptyList()

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAutor: TextView = view.findViewById(R.id.tvAutorComentario)
        val tvData: TextView = view.findViewById(R.id.tvDataComentario)
        val tvTexto: TextView = view.findViewById(R.id.tvTextoComentario)
        val layoutAcoes: View = view.findViewById(R.id.layoutAcoesComentario)
        val btnEditar: Button = view.findViewById(R.id.btnEditarComentario)
        val btnExcluir: Button = view.findViewById(R.id.btnExcluirComentario)
    }

    fun atualizar(novos: List<Comment>) {
        comentarios = novos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val c = comentarios[position]

        holder.tvAutor.text = c.autorNome
        holder.tvData.text = Formatters.commentDate(c.data?.toDate())
        holder.tvTexto.text = c.texto

        val ehMeu = c.autorId == uid
        holder.layoutAcoes.visibility = if (ehMeu) View.VISIBLE else View.GONE
        holder.btnEditar.setOnClickListener { onEditar(c) }
        holder.btnExcluir.setOnClickListener { onExcluir(c) }
    }

    override fun getItemCount() = comentarios.size
}