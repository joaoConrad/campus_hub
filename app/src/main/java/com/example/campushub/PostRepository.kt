package com.example.campushub

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class PostRepository(private val db: FirebaseFirestore) {

    fun publish(
        eventId: String, tipo: String, autorId: String, autorNome: String, caminho: String
    ): Task<DocumentReference> =
        db.collection("posts").add(
            hashMapOf(
                "eventId" to eventId,
                "tipo" to tipo,
                "autorId" to autorId,
                "autorNome" to autorNome,
                "caminho" to caminho,
                "data" to FieldValue.serverTimestamp()
            )
        )

    // apaga a publicação e, depois, o arquivo guardado no aparelho

    fun delete(post: Post): Task<Void> =
        db.collection("posts").document(post.id).delete()
            .addOnSuccessListener {
                if (post.caminho.isNotBlank()) {
                    File(post.caminho).delete()
                }
            }


    fun publishPhoto(
        eventId: String,
        autorId: String,
        autorNome: String,
        imagem: String
    ): Task<DocumentReference> {

        return db.collection("posts").add(
            hashMapOf(
                "eventId" to eventId,
                "tipo" to PostType.FOTO,
                "autorId" to autorId,
                "autorNome" to autorNome,
                "imagem" to imagem,
                "data" to FieldValue.serverTimestamp()
            )
        )
    }
    }
