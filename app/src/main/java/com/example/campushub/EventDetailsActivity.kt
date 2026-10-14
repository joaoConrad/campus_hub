package com.example.campushub

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
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
    private lateinit var btnInscrever: Button

    private lateinit var btnVoltar: Button

    private var eventId: String = ""

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
        btnInscrever = findViewById(R.id.btnInscrever)

        btnVoltar = findViewById(R.id.btnVoltar)

        btnVoltar.setOnClickListener {
            finish()
        }

        eventId = intent.getStringExtra("eventId") ?: ""

        if (eventId.isEmpty()) {
            Toast.makeText(this, "Evento não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
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
        tvDescricao.text = evento.descricao
        tvData.text = "Data: " + (dataHora?.let { formatoData.format(it) } ?: "a definir")
        tvHorario.text = "Horário: " + (dataHora?.let { formatoHora.format(it) } ?: "a definir")
        tvLocal.text = "Local: ${evento.local}"
        tvVagas.text = "Vagas restantes: ${evento.vagas}"
    }
}