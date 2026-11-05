package com.example.campushub

import java.util.Date

object EventFilter {
    const val TODAS = "Todas"
    const val PROXIMOS = "Próximos"
    const val ENCERRADOS = "Encerrados"

    fun apply(
        eventos: List<Event>,
        busca: String,
        categoria: String,
        situacao: String,
        agora: Date = Date()
    ): List<Event> {
        val termo = busca.trim()
        return eventos.filter { e ->
            e.titulo.contains(termo, ignoreCase = true) &&
                    (categoria == TODAS || e.categoria.equals(categoria, ignoreCase = true)) &&
                    when (situacao) {
                        PROXIMOS -> !e.estaEncerrado(agora)
                        ENCERRADOS -> e.estaEncerrado(agora)
                        else -> true
                    }
        }
    }
}