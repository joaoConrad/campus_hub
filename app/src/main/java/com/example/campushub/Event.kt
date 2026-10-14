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
    val categoria: String = ""
) {
    fun estaEncerrado(): Boolean {
        val data = dataHora?.toDate() ?: return false
        return data.before(Date())
    }
}