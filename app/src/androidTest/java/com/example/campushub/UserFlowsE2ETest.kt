package com.example.campushub

import android.widget.DatePicker
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Date

@RunWith(AndroidJUnit4::class)
class UserFlowsE2ETest {

    @Before
    fun preparar() {
        TestEnv.conectar()
        TestEnv.limpar()
    }

    // O Espresso não sabe esperar o Firebase, então repete a verificação até passar ou estourar o tempo
    private fun esperar(timeoutMs: Long = 10_000, verificacao: () -> Unit) {
        val limite = System.currentTimeMillis() + timeoutMs
        while (true) {
            try {
                verificacao()
                return
            } catch (e: Throwable) {
                if (System.currentTimeMillis() > limite) throw e
                Thread.sleep(250)
            }
        }
    }

    private fun fazerLogin(email: String) {
        onView(withId(R.id.editEmail)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.editSenha)).perform(typeText(TestEnv.SENHA), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())
    }

    @Test
    fun alunoSeInscreveEVeOEventoEmMeusEventos() {
        val orgUid = TestEnv.criarUsuario("org@e2e.com", "Organizador E2E", "organizador")
        val eventId = "evento_e2e"
        TestEnv.criarDocAdmin(
            "events", eventId,
            mapOf(
                "titulo" to "Palestra E2E",
                "descricao" to "Evento usado no teste ponta a ponta",
                "categoria" to "Palestra",
                "local" to "Auditório",
                "endereco" to "Rua Teste, 123",
                "vagas" to 5L,
                "capacidade" to 5L,
                "fila" to 0L,
                "status" to "publicado",
                "organizadorId" to orgUid,
                "dataHora" to Date(System.currentTimeMillis() + 7 * 86400000L)
            )
        )
        val alunoUid = TestEnv.criarUsuario("aluno@e2e.com", "Aluno E2E")
        TestEnv.auth.signOut() // pro app abrir na tela de login

        ActivityScenario.launch(LoginActivity::class.java).use {
            fazerLogin("aluno@e2e.com")

            // lista de eventos -> detalhes
            esperar { onView(withText("Palestra E2E")).check(matches(isDisplayed())) }
            onView(withText("Palestra E2E")).perform(click())

            // inscrição
            esperar {
                onView(withId(R.id.btnInscrever)).perform(scrollTo())
                    .check(matches(allOf(isDisplayed(), isEnabled(), withText("Inscrever-se"))))
            }
            onView(withId(R.id.btnInscrever)).perform(scrollTo(), click())

            esperar {
                onView(withId(R.id.btnInscrever)).check(matches(withText("Cancelar inscrição")))
            }
            esperar {
                onView(withId(R.id.tvVagasDetalhe)).perform(scrollTo())
                    .check(matches(withText(containsString("Vagas restantes: 4"))))
            }

            // a inscrição existe de verdade no banco
            val inscricao = Tasks.await(
                TestEnv.db.collection("registrations").document("${alunoUid}_$eventId").get()
            )
            assertTrue(inscricao.exists())

            // volta e confere em "Meus eventos"
            onView(withId(R.id.btnVoltar)).perform(scrollTo(), click())
            esperar { onView(withId(R.id.btnMeusEventos)).check(matches(isDisplayed())) }
            onView(withId(R.id.btnMeusEventos)).perform(click())
            esperar {
                onView(withId(R.id.recyclerMeusEventos))
                    .check(matches(hasDescendant(withText("Palestra E2E"))))
            }
        }
    }

    @Test
    fun organizadorCriaEEncerraEvento() {
        val orgUid = TestEnv.criarUsuario("org2@e2e.com", "Organizador E2E", "organizador")
        TestEnv.auth.signOut()
        val proximoAno = Calendar.getInstance().get(Calendar.YEAR) + 1

        ActivityScenario.launch(LoginActivity::class.java).use {
            fazerLogin("org2@e2e.com")

            // só o organizador vê o botão de criar evento
            esperar { onView(withId(R.id.btnCriarEvento)).check(matches(isDisplayed())) }
            onView(withId(R.id.btnCriarEvento)).perform(click())

            // formulário
            onView(withId(R.id.editTitulo)).perform(typeText("Evento criado E2E"), closeSoftKeyboard())
            onView(withId(R.id.editDescricao)).perform(typeText("Descrição do teste"), closeSoftKeyboard())
            onView(withId(R.id.btnData)).perform(scrollTo(), click())
            onView(withClassName(equalTo(DatePicker::class.java.name)))
                .perform(PickerActions.setDate(proximoAno, 6, 15))
            onView(withId(android.R.id.button1)).perform(click())
            onView(withId(R.id.editEndereco))
                .perform(scrollTo(), typeText("Rua do Teste, 100"), closeSoftKeyboard())
            onView(withId(R.id.editCapacidade))
                .perform(scrollTo(), typeText("30"), closeSoftKeyboard())
            onView(withId(R.id.btnSalvar)).perform(scrollTo(), click())

            esperar(25_000) { onView(withText("Evento criado E2E")).check(matches(isDisplayed())) }

            // e foi salvo certo no banco
            val resultado = Tasks.await(
                TestEnv.db.collection("events").whereEqualTo("titulo", "Evento criado E2E").get()
            )
            assertEquals(1, resultado.size())
            val criado = resultado.documents[0]
            assertEquals(30L, criado.getLong("capacidade"))
            assertEquals(30L, criado.getLong("vagas"))
            assertEquals("publicado", criado.getString("status"))
            assertEquals(orgUid, criado.getString("organizadorId"))

            // abre o evento e encerra
            onView(withText("Evento criado E2E")).perform(click())
            esperar {
                onView(withId(R.id.btnEncerrar)).perform(scrollTo()).check(matches(isDisplayed()))
            }
            onView(withId(R.id.btnEncerrar)).perform(scrollTo(), click())
            onView(withId(android.R.id.button1)).perform(click()) // "Encerrar" no diálogo

            esperar { onView(withId(R.id.btnEncerrar)).check(matches(not(isDisplayed()))) }
            val depois = Tasks.await(TestEnv.db.collection("events").document(criado.id).get())
            assertEquals("encerrado", depois.getString("status"))
        }
    }
}