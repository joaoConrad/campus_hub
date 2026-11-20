package com.example.campushub

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityRulesIntegrationTest {

    @Before
    fun preparar() {
        TestEnv.conectar()
        TestEnv.limpar()
    }

    // devolve true se a operação foi recusada (ex.: permissão negada)
    private fun recusado(operacao: () -> Unit): Boolean =
        try {
            operacao()
            false
        } catch (e: Exception) {
            true
        }

    @Test
    fun alunoNaoConsegueMudarOProprioTipo() {
        val uid = TestEnv.criarUsuario("aluno@teste.com", "Aluno")
        val ref = TestEnv.db.collection("users").document(uid)

        assertTrue(recusado { Tasks.await(ref.update("tipo", "organizador")) })
        assertEquals("aluno", Tasks.await(ref.get()).getString("tipo"))
    }

    @Test
    fun soOrganizadorCriaEvento() {
        val alunoUid = TestEnv.criarUsuario("aluno@teste.com", "Aluno")
        val dadosAluno = mapOf("titulo" to "Evento", "organizadorId" to alunoUid, "vagas" to 10)
        assertTrue(recusado {
            Tasks.await(TestEnv.db.collection("events").document("evento_aluno").set(dadosAluno))
        })

        val orgUid = TestEnv.criarUsuario("org@teste.com", "Organizador", "organizador")
        val dadosOrg = mapOf("titulo" to "Evento", "organizadorId" to orgUid, "vagas" to 10)
        Tasks.await(TestEnv.db.collection("events").document("evento_org").set(dadosOrg))

        val criado = Tasks.await(TestEnv.db.collection("events").document("evento_org").get())
        assertTrue(criado.exists())
    }

    @Test
    fun soODonoEditaOEvento() {
        val org1 = TestEnv.criarUsuario("org1@teste.com", "Organizador Um", "organizador")
        TestEnv.criarUsuario("org2@teste.com", "Organizador Dois", "organizador")
        TestEnv.criarDocAdmin(
            "events", "evento_dono",
            mapOf("titulo" to "Original", "organizadorId" to org1, "vagas" to 10L)
        )
        val ref = TestEnv.db.collection("events").document("evento_dono")

        // logado como org2 (o último criado): não é o dono
        assertTrue(recusado { Tasks.await(ref.update("titulo", "Invadido")) })

        TestEnv.entrar("org1@teste.com")
        Tasks.await(ref.update("titulo", "Editado"))
        assertEquals("Editado", Tasks.await(ref.get()).getString("titulo"))
    }

    @Test
    fun soODonoMarcaCheckinNosInscritos() {
        val alunoUid = TestEnv.criarUsuario("aluno@teste.com", "Aluno")
        val orgUid = TestEnv.criarUsuario("org@teste.com", "Organizador", "organizador")
        val eventId = "evento_checkin"
        TestEnv.criarDocAdmin(
            "events", eventId,
            mapOf("titulo" to "Evento", "organizadorId" to orgUid, "vagas" to 9L)
        )
        TestEnv.criarDocAdmin(
            "registrations", "${alunoUid}_$eventId",
            mapOf("userId" to alunoUid, "eventId" to eventId, "nome" to "Aluno")
        )
        val regRef = TestEnv.db.collection("registrations").document("${alunoUid}_$eventId")

        // o aluno não pode marcar o próprio check-in
        TestEnv.entrar("aluno@teste.com")
        assertTrue(recusado { Tasks.await(regRef.update("checkin", true)) })

        // o organizador dono vê os inscritos e marca o check-in
        TestEnv.entrar("org@teste.com")
        val inscritos = Tasks.await(
            TestEnv.db.collection("registrations").whereEqualTo("eventId", eventId).get()
        )
        assertEquals(1, inscritos.size())

        Tasks.await(regRef.update("checkin", true))
        assertEquals(true, Tasks.await(regRef.get()).getBoolean("checkin"))
    }
}