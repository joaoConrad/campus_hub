package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
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
    private var todosEventos: List<Event> = emptyList()
    private lateinit var editBusca: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var spinnerSituacao: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        db = FirebaseFirestore.getInstance()

        recyclerEventos = findViewById(R.id.recyclerEventos)
        progressEventos = findViewById(R.id.progressEventos)
        tvNenhumEvento = findViewById(R.id.tvNenhumEvento)
        val btnMeusEventos = findViewById<Button>(R.id.btnMeusEventos)
        val btnFavoritos = findViewById<Button>(R.id.btnFavoritos)
        val btnPerfil = findViewById<Button>(R.id.btnPerfil)


        adapter = EventAdapter { evento ->
            val intent = Intent(this, EventDetailsActivity::class.java)
            intent.putExtra("eventId", evento.id)
            startActivity(intent)
        }
        recyclerEventos.layoutManager = LinearLayoutManager(this)
        recyclerEventos.adapter = adapter

        editBusca = findViewById(R.id.editBusca)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        spinnerSituacao = findViewById(R.id.spinnerSituacao)

        editBusca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { aplicarFiltros() }
        })

        val ouvinte = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { aplicarFiltros() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinnerCategoria.onItemSelectedListener = ouvinte
        spinnerSituacao.onItemSelectedListener = ouvinte

        btnMeusEventos.setOnClickListener {
            startActivity(Intent(this, MyEventsActivity::class.java))
        }
        btnPerfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        btnFavoritos.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
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
                todosEventos = resultado.toObjects(Event::class.java)
                progressEventos.visibility = View.GONE
                aplicarFiltros()
            }
            .addOnFailureListener { erro ->
                progressEventos.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar eventos: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun aplicarFiltros() {
        val filtrados = EventFilter.apply(
            todosEventos,
            editBusca.text.toString(),
            spinnerCategoria.selectedItem.toString(),
            spinnerSituacao.selectedItem.toString()
        )

        adapter.atualizar(filtrados)
        tvNenhumEvento.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
    }
}