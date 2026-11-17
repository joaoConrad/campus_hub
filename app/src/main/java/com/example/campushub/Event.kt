package com.example.campushub

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Event(
    @DocumentId val id: String = "",
    val titulo: String = "",
    val descricao: String = "",
    val dataHora: Timestamp? = null,
    val local: String = "",
    val vagas: Int = 0,
    val categoria: String = "",
    val endereco: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val organizadorId: String = "",
    val capacidade: Int = 0,
    val status: String = EventStatus.PUBLICADO,
    val imagem: String = "",
    val fila: Int = 0
) {
    fun estaEncerrado(agora: Date = Date()): Boolean {
        if (status == EventStatus.ENCERRADO) return true
        val data = dataHora?.toDate() ?: return false
        return data.before(agora)
    }
}
object EventStatus {
    const val PUBLICADO = "publicado"
    const val ENCERRADO = "encerrado"
}