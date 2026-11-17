package com.example.campushub

object Waitlist {

    fun canJoin(encerrado: Boolean, inscrito: Boolean, naFila: Boolean, vagasLivres: Long): String? {
        if (encerrado) return "Este evento já foi encerrado"
        if (inscrito) return "Você já está inscrito neste evento"
        if (naFila) return "Você já está na lista de espera"
        if (vagasLivres > 0) return "Ainda há vagas, é só se inscrever"
        return null
    }

    fun canLeave(naFila: Boolean): String? {
        if (!naFila) return "Você não está na lista de espera"
        return null
    }

    fun canConfirm(encerrado: Boolean, inscrito: Boolean, naFila: Boolean, vagas: Long): String? {
        if (encerrado) return "Este evento já foi encerrado"
        if (inscrito) return "Você já está inscrito neste evento"
        if (!naFila) return "Você não está na lista de espera"
        if (vagas <= 0) return "Ainda não há vaga disponível"
        return null
    }
}