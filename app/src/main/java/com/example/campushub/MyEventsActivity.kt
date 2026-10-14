package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore


class MyEventsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: EventAdapter
    private lateinit var recyclerMeusEventos: RecyclerView
    private lateinit var progressMeusEventos: ProgressBar
    private lateinit var tvNenhumMeuEvento: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_events)

        db = FirebaseFirestore.getInstance()

        recyclerMeusEventos = findViewById(R.id.recyclerMeusEventos)
        progressMeusEventos = findViewById(R.id.progressMeusEventos)
        tvNenhumMeuEvento = findViewById(R.id.tvNenhumMeuEvento)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)

        adapter = EventAdapter { evento ->
            val intent = Intent(this, EventDetailsActivity::class.java)
            intent.putExtra("eventId", evento.id)
            startActivity(intent)
        }
        recyclerMeusEventos.layoutManager = LinearLayoutManager(this)
        recyclerMeusEventos.adapter = adapter

        btnVoltar.setOnClickListener {
            finish()
        }
    }


    override fun onResume() {
        super.onResume()
        carregarMeusEventos()
    }

    private fun carregarMeusEventos() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        progressMeusEventos.visibility = View.VISIBLE
        tvNenhumMeuEvento.visibility = View.GONE


        db.collection("registrations")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { inscricoes ->
                val ids = inscricoes.documents.mapNotNull { it.getString("eventId") }

                if (ids.isEmpty()) {
                    mostrarLista(emptyList())
                    return@addOnSuccessListener
                }


                val buscas = ids.map { db.collection("events").document(it).get() }


                Tasks.whenAllSuccess<DocumentSnapshot>(buscas)
                    .addOnSuccessListener { documentos ->
                        val eventos = documentos
                            .mapNotNull { it.toObject(Event::class.java) }
                            .sortedBy { it.dataHora?.seconds ?: Long.MAX_VALUE }
                        mostrarLista(eventos)
                    }
                    .addOnFailureListener { erro ->
                        progressMeusEventos.visibility = View.GONE
                        Toast.makeText(this, "Erro ao carregar eventos: ${erro.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { erro ->
                progressMeusEventos.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar inscrições: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun mostrarLista(eventos: List<Event>) {
        adapter.atualizar(eventos)
        progressMeusEventos.visibility = View.GONE
        tvNenhumMeuEvento.visibility = if (eventos.isEmpty()) View.VISIBLE else View.GONE
    }
}