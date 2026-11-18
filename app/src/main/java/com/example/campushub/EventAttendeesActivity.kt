package com.example.campushub

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Tela do organizador: inscritos do evento, vagas ocupadas e check-in
class EventAttendeesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: AttendeeAdapter
    private lateinit var tvResumo: TextView
    private lateinit var tvNenhumInscrito: TextView

    private var eventId = ""
    private var capacidade = 0
    private var inscritos: List<Attendee> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_attendees)

        db = FirebaseFirestore.getInstance()
        tvResumo = findViewById(R.id.tvResumo)
        tvNenhumInscrito = findViewById(R.id.tvNenhumInscrito)
        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }

        eventId = intent.getStringExtra("eventId") ?: ""
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (eventId.isEmpty() || uid.isEmpty()) {
            finish()
            return
        }

        adapter = AttendeeAdapter { aluno, marcado -> marcarCheckin(aluno, marcado) }
        val recycler = findViewById<RecyclerView>(R.id.recyclerInscritos)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        db.collection("events").document(eventId).get()
            .addOnSuccessListener { doc ->
                val evento = doc.toObject(Event::class.java)
                if (evento == null || evento.organizadorId != uid) {
                    Toast.makeText(this, "Você não pode ver os inscritos deste evento", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                capacidade = evento.capacidade
                carregarInscritos()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar evento: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun carregarInscritos() {
        db.collection("registrations").whereEqualTo("eventId", eventId).get()
            .addOnSuccessListener { resultado ->
                inscritos = resultado.documents
                    .map {
                        Attendee(it.id, it.getString("nome") ?: "Aluno", it.getBoolean("checkin") ?: false)
                    }
                    .sortedBy { it.nome.lowercase() }
                adapter.atualizar(inscritos)
                atualizarResumo()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar inscritos: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun atualizarResumo() {
        tvResumo.text = Formatters.occupancyText(
            inscritos.size, capacidade, inscritos.count { it.checkin }
        )
        tvNenhumInscrito.visibility = if (inscritos.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun marcarCheckin(aluno: Attendee, marcado: Boolean) {
        db.collection("registrations").document(aluno.id).update("checkin", marcado)
            .addOnSuccessListener {
                inscritos = inscritos.map {
                    if (it.id == aluno.id) it.copy(checkin = marcado) else it
                }
                atualizarResumo()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao marcar check-in: ${erro.message}", Toast.LENGTH_LONG).show()
                carregarInscritos()
            }
    }
}