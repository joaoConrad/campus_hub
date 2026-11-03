package com.example.campushub

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Comment(
    @DocumentId val id: String = "",
    val eventId: String = "",
    val autorId: String = "",
    val autorNome: String = "",
    val texto: String = "",
    val data: Timestamp? = null
)