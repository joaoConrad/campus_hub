
package com.example.campushub

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.util.Date

@RunWith(AndroidJUnit4::class)
class WaitlistIntegrationTest {

    private lateinit var repository: RegistrationRepository

    @Before
    fun preparar() {
        TestEnv.conectar()
        TestEnv.limpar()
        repository = RegistrationRepository(TestEnv.db)
    }

    @Test
    fun alunoDaListaDeEsperaConfirmaVagaLiberada() {
        val eventId = "evento_lista_espera"

        // Cria evento com apenas uma vaga
        TestEnv.criarDocAdmin(
            "events",
            eventId,
            mapOf(
                "titulo" to "Evento de teste",
                "vagas" to 1L,
                "fila" to 0L
            )
        )

        // Cria o primeiro aluno e ocupa a última vaga
        val uid1 = TestEnv.criarUsuario(
            "aluno1@email.com",
            "Aluno Um"
        )

        Tasks.await(repository.register(uid1, eventId))

        // Cria o segundo aluno e entra na lista de espera
        val uid2 = TestEnv.criarUsuario(
            "aluno2@email.com",
            "Aluno Dois"
        )

        Tasks.await(repository.joinWaitlist(uid2, eventId))

        val waitRef = TestEnv.db.collection("waitlist")
            .document("${uid2}_$eventId")

        val eventRef = TestEnv.db.collection("events")
            .document(eventId)

        // Verifica se o aluno entrou na fila
        val waitDoc = Tasks.await(waitRef.get())
        assertTrue(waitDoc.exists())
        assertEquals(uid2, waitDoc.getString("userId"))

        var eventDoc = Tasks.await(eventRef.get())
        assertEquals(0L, eventDoc.getLong("vagas"))
        assertEquals(1L, eventDoc.getLong("fila"))

        // O primeiro aluno cancela a inscrição
        TestEnv.entrar("aluno1@email.com")
        Tasks.await(repository.cancel(uid1, eventId))

        eventDoc = Tasks.await(eventRef.get())
        assertEquals(1L, eventDoc.getLong("vagas"))
        assertEquals(1L, eventDoc.getLong("fila"))

        // O segundo aluno confirma a vaga liberada
        TestEnv.entrar("aluno2@email.com")
        Tasks.await(repository.confirmFromWaitlist(uid2, eventId))

        // Verifica se saiu da lista de espera
        val waitAfter = Tasks.await(waitRef.get())
        assertFalse(waitAfter.exists())

        // Verifica se virou uma inscrição normal
        val registrationRef = TestEnv.db.collection("registrations")
            .document("${uid2}_$eventId")

        val registration = Tasks.await(registrationRef.get())
        assertTrue(registration.exists())
        assertEquals(uid2, registration.getString("userId"))
        assertEquals(eventId, registration.getString("eventId"))

        // Verifica os valores finais do evento
        eventDoc = Tasks.await(eventRef.get())
        assertEquals(0L, eventDoc.getLong("vagas"))
        assertEquals(0L, eventDoc.getLong("fila"))
    }

    @Test
    fun vagaReservadaParaFilaBloqueiaQuemEstaDeFora() {
        val eventId = "evento_reservado"
        TestEnv.criarDocAdmin(
            "events", eventId,
            mapOf(
                "titulo" to "Evento reservado",
                "vagas" to 1L,
                "fila" to 1L,
                "status" to "publicado",
                "dataHora" to Date(System.currentTimeMillis() + 86400000L)
            )
        )
        val uid = TestEnv.criarUsuario("fora@email.com", "Aluno de Fora")

        var recusado = false
        try {
            Tasks.await(repository.register(uid, eventId))
        } catch (e: Exception) {
            recusado = true
        }
        assertTrue(recusado)

        val evento = Tasks.await(TestEnv.db.collection("events").document(eventId).get())
        assertEquals(1L, evento.getLong("vagas"))
        val inscricao = Tasks.await(
            TestEnv.db.collection("registrations").document("${uid}_$eventId").get()
        )
        assertFalse(inscricao.exists())
    }

    @Test
    fun sairDaListaDeEsperaDevolveAFila() {
        val eventId = "evento_sair_fila"
        TestEnv.criarDocAdmin(
            "events", eventId,
            mapOf(
                "titulo" to "Evento lotado",
                "vagas" to 0L,
                "fila" to 0L,
                "status" to "publicado",
                "dataHora" to Date(System.currentTimeMillis() + 86400000L)
            )
        )
        val uid = TestEnv.criarUsuario("fila@email.com", "Aluno da Fila")
        val eventRef = TestEnv.db.collection("events").document(eventId)
        val waitRef = TestEnv.db.collection("waitlist").document("${uid}_$eventId")

        Tasks.await(repository.joinWaitlist(uid, eventId))
        assertEquals(1L, Tasks.await(eventRef.get()).getLong("fila"))
        assertTrue(Tasks.await(waitRef.get()).exists())

        Tasks.await(repository.leaveWaitlist(uid, eventId))
        assertEquals(0L, Tasks.await(eventRef.get()).getLong("fila"))
        assertFalse(Tasks.await(waitRef.get()).exists())
    }
}
