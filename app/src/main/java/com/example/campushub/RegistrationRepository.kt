package com.example.campushub

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Date

class RegistrationRepository(private val db: FirebaseFirestore) {

    private fun erro(mensagem: String) =
        FirebaseFirestoreException(mensagem, FirebaseFirestoreException.Code.ABORTED)

    private fun estaEncerrado(doc: DocumentSnapshot): Boolean {
        val status = doc.getString("status") ?: EventStatus.PUBLICADO
        val dataHora = doc.getTimestamp("dataHora")?.toDate()
        return status == EventStatus.ENCERRADO || (dataHora != null && dataHora.before(Date()))
    }

    fun register(uid: String, eventId: String): Task<Void?> {
        val eventRef = db.collection("events").document(eventId)
        val regRef = db.collection("registrations").document("${uid}_$eventId")
        val userRef = db.collection("users").document(uid)

        return db.runTransaction<Void?> { transaction ->
            val eventoDoc = transaction.get(eventRef)
            val inscricaoDoc = transaction.get(regRef)
            val userDoc = transaction.get(userRef)
            val vagas = eventoDoc.getLong("vagas") ?: 0L
            val fila = eventoDoc.getLong("fila") ?: 0L

            // as vagas que sobram depois de reservar pra lista de espera
            val problema = Registration.canRegister(estaEncerrado(eventoDoc), inscricaoDoc.exists(), vagas - fila)
            if (problema != null) throw erro(problema)

            transaction.update(eventRef, "vagas", vagas - 1)
            transaction.set(
                regRef,
                hashMapOf(
                    "userId" to uid,
                    "eventId" to eventId,
                    "nome" to (userDoc.getString("nome") ?: "Aluno"),
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

    fun joinWaitlist(uid: String, eventId: String): Task<Void?> {
        val eventRef = db.collection("events").document(eventId)
        val regRef = db.collection("registrations").document("${uid}_$eventId")
        val waitRef = db.collection("waitlist").document("${uid}_$eventId")
        val userRef = db.collection("users").document(uid)

        return db.runTransaction<Void?> { transaction ->
            val eventoDoc = transaction.get(eventRef)
            val inscricaoDoc = transaction.get(regRef)
            val filaDoc = transaction.get(waitRef)
            val userDoc = transaction.get(userRef)
            val vagas = eventoDoc.getLong("vagas") ?: 0L
            val fila = eventoDoc.getLong("fila") ?: 0L

            val problema = Waitlist.canJoin(
                estaEncerrado(eventoDoc), inscricaoDoc.exists(), filaDoc.exists(), vagas - fila
            )
            if (problema != null) throw erro(problema)

            transaction.update(eventRef, "fila", fila + 1)
            transaction.set(
                waitRef,
                hashMapOf(
                    "userId" to uid,
                    "eventId" to eventId,
                    "nome" to (userDoc.getString("nome") ?: "Aluno"),
                    "data" to FieldValue.serverTimestamp()
                )
            )
            null
        }
    }

    fun leaveWaitlist(uid: String, eventId: String): Task<Void?> {
        val eventRef = db.collection("events").document(eventId)
        val waitRef = db.collection("waitlist").document("${uid}_$eventId")

        return db.runTransaction<Void?> { transaction ->
            val eventoDoc = transaction.get(eventRef)
            val filaDoc = transaction.get(waitRef)
            val fila = eventoDoc.getLong("fila") ?: 0L

            val problema = Waitlist.canLeave(filaDoc.exists())
            if (problema != null) throw erro(problema)

            transaction.update(eventRef, "fila", maxOf(fila - 1, 0L))
            transaction.delete(waitRef)
            null
        }
    }

    fun confirmFromWaitlist(uid: String, eventId: String): Task<Void?> {
        val eventRef = db.collection("events").document(eventId)
        val regRef = db.collection("registrations").document("${uid}_$eventId")
        val waitRef = db.collection("waitlist").document("${uid}_$eventId")
        val userRef = db.collection("users").document(uid)

        return db.runTransaction<Void?> { transaction ->
            val eventoDoc = transaction.get(eventRef)
            val inscricaoDoc = transaction.get(regRef)
            val filaDoc = transaction.get(waitRef)
            val userDoc = transaction.get(userRef)
            val vagas = eventoDoc.getLong("vagas") ?: 0L
            val fila = eventoDoc.getLong("fila") ?: 0L

            val problema = Waitlist.canConfirm(
                estaEncerrado(eventoDoc), inscricaoDoc.exists(), filaDoc.exists(), vagas
            )
            if (problema != null) throw erro(problema)

            transaction.update(eventRef, mapOf("vagas" to vagas - 1, "fila" to maxOf(fila - 1, 0L)))
            transaction.delete(waitRef)
            transaction.set(
                regRef,
                hashMapOf(
                    "userId" to uid,
                    "eventId" to eventId,
                    "nome" to (userDoc.getString("nome") ?: "Aluno"),
                    "dataInscricao" to FieldValue.serverTimestamp()
                )
            )
            null
        }
    }
}