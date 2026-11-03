package com.example.campushub

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Locale
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.RatingBar

// Tela de detalhes: recebe o id de um evento, busca ele no Firestore e mostra os dados
class EventDetailsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var tvTitulo: TextView
    private lateinit var tvDescricao: TextView
    private lateinit var tvData: TextView
    private lateinit var tvHorario: TextView
    private lateinit var tvLocal: TextView
    private lateinit var tvVagas: TextView
    private lateinit var tvCategoria: TextView
    private lateinit var btnInscrever: Button
    private lateinit var layoutAvaliacao: View
    private lateinit var ratingBarAvaliacao: RatingBar
    private lateinit var btnAvaliar: Button
    private lateinit var tvMediaDetalhe: TextView
    private lateinit var ratingRef: DocumentReference

    private lateinit var btnVoltar: Button

    private var eventId: String = ""
    private var uid: String = ""

    private var inscrito = false

    private lateinit var eventRef: DocumentReference
    private lateinit var regRef: DocumentReference
    private lateinit var btnFavoritar: Button
    private lateinit var favRef: DocumentReference
    private var favoritado = false

    private lateinit var recyclerComentarios: RecyclerView
    private lateinit var editComentario: EditText
    private lateinit var btnComentar: Button
    private lateinit var tvNenhumComentario: TextView
    private lateinit var comentariosAdapter: CommentAdapter
    private var nomeUsuario: String = "Aluno"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        db = FirebaseFirestore.getInstance()

        tvTitulo = findViewById(R.id.tvTituloDetalhe)
        tvDescricao = findViewById(R.id.tvDescricaoDetalhe)
        tvData = findViewById(R.id.tvDataDetalhe)
        tvHorario = findViewById(R.id.tvHorarioDetalhe)
        tvLocal = findViewById(R.id.tvLocalDetalhe)
        tvVagas = findViewById(R.id.tvVagasDetalhe)
        tvCategoria = findViewById(R.id.tvCategoriaDetalhe)
        btnInscrever = findViewById(R.id.btnInscrever)
        btnFavoritar = findViewById(R.id.btnFavoritar)


        btnVoltar = findViewById(R.id.btnVoltar)
        btnFavoritar = findViewById(R.id.btnFavoritar)

        btnVoltar.setOnClickListener {
            finish()
        }

        val usuario = FirebaseAuth.getInstance().currentUser
        eventId = intent.getStringExtra("eventId") ?: ""

        if (usuario == null || eventId.isEmpty()) {
            Toast.makeText(this, "Evento não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        uid = usuario.uid

        eventRef = db.collection("events").document(eventId)
        regRef = db.collection("registrations").document("${uid}_$eventId")
        favRef = db.collection("favorites").document("${uid}_$eventId")

        btnFavoritar.isEnabled = false
        btnFavoritar.setOnClickListener { alternarFavorito() }

        btnInscrever.isEnabled = false

        btnInscrever.setOnClickListener {
            if (inscrito) cancelarInscricao() else inscrever()
        }

        carregarEvento()
        verificarFavorito()
        configurarComentarios()
        configurarAvaliacao()
    }

    private fun carregarEvento() {
        db.collection("events").document(eventId).get()
            .addOnSuccessListener { documento ->
                val evento = documento.toObject(Event::class.java)

                if (evento == null) {
                    Toast.makeText(this, "Evento não encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    mostrarEvento(evento)
                    verificarInscricao(evento)
                }
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar evento: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun mostrarEvento(evento: Event) {
        val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
        val formatoHora = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR"))

        val dataHora = evento.dataHora?.toDate()

        tvTitulo.text = evento.titulo
        tvCategoria.text = "Categoria: ${evento.categoria}"
        tvDescricao.text = evento.descricao
        tvData.text = "Data: " + (dataHora?.let { formatoData.format(it) } ?: "a definir")
        tvHorario.text = "Horário: " + (dataHora?.let { formatoHora.format(it) } ?: "a definir")
        tvLocal.text = "Local: ${evento.local}"
        tvVagas.text = "Vagas restantes: ${evento.vagas}"
    }

    private fun verificarInscricao(evento: Event) {
        regRef.get()
            .addOnSuccessListener { documento ->
                inscrito = documento.exists()
                atualizarBotao(evento)
                atualizarAvaliacao(evento)
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao verificar inscrição: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarBotao(evento: Event) {
        when {
            evento.estaEncerrado() -> {
                btnInscrever.text = "Evento encerrado"
                btnInscrever.isEnabled = false
            }
            inscrito -> {
                btnInscrever.text = "Cancelar inscrição"
                btnInscrever.isEnabled = true
            }
            evento.vagas > 0 -> {
                btnInscrever.text = "Inscrever-se"
                btnInscrever.isEnabled = true
            }
            else -> {
                btnInscrever.text = "Esgotado"
                btnInscrever.isEnabled = false
            }
        }
    }

    private fun inscrever() {
        btnInscrever.isEnabled = false

        db.runTransaction { transaction ->

            val eventoDoc = transaction.get(eventRef)
            val inscricaoDoc = transaction.get(regRef)
            val vagas = eventoDoc.getLong("vagas") ?: 0L

            val dataHora = eventoDoc.getTimestamp("dataHora")?.toDate()
            if (dataHora != null && dataHora.before(java.util.Date())) {
                throw FirebaseFirestoreException(
                    "Este evento já foi encerrado",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }

            if (inscricaoDoc.exists()) {
                throw FirebaseFirestoreException(
                    "Você já está inscrito neste evento",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }
            if (vagas <= 0) {
                throw FirebaseFirestoreException(
                    "As vagas deste evento esgotaram",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }

            transaction.update(eventRef, "vagas", vagas - 1)
            transaction.set(
                regRef,
                hashMapOf(
                    "userId" to uid,
                    "eventId" to eventId,
                    "dataInscricao" to FieldValue.serverTimestamp()
                )
            )
            null
        }
            .addOnSuccessListener {
                Toast.makeText(this, "Inscrição realizada!", Toast.LENGTH_SHORT).show()
                carregarEvento()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, erro.message, Toast.LENGTH_LONG).show()
                carregarEvento()
            }
    }

    private fun cancelarInscricao() {
        btnInscrever.isEnabled = false

        db.runTransaction { transaction ->
            val eventoDoc = transaction.get(eventRef)
            val inscricaoDoc = transaction.get(regRef)
            val vagas = eventoDoc.getLong("vagas") ?: 0L

            if (!inscricaoDoc.exists()) {
                throw FirebaseFirestoreException(
                    "Você não está inscrito neste evento",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }

            transaction.update(eventRef, "vagas", vagas + 1)
            transaction.delete(regRef)
            null
        }
            .addOnSuccessListener {
                Toast.makeText(this, "Inscrição cancelada", Toast.LENGTH_SHORT).show()
                carregarEvento()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, erro.message, Toast.LENGTH_LONG).show()
                carregarEvento()
            }
    }

    private fun verificarFavorito() {
        favRef.get()
            .addOnSuccessListener { documento ->
                favoritado = documento.exists()
                atualizarBotaoFavorito()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao verificar favorito: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarBotaoFavorito() {
        btnFavoritar.text = if (favoritado) "Remover dos favoritos" else "Favoritar"
        btnFavoritar.isEnabled = true
    }

    private fun alternarFavorito() {
        btnFavoritar.isEnabled = false

        if (favoritado) {
            favRef.delete()
                .addOnSuccessListener {
                    favoritado = false
                    atualizarBotaoFavorito()
                    Toast.makeText(this, "Removido dos favoritos", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { erro ->
                    btnFavoritar.isEnabled = true
                    Toast.makeText(this, "Erro ao remover: ${erro.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            favRef.set(
                hashMapOf(
                    "userId" to uid,
                    "eventId" to eventId,
                    "dataFavorito" to FieldValue.serverTimestamp()
                )
            )
                .addOnSuccessListener {
                    favoritado = true
                    atualizarBotaoFavorito()
                    Toast.makeText(this, "Adicionado aos favoritos", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { erro ->
                    btnFavoritar.isEnabled = true
                    Toast.makeText(this, "Erro ao favoritar: ${erro.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun configurarComentarios() {
        recyclerComentarios = findViewById(R.id.recyclerComentarios)
        editComentario = findViewById(R.id.editComentario)
        btnComentar = findViewById(R.id.btnComentar)
        tvNenhumComentario = findViewById(R.id.tvNenhumComentario)

        comentariosAdapter = CommentAdapter(
            uid,
            { c -> editarComentario(c) },
            { c -> excluirComentario(c) }
        )
        recyclerComentarios.layoutManager = LinearLayoutManager(this)
        recyclerComentarios.adapter = comentariosAdapter

        db.collection("users").document(uid).get()
            .addOnSuccessListener { nomeUsuario = it.getString("nome") ?: "Aluno" }

        btnComentar.setOnClickListener { publicarComentario() }
        carregarComentarios()
    }

    private fun carregarComentarios() {
        db.collection("comments")
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { resultado ->
                val lista = resultado.toObjects(Comment::class.java)
                    .sortedByDescending { it.data?.seconds ?: Long.MAX_VALUE }
                comentariosAdapter.atualizar(lista)
                tvNenhumComentario.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar comentários: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun publicarComentario() {
        val texto = editComentario.text.toString().trim()
        if (texto.isEmpty()) {
            Toast.makeText(this, "Escreva um comentário", Toast.LENGTH_SHORT).show()
            return
        }

        btnComentar.isEnabled = false

        db.collection("comments").add(
            hashMapOf(
                "eventId" to eventId,
                "autorId" to uid,
                "autorNome" to nomeUsuario,
                "texto" to texto,
                "data" to FieldValue.serverTimestamp()
            )
        )
            .addOnSuccessListener {
                editComentario.text.clear()
                btnComentar.isEnabled = true
                carregarComentarios()
            }
            .addOnFailureListener { erro ->
                btnComentar.isEnabled = true
                Toast.makeText(this, "Erro ao comentar: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun editarComentario(c: Comment) {
        if (c.autorId != uid) return

        val campo = EditText(this)
        campo.setText(c.texto)

        AlertDialog.Builder(this)
            .setTitle("Editar comentário")
            .setView(campo)
            .setPositiveButton("Salvar") { _, _ ->
                val novo = campo.text.toString().trim()
                if (novo.isEmpty()) {
                    Toast.makeText(this, "O comentário não pode ficar vazio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                db.collection("comments").document(c.id).update("texto", novo)
                    .addOnSuccessListener { carregarComentarios() }
                    .addOnFailureListener { erro ->
                        Toast.makeText(this, "Erro ao editar: ${erro.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirComentario(c: Comment) {
        if (c.autorId != uid) return

        AlertDialog.Builder(this)
            .setTitle("Excluir comentário")
            .setMessage("Deseja excluir este comentário?")
            .setPositiveButton("Excluir") { _, _ ->
                db.collection("comments").document(c.id).delete()
                    .addOnSuccessListener { carregarComentarios() }
                    .addOnFailureListener { erro ->
                        Toast.makeText(this, "Erro ao excluir: ${erro.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configurarAvaliacao() {
        layoutAvaliacao = findViewById(R.id.layoutAvaliacao)
        ratingBarAvaliacao = findViewById(R.id.ratingBarAvaliacao)
        btnAvaliar = findViewById(R.id.btnAvaliar)
        tvMediaDetalhe = findViewById(R.id.tvMediaDetalhe)

        ratingRef = db.collection("ratings").document("${uid}_$eventId")

        btnAvaliar.setOnClickListener { enviarAvaliacao() }
        carregarMedia()
    }

    // Só aparece pra aluno inscrito em evento já encerrado
    private fun atualizarAvaliacao(evento: Event) {
        if (inscrito && evento.estaEncerrado()) {
            layoutAvaliacao.visibility = View.VISIBLE
            carregarMinhaNota()
        } else {
            layoutAvaliacao.visibility = View.GONE
        }
    }

    private fun carregarMinhaNota() {
        ratingRef.get()
            .addOnSuccessListener { documento ->
                if (documento.exists()) {
                    ratingBarAvaliacao.rating = (documento.getLong("nota") ?: 0L).toFloat()
                    btnAvaliar.text = "Atualizar avaliação"
                } else {
                    btnAvaliar.text = "Enviar avaliação"
                }
            }
    }

    private fun enviarAvaliacao() {
        val nota = ratingBarAvaliacao.rating.toInt()
        if (nota < 1 || nota > 5) {
            Toast.makeText(this, "Escolha uma nota de 1 a 5", Toast.LENGTH_SHORT).show()
            return
        }

        btnAvaliar.isEnabled = false

        ratingRef.set(
            hashMapOf(
                "userId" to uid,
                "eventId" to eventId,
                "nota" to nota,
                "data" to FieldValue.serverTimestamp()
            )
        )
            .addOnSuccessListener {
                btnAvaliar.isEnabled = true
                btnAvaliar.text = "Atualizar avaliação"
                Toast.makeText(this, "Avaliação salva!", Toast.LENGTH_SHORT).show()
                carregarMedia()
            }
            .addOnFailureListener { erro ->
                btnAvaliar.isEnabled = true
                Toast.makeText(this, "Erro ao avaliar: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun carregarMedia() {
        db.collection("ratings")
            .whereEqualTo("eventId", eventId)
            .get()
            .addOnSuccessListener { resultado ->
                val notas = resultado.documents.mapNotNull { it.getLong("nota") }
                if (notas.isEmpty()) {
                    tvMediaDetalhe.text = "Avaliação média: sem avaliações"
                } else {
                    val media = notas.average()
                    val mediaTexto = String.format(Locale("pt", "BR"), "%.1f", media)
                    tvMediaDetalhe.text = "Avaliação média: $mediaTexto (${notas.size} avaliações)"
                }
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar média: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }
}