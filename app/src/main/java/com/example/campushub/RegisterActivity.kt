package com.example.campushub

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val editNome = findViewById<EditText>(R.id.editNome)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editCurso = findViewById<EditText>(R.id.editCurso)
        val editSenha = findViewById<EditText>(R.id.editSenha)
        val editConfirmarSenha = findViewById<EditText>(R.id.editConfirmarSenha)
        val btnCadastrar = findViewById<Button>(R.id.btnCadastrar)
        val btnVoltarLogin = findViewById<Button>(R.id.btnVoltarLogin)

        btnCadastrar.setOnClickListener {
            val nome = editNome.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val curso = editCurso.text.toString().trim()
            val senha = editSenha.text.toString()
            val confirmar = editConfirmarSenha.text.toString()

            if (nome.isEmpty() || email.isEmpty() || curso.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (senha.length < 6) {
                Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (senha != confirmar) {
                Toast.makeText(this, "As senhas não conferem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnCadastrar.isEnabled = false

            auth.createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener { resultado ->
                    val uid = resultado.user!!.uid
                    val usuario = hashMapOf(
                        "nome" to nome,
                        "email" to email,
                        "curso" to curso
                    )

                    db.collection("users").document(uid).set(usuario)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, EventsActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        .addOnFailureListener { erro ->
                            btnCadastrar.isEnabled = true
                            Toast.makeText(this, "Erro ao salvar perfil: ${erro.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { erro ->
                    btnCadastrar.isEnabled = true
                    Toast.makeText(this, "Erro ao cadastrar: ${erro.message}", Toast.LENGTH_LONG).show()
                }
        }

        btnVoltarLogin.setOnClickListener {
            finish()
        }
    }
}

