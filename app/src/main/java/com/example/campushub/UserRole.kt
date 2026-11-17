package com.example.campushub

object UserRole {
    const val ALUNO = "aluno"
    const val ORGANIZADOR = "organizador"

    // qualquer valor que não seja "organizador" (inclusive vazio) vira aluno
    fun normalize(tipo: String?): String =
        if (tipo == ORGANIZADOR) ORGANIZADOR else ALUNO

    fun isOrganizador(tipo: String?): Boolean = normalize(tipo) == ORGANIZADOR
}