package com.example.campushub

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Post(
    @DocumentId val id: String = "",
    val eventId: String = "",
    val tipo: String = "",
    val autorId: String = "",
    val autorNome: String = "",
    val caminho: String = "",
    val imagem: String = "",
    val data: Timestamp? = null
)

object PostType {
    const val FOTO = "foto"
    const val AUDIO = "audio"
    const val VIDEO = "video"
}