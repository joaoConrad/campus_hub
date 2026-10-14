package com.example.campushub

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Event(
    @DocumentId val id: String = "",
    val titulo: String = "",
    val descricao: String = "",
    val dataHora: Timestamp? = null,
    val local: String = "",
    val vagas: Int = 0
)