package com.example.campushub

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Calendar
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts

// Tela do organizador: cria um evento novo ou edita um existente (se vier eventId)
class EventFormActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var editTitulo: EditText
    private lateinit var editDescricao: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var btnData: Button
    private lateinit var btnHora: Button
    private lateinit var editEndereco: EditText
    private lateinit var editCapacidade: EditText
    private lateinit var btnSalvar: Button

    private val cal = Calendar.getInstance()
    private var temData = false
    private var uid = ""
    private var eventId = ""
    private var eventoAtual: Event? = null

    private lateinit var btnImagem: Button
    private lateinit var imgPreview: ImageView
    private var imagemBase64 = ""

    private val escolherImagem = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val texto = ImageUtils.toBase64(this, uri)
            if (texto == null) {
                Toast.makeText(this, "Não foi possível usar essa imagem", Toast.LENGTH_SHORT).show()
            } else {
                imagemBase64 = texto
                mostrarPreview()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_form)

        db = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (uid.isEmpty()) {
            finish()
            return
        }

        editTitulo = findViewById(R.id.editTitulo)
        editDescricao = findViewById(R.id.editDescricao)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        btnData = findViewById(R.id.btnData)
        btnHora = findViewById(R.id.btnHora)
        editEndereco = findViewById(R.id.editEndereco)
        editCapacidade = findViewById(R.id.editCapacidade)
        btnImagem = findViewById(R.id.btnImagem)
        imgPreview = findViewById(R.id.imgPreview)
        btnImagem.setOnClickListener { escolherImagem.launch("image/*") }
        btnSalvar = findViewById(R.id.btnSalvar)

        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        btnData.setOnClickListener {
            DatePickerDialog(this, { _, ano, mes, dia ->
                cal.set(ano, mes, dia)
                temData = true
                atualizarBotoes()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnHora.setOnClickListener {
            TimePickerDialog(this, { _, hora, minuto ->
                cal.set(Calendar.HOUR_OF_DAY, hora)
                cal.set(Calendar.MINUTE, minuto)
                atualizarBotoes()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        btnSalvar.setOnClickListener { salvar() }
        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }

        eventId = intent.getStringExtra("eventId") ?: ""
        if (eventId.isNotEmpty()) {
            findViewById<TextView>(R.id.tvTituloForm).text = "Editar evento"
            carregarEvento()
        }
        atualizarBotoes()
    }

    private fun atualizarBotoes() {
        btnData.text = if (temData) Formatters.date(cal.time) else "Escolher data"
        btnHora.text = Formatters.time(cal.time)
    }

    private fun carregarEvento() {
        db.collection("events").document(eventId).get()
            .addOnSuccessListener { doc ->
                val e = doc.toObject(Event::class.java)
                if (e == null || e.organizadorId != uid) {
                    Toast.makeText(this, "Você não pode editar este evento", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                eventoAtual = e
                editTitulo.setText(e.titulo)
                editDescricao.setText(e.descricao)
                editEndereco.setText(e.endereco.ifEmpty { e.local })
                editCapacidade.setText(e.capacidade.toString())
                imagemBase64 = e.imagem
                mostrarPreview()

                val categorias = resources.getStringArray(R.array.categorias_evento)
                spinnerCategoria.setSelection(categorias.indexOf(e.categoria).coerceAtLeast(0))

                e.dataHora?.let {
                    cal.time = it.toDate()
                    temData = true
                }
                atualizarBotoes()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar evento: ${erro.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun salvar() {
        val titulo = editTitulo.text.toString().trim()
        val descricao = editDescricao.text.toString().trim()
        val endereco = editEndereco.text.toString().trim()
        val categoria = spinnerCategoria.selectedItem.toString()
        val capacidade = editCapacidade.text.toString().trim().toIntOrNull()
        val ocupadas = eventoAtual?.let { it.capacidade - it.vagas } ?: 0

        val erro = Validation.event(titulo, descricao, categoria, endereco, capacidade, ocupadas, temData)
        if (erro != null) {
            Toast.makeText(this, erro, Toast.LENGTH_SHORT).show()
            return
        }
        val cap = capacidade ?: 0

        val dados = hashMapOf<String, Any>(
            "titulo" to titulo,
            "descricao" to descricao,
            "categoria" to categoria,
            "dataHora" to Timestamp(cal.time),
            "local" to endereco,
            "endereco" to endereco,
            "capacidade" to cap,
            "imagem" to imagemBase64
        )

        btnSalvar.isEnabled = false

        if (eventId.isEmpty()) {
            dados["vagas"] = cap
            dados["organizadorId"] = uid
            dados["status"] = EventStatus.PUBLICADO
            db.collection("events").add(dados)
                .addOnSuccessListener { terminar("Evento criado!") }
                .addOnFailureListener { falhar(it.message) }
        } else {
            val ref = db.collection("events").document(eventId)
            db.runTransaction<Void?> { t ->
                val doc = t.get(ref)
                val ocupadasAgora = (doc.getLong("capacidade") ?: 0L) - (doc.getLong("vagas") ?: 0L)
                val novasVagas = cap - ocupadasAgora
                if (novasVagas < 0) {
                    throw FirebaseFirestoreException(
                        "A capacidade não pode ser menor que as vagas já ocupadas",
                        FirebaseFirestoreException.Code.ABORTED
                    )
                }
                dados["vagas"] = novasVagas
                t.update(ref, dados)
                null
            }
                .addOnSuccessListener { terminar("Evento atualizado!") }
                .addOnFailureListener { falhar(it.message) }
        }
    }

    private fun mostrarPreview() {
        val bitmap = ImageUtils.toBitmap(imagemBase64)
        if (bitmap == null) {
            imgPreview.visibility = View.GONE
        } else {
            imgPreview.setImageBitmap(bitmap)
            imgPreview.visibility = View.VISIBLE
        }
    }

    private fun terminar(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun falhar(msg: String?) {
        btnSalvar.isEnabled = true
        Toast.makeText(this, "Erro ao salvar: $msg", Toast.LENGTH_LONG).show()
    }
}