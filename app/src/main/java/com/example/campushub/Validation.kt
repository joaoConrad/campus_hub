package com.example.campushub

// Cada função devolve a mensagem de erro, ou null se estiver tudo certo
object Validation {

    fun login(email: String, senha: String): String? {
        if (email.isBlank() || senha.isEmpty()) return "Preencha e-mail e senha"
        return null
    }

    fun forgotPassword(email: String): String? {
        if (email.isBlank()) return "Digite seu e-mail"
        return null
    }

    fun register(nome: String, email: String, curso: String, senha: String, confirmar: String): String? {
        if (nome.isBlank() || email.isBlank() || curso.isBlank() || senha.isEmpty()) {
            return "Preencha todos os campos"
        }
        if (senha.length < 6) return "A senha deve ter pelo menos 6 caracteres"
        if (senha != confirmar) return "As senhas não conferem"
        return null
    }

    fun profile(nome: String, curso: String): String? {
        if (nome.isBlank() || curso.isBlank()) return "Preencha nome e curso"
        return null
    }

    fun comment(texto: String): String? {
        if (texto.isBlank()) return "Escreva um comentário"
        return null
    }

    fun commentEdit(texto: String): String? {
        if (texto.isBlank()) return "O comentário não pode ficar vazio"
        return null
    }

    fun rating(nota: Int): String? {
        if (nota < 1 || nota > 5) return "Escolha uma nota de 1 a 5"
        return null
    }
}