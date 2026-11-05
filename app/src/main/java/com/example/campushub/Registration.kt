package com.example.campushub

object Registration {

    fun canRegister(encerrado: Boolean, inscrito: Boolean, vagas: Long): String? {
        if (encerrado) return "Este evento já foi encerrado"
        if (inscrito) return "Você já está inscrito neste evento"
        if (vagas <= 0) return "As vagas deste evento esgotaram"
        return null
    }

    fun canCancel(inscrito: Boolean): String? {
        if (!inscrito) return "Você não está inscrito neste evento"
        return null
    }
}