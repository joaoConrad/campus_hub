package com.example.campushub

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmulatorSmokeTest {

    @Before
    fun preparar() {
        TestEnv.conectar()
        TestEnv.limpar()
    }

    @Test
    fun criaAlunoELeOProprioDocumento() {
        val uid = TestEnv.criarUsuario("aluno@teste.com", "Aluno Teste")
        val doc = Tasks.await(TestEnv.db.collection("users").document(uid).get())
        assertEquals("aluno", doc.getString("tipo"))
    }

    @Test
    fun criaOrganizadorPeloEmulador() {
        val uid = TestEnv.criarUsuario("org@teste.com", "Organizador", "organizador")
        val doc = Tasks.await(TestEnv.db.collection("users").document(uid).get())
        assertEquals("organizador", doc.getString("tipo"))
    }
}