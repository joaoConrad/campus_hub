
package com.example.campushub

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class RegistrationIntegrationTest {

    private lateinit var repository: RegistrationRepository

    @Before
    fun preparar() {
        // Conecta aos emuladores e limpa os dados anteriores
        TestEnv.conectar()
        TestEnv.limpar()

        repository = RegistrationRepository(TestEnv.db)
    }

    @Test
    fun inscricaoAtualizaVagasERegistraAluno() {
        // 1. Cria um aluno autenticado
        val uid = TestEnv.criarUsuario(
            email = "aluno@teste.com",
            nome = "Aluno Teste"
        )

        // 2. Cria um evento com 10 vagas disponíveis
        val eventId = "evento_teste_01"

        TestEnv.criarDocAdmin(
            colecao = "events",
            id = eventId,
            campos = mapOf(
                "titulo" to "Evento de Integração",
                "vagas" to 10L,
                "fila" to 0L,
                "status" to "publicado",
                "dataHora" to Date(System.currentTimeMillis() + 86400000L)
            )
        )

        // 3. Realiza a inscrição do aluno
        Tasks.await(repository.register(uid, eventId))

        // 4. Consulta o evento no Firestore
        val evento = Tasks.await(
            TestEnv.db.collection("events")
                .document(eventId)
                .get()
        )

        // 5. Consulta o registro da inscrição
        val inscricao = Tasks.await(
            TestEnv.db.collection("registrations")
                .document("${uid}_$eventId")
                .get()
        )

        // 6. Verifica se as vagas foram atualizadas
        assertEquals(9L, evento.getLong("vagas"))

        // 7. Verifica se a inscrição foi criada
        assertNotNull(inscricao)
        assertEquals(true, inscricao.exists())

        // 8. Verifica os dados da inscrição
        assertEquals(uid, inscricao.getString("userId"))
        assertEquals(eventId, inscricao.getString("eventId"))
        assertEquals("Aluno Teste", inscricao.getString("nome"))
        assertNotNull(inscricao.getTimestamp("dataInscricao"))
    }


    @Test
    fun cancelamentoRemoveInscricaoEDevolveVaga() {
        // 1. Cria um aluno autenticado
        val uid = TestEnv.criarUsuario(
            email = "aluno2@teste.com",
            nome = "Aluno Teste"
        )

        // 2. Cria um evento com 10 vagas disponíveis
        val eventId = "evento_teste_02"

        TestEnv.criarDocAdmin(
            colecao = "events",
            id = eventId,
            campos = mapOf(
                "titulo" to "Evento de Integração",
                "vagas" to 10L,
                "fila" to 0L,
                "status" to "publicado",
                "dataHora" to Date(System.currentTimeMillis() + 86400000L)
            )
        )

        // 3. Realiza a inscrição do aluno
        Tasks.await(repository.register(uid, eventId))

        // 4. Verifica se a inscrição foi realizada
        val inscricaoAntes = Tasks.await(
            TestEnv.db.collection("registrations")
                .document("${uid}_$eventId")
                .get()
        )

        assertEquals(true, inscricaoAntes.exists())

        // 5. Verifica se a vaga foi descontada
        val eventoAntes = Tasks.await(
            TestEnv.db.collection("events")
                .document(eventId)
                .get()
        )

        assertEquals(9L, eventoAntes.getLong("vagas"))

        // 6. Realiza o cancelamento da inscrição
        Tasks.await(repository.cancel(uid, eventId))

        // 7. Consulta novamente o evento
        val eventoDepois = Tasks.await(
            TestEnv.db.collection("events")
                .document(eventId)
                .get()
        )

        // 8. Consulta novamente a inscrição
        val inscricaoDepois = Tasks.await(
            TestEnv.db.collection("registrations")
                .document("${uid}_$eventId")
                .get()
        )

        // 9. Verifica se a vaga foi devolvida
        assertEquals(10L, eventoDepois.getLong("vagas"))

        // 10. Verifica se a inscrição foi removida
        assertEquals(false, inscricaoDepois.exists())
    }


    @Test
    fun preventsDuplicateRegistrationWithoutChangingAvailableSpots() {

        // 1. Cria um aluno autenticado
        val uid = TestEnv.criarUsuario(
            email = "student3@test.com",
            nome = "Test Student"
        )

        // 2. Cria um evento com 10 vagas disponíveis
        val eventId = "event_test_03"

        TestEnv.criarDocAdmin(
            colecao = "events",
            id = eventId,
            campos = mapOf(
                "titulo" to "Integration Test Event",
                "vagas" to 10L,
                "fila" to 0L,
                "status" to "publicado",
                "dataHora" to Date(System.currentTimeMillis() + 86400000L)
            )
        )

        // 3. Realiza a primeira inscrição
        Tasks.await(repository.register(uid, eventId))

        // 4. Verifica se a segunda inscrição é recusada
        var duplicateRejected = false

        try {
            Tasks.await(repository.register(uid, eventId))
        } catch (e: Exception) {
            duplicateRejected = true
        }

        assertEquals(true, duplicateRejected)

        // 5. Consulta as vagas disponíveis
        val evento = Tasks.await(
            TestEnv.db.collection("events")
                .document(eventId)
                .get()
        )

        // Consulta diretamente a inscrição do aluno
        val inscricao = Tasks.await(
            TestEnv.db.collection("registrations")
                .document("${uid}_$eventId")
                .get()
        )

        // 7. Verifica se as vagas permaneceram em 9
        assertEquals(9L, evento.getLong("vagas"))

        // 8. Verifica se existe somente uma inscrição
        // Verifica se a inscrição continua existindo
        assertEquals(true, inscricao.exists())
    }

}
