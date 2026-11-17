package com.example.campushub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRoleTest {
    @Test fun organizadorEhReconhecido() = assertTrue(UserRole.isOrganizador("organizador"))
    @Test fun nuloViraAluno() = assertEquals("aluno", UserRole.normalize(null))
    @Test fun valorEstranhoViraAluno() = assertFalse(UserRole.isOrganizador("admin"))
}