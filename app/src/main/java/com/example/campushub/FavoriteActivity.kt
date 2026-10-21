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

class FavoritesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: EventAdapter
    private lateinit var recyclerFavoritos: RecyclerView
    private lateinit var progressFavoritos: ProgressBar
    private lateinit var tvNenhumFavorito: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        db = FirebaseFirestore.getInstance()

        recyclerFavoritos = findViewById(R.id.recyclerFavoritos)
        progressFavoritos = findViewById(R.id.progressFavoritos)
        tvNenhumFavorito = findViewById(R.id.tvNenhumFavorito)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)

        adapter = EventAdapter { evento ->
            val intent = Intent(this, EventDetailsActivity::class.java)
            intent.putExtra("eventId", evento.id)
            startActivity(intent)
        }
        recyclerFavoritos.layoutManager = LinearLayoutManager(this)
        recyclerFavoritos.adapter = adapter

        btnVoltar.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        carregarFavoritos()
    }

    private fun carregarFavoritos() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        progressFavoritos.visibility = View.VISIBLE
        tvNenhumFavorito.visibility = View.GONE

        db.collection("favorites")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { favoritos ->
                val ids = favoritos.documents.mapNotNull { it.getString("eventId") }

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
                        progressFavoritos.visibility = View.GONE
                        Toast.makeText(this, "Erro ao carregar eventos: ${erro.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { erro ->
                progressFavoritos.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar favoritos: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun mostrarLista(eventos: List<Event>) {
        adapter.atualizar(eventos)
        progressFavoritos.visibility = View.GONE
        tvNenhumFavorito.visibility = if (eventos.isEmpty()) View.VISIBLE else View.GONE
    }
}