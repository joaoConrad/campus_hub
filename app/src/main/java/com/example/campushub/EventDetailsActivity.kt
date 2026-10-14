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

    private lateinit var btnVoltar: Button

    private var eventId: String = ""
    private var uid: String = ""

    private var inscrito = false

    private lateinit var eventRef: DocumentReference
    private lateinit var regRef: DocumentReference

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

        btnVoltar = findViewById(R.id.btnVoltar)

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

        btnInscrever.isEnabled = false

        btnInscrever.setOnClickListener {
            if (inscrito) cancelarInscricao() else inscrever()
        }

        carregarEvento()
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
}