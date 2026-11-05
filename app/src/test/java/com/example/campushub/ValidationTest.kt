package com.example.campushub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationTest {

    @Test
    fun login_validInput_returnsNull() {
        assertNull(Validation.login("a@a.com", "123456"))
    }

    @Test
    fun login_blankEmail_returnsError() {
        assertEquals("Preencha e-mail e senha", Validation.login("  ", "123456"))
    }

    @Test
    fun login_emptyPassword_returnsError() {
        assertEquals("Preencha e-mail e senha", Validation.login("a@a.com", ""))
    }

    @Test
    fun forgotPassword_validEmail_returnsNull() {
        assertNull(Validation.forgotPassword("a@a.com"))
    }

    @Test
    fun forgotPassword_blankEmail_returnsError() {
        assertEquals("Digite seu e-mail", Validation.forgotPassword(" "))
    }

    @Test
    fun register_validInput_returnsNull() {
        assertNull(Validation.register("Ana", "a@a.com", "SI", "123456", "123456"))
    }

    @Test
    fun register_emptyField_returnsError() {
        assertEquals("Preencha todos os campos", Validation.register("", "a@a.com", "SI", "123456", "123456"))
        assertEquals("Preencha todos os campos", Validation.register("Ana", "", "SI", "123456", "123456"))
        assertEquals("Preencha todos os campos", Validation.register("Ana", "a@a.com", "", "123456", "123456"))
        assertEquals("Preencha todos os campos", Validation.register("Ana", "a@a.com", "SI", "", ""))
    }

    @Test
    fun register_shortPassword_returnsError() {
        assertEquals(
            "A senha deve ter pelo menos 6 caracteres",
            Validation.register("Ana", "a@a.com", "SI", "12345", "12345")
        )
    }

    @Test
    fun register_sixCharPassword_returnsNull() {
        assertNull(Validation.register("Ana", "a@a.com", "SI", "123456", "123456"))
    }

    @Test
    fun register_passwordMismatch_returnsError() {
        assertEquals(
            "As senhas não conferem",
            Validation.register("Ana", "a@a.com", "SI", "123456", "654321")
        )
    }

    @Test
    fun profile_validInput_returnsNull() {
        assertNull(Validation.profile("Ana", "SI"))
    }

    @Test
    fun profile_blankNameOrCourse_returnsError() {
        assertEquals("Preencha nome e curso", Validation.profile("", "SI"))
        assertEquals("Preencha nome e curso", Validation.profile("Ana", "  "))
    }

    @Test
    fun comment_validText_returnsNull() {
        assertNull(Validation.comment("legal"))
    }

    @Test
    fun comment_blankText_returnsError() {
        assertEquals("Escreva um comentário", Validation.comment("   "))
    }

    @Test
    fun commentEdit_validText_returnsNull() {
        assertNull(Validation.commentEdit("novo texto"))
    }

    @Test
    fun commentEdit_emptyText_returnsError() {
        assertEquals("O comentário não pode ficar vazio", Validation.commentEdit(""))
    }

    @Test
    fun rating_oneToFive_returnsNull() {
        for (score in 1..5) assertNull(Validation.rating(score))
    }

    @Test
    fun rating_outOfRange_returnsError() {
        assertEquals("Escolha uma nota de 1 a 5", Validation.rating(0))
        assertEquals("Escolha uma nota de 1 a 5", Validation.rating(6))
    }
}