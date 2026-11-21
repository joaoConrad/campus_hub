package com.example.campushub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PostAdapter(
    private val uid: String,
    private val onExcluir: (Post) -> Unit,
    private val onConteudo: (Post, FrameLayout) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var posts: List<Post> = emptyList()

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAutor: TextView = view.findViewById(R.id.tvAutorPost)
        val tvData: TextView = view.findViewById(R.id.tvDataPost)
        val tvTipo: TextView = view.findViewById(R.id.tvTipoPost)
        val conteudo: FrameLayout = view.findViewById(R.id.layoutConteudoPost)
        val btnExcluir: Button = view.findViewById(R.id.btnExcluirPost)
    }

    fun atualizar(novos: List<Post>) {
        posts = novos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val p = posts[position]
        holder.tvAutor.text = p.autorNome
        holder.tvData.text = Formatters.commentDate(p.data?.toDate())
        holder.tvTipo.text = Posts.rotulo(p.tipo)
        onConteudo(p, holder.conteudo)

        val ehMeu = Posts.podeExcluir(p.autorId, uid)
        holder.btnExcluir.visibility = if (ehMeu) View.VISIBLE else View.GONE
        holder.btnExcluir.setOnClickListener { onExcluir(p) }
    }

    override fun getItemCount() = posts.size
}