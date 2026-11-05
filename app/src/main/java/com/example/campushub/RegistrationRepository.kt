package com.example.campushub

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Date

class RegistrationRepository(private val db: FirebaseFirestore) {

    private fun erro(mensagem: String) =
        FirebaseFirestoreException(mensagem, FirebaseFirestoreException.Code.ABORTED)

    fun register(uid: String, eventId: String): Task<Void?> {
        val eventRef = db.collection("events").document(eventId)
        val regRef = db.collection("registrations").document("${uid}_$eventId")

        return db.runTransaction<Void?> { transaction ->
            val eventoDoc = transaction.get(eventRef)
            val inscricaoDoc = transaction.get(regRef)
            val vagas = eventoDoc.getLong("vagas") ?: 0L
            val dataHora = eventoDoc.getTimestamp("dataHora")?.toDate()
            val encerrado = dataHora != null && dataHora.before(Date())

            val problema = Registration.canRegister(encerrado, inscricaoDoc.exists(), vagas)
            if (problema != null) throw erro(problema)

            transaction.update(eventRef, "vagas", vagas - 1)
            transaction.set(
                regRef,
                hashMapOf(
                    "userId" to uid,
                    "eventId" to eventId,
                    "dataInscricao" to FieldValue.serverTimestamp()
                )
            )
            null
        }
    }

    fun cancel(uid: String, eventId: String): Task<Void?> {
        val eventRef = db.collection("events").document(eventId)
        val regRef = db.collection("registrations").document("${uid}_$eventId")

        return db.runTransaction<Void?> { transaction ->
            val eventoDoc = transaction.get(eventRef)
            val inscricaoDoc = transaction.get(regRef)
            val vagas = eventoDoc.getLong("vagas") ?: 0L

            val problema = Registration.canCancel(inscricaoDoc.exists())
            if (problema != null) throw erro(problema)

            transaction.update(eventRef, "vagas", vagas + 1)
            transaction.delete(regRef)
            null
        }
    }
}