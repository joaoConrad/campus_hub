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
import com.google.firebase.firestore.FirebaseFirestore

class EventsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: EventAdapter
    private lateinit var progressEventos: ProgressBar
    private lateinit var tvNenhumEvento: TextView
    private lateinit var recyclerEventos: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        db = FirebaseFirestore.getInstance()

        recyclerEventos = findViewById(R.id.recyclerEventos)
        progressEventos = findViewById(R.id.progressEventos)
        tvNenhumEvento = findViewById(R.id.tvNenhumEvento)
        val btnMeusEventos = findViewById<Button>(R.id.btnMeusEventos)
        val btnPerfil = findViewById<Button>(R.id.btnPerfil)

        adapter = EventAdapter { evento ->
            val intent = Intent(this, EventDetailsActivity::class.java)
            intent.putExtra("eventId", evento.id)
            startActivity(intent)
        }
        recyclerEventos.layoutManager = LinearLayoutManager(this)
        recyclerEventos.adapter = adapter

        btnMeusEventos.setOnClickListener {
            startActivity(Intent(this, MyEventsActivity::class.java))
        }
        btnPerfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        carregarEventos()
    }

    private fun carregarEventos() {
        progressEventos.visibility = View.VISIBLE
        tvNenhumEvento.visibility = View.GONE

        db.collection("events")
            .orderBy("dataHora")
            .get()
            .addOnSuccessListener { resultado ->
                val lista = resultado.toObjects(Event::class.java)
                adapter.atualizar(lista)
                progressEventos.visibility = View.GONE
                tvNenhumEvento.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { erro ->
                progressEventos.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar eventos: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }
}