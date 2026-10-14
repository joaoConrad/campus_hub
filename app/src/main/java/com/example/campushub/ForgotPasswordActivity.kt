package com.example.campushub

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val editEmail = findViewById<EditText>(R.id.editEmail)
        val btnEnviarLink = findViewById<Button>(R.id.btnEnviarLink)
        val btnVoltarLogin = findViewById<Button>(R.id.btnVoltarLogin)

        btnEnviarLink.setOnClickListener {
            val email = editEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Digite seu e-mail", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnEnviarLink.isEnabled = false

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        "Se o e-mail estiver cadastrado, você receberá um link de redefinição",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                .addOnFailureListener { erro ->
                    btnEnviarLink.isEnabled = true
                    Toast.makeText(this, "Erro ao enviar: ${erro.message}", Toast.LENGTH_LONG).show()
                }
        }

        btnVoltarLogin.setOnClickListener {
            finish()
        }
    }
}