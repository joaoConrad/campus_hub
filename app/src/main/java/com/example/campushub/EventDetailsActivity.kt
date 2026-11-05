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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// Tela de detalhes: recebe o id de um evento, busca ele no Firestore e mostra os dados
class EventDetailsActivity : AppCompatActivity() {

    private lateinit var registrations: RegistrationRepository
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

    private lateinit var mapaEvento: MapView
    private lateinit var tvEnderecoDetalhe: TextView

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
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "CampusHub/1.0 (trabalho academico URI Santiago)"
        setContentView(R.layout.activity_event_details)

        db = FirebaseFirestore.getInstance()
        registrations = RegistrationRepository(db)

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
        configurarMapa()
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
        val dataHora = evento.dataHora?.toDate()

        tvTitulo.text = evento.titulo
        tvCategoria.text = "Categoria: ${evento.categoria}"
        tvDescricao.text = evento.descricao
        tvData.text = "Data: " + Formatters.date(dataHora)
        tvHorario.text = "Horário: " + Formatters.time(dataHora)
        tvLocal.text = "Local: ${evento.local}"
        tvVagas.text = "Vagas restantes: ${evento.vagas}"
        mostrarLocalizacao(evento)
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
        val estado = ButtonState.registration(evento.estaEncerrado(), inscrito, evento.vagas)
        btnInscrever.text = estado.text
        btnInscrever.isEnabled = estado.enabled
    }

    private fun inscrever() {
        btnInscrever.isEnabled = false

        registrations.register(uid, eventId)
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

        registrations.cancel(uid, eventId)
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
        val erro = Validation.comment(texto)
        if (erro != null) {
            Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
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
                val erro = Validation.commentEdit(novo)
                if (erro != null) {
                    Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
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
        val erro = Validation.rating(nota)
        if (erro != null) {
            Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
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
                tvMediaDetalhe.text = Formatters.averageText(Ratings.average(notas), notas.size)
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar média: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun configurarMapa() {
        mapaEvento = findViewById(R.id.mapaEvento)
        tvEnderecoDetalhe = findViewById(R.id.tvEnderecoDetalhe)

        mapaEvento.setTileSource(TileSourceFactory.MAPNIK)
        mapaEvento.setMultiTouchControls(true)
        // Deixa mexer no mapa sem a tela rolar junto
        mapaEvento.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
    }

    private fun mostrarLocalizacao(evento: Event) {
        tvEnderecoDetalhe.text = "Endereço: " + evento.endereco.ifEmpty { evento.local }

        val lat = evento.latitude
        val lng = evento.longitude

        if (lat == null || lng == null) {
            mapaEvento.visibility = View.GONE
            return
        }

        mapaEvento.visibility = View.VISIBLE

        val ponto = GeoPoint(lat, lng)
        val marcador = Marker(mapaEvento)
        marcador.position = ponto
        marcador.title = evento.titulo
        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        mapaEvento.overlays.clear()
        mapaEvento.overlays.add(marcador)
        mapaEvento.controller.setZoom(17.0)
        mapaEvento.controller.setCenter(ponto)
        mapaEvento.invalidate()
    }

    override fun onResume() {
        super.onResume()
        if (::mapaEvento.isInitialized) mapaEvento.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::mapaEvento.isInitialized) mapaEvento.onPause()
    }
}