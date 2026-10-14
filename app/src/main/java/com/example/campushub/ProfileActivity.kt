package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// Tela de perfil: mostra e edita os dados do usuário logado, e faz o logout
class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var editNome: EditText
    private lateinit var editEmail: EditText
    private lateinit var editCurso: EditText
    private lateinit var btnSalvar: Button

    private var uid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val usuario = auth.currentUser
        if (usuario == null) {
            irParaLogin()
            return
        }
        uid = usuario.uid

        editNome = findViewById(R.id.editNome)
        editEmail = findViewById(R.id.editEmail)
        editCurso = findViewById(R.id.editCurso)
        btnSalvar = findViewById(R.id.btnSalvar)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        val btnSair = findViewById<Button>(R.id.btnSair)

        editEmail.setText(usuario.email)
        editEmail.isEnabled = false

        btnSalvar.setOnClickListener {
            salvarPerfil()
        }

        btnVoltar.setOnClickListener {
            finish()
        }

        btnSair.setOnClickListener {
            auth.signOut()
            irParaLogin()
        }

        carregarPerfil()
    }

    private fun carregarPerfil() {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { documento ->
                editNome.setText(documento.getString("nome") ?: "")
                editCurso.setText(documento.getString("curso") ?: "")
            }
            .addOnFailureListener { erro ->
                Toast.makeText(this, "Erro ao carregar perfil: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun salvarPerfil() {
        val nome = editNome.text.toString().trim()
        val curso = editCurso.text.toString().trim()

        if (nome.isEmpty() || curso.isEmpty()) {
            Toast.makeText(this, "Preencha nome e curso", Toast.LENGTH_SHORT).show()
            return
        }

        btnSalvar.isEnabled = false

        db.collection("users").document(uid)
            .set(mapOf("nome" to nome, "curso" to curso), SetOptions.merge())
            .addOnSuccessListener {
                btnSalvar.isEnabled = true
                Toast.makeText(this, "Perfil atualizado!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { erro ->
                btnSalvar.isEnabled = true
                Toast.makeText(this, "Erro ao salvar: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }
    private fun irParaLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}