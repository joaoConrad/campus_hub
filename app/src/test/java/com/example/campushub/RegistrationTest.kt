package com.example.campushub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegistrationTest {

    @Test
    fun canRegister_allOk_returnsNull() {
        assertNull(Registration.canRegister(false, false, 5))
    }

    @Test
    fun canRegister_closedEvent_returnsError() {
        assertEquals("Este evento já foi encerrado", Registration.canRegister(true, false, 5))
    }

    @Test
    fun canRegister_alreadyRegistered_returnsError() {
        assertEquals("Você já está inscrito neste evento", Registration.canRegister(false, true, 5))
    }

    @Test
    fun canRegister_noSeats_returnsError() {
        assertEquals("As vagas deste evento esgotaram", Registration.canRegister(false, false, 0))
    }

    @Test
    fun canRegister_closedEvent_hasPriority() {
        assertEquals("Este evento já foi encerrado", Registration.canRegister(true, true, 0))
    }

    @Test
    fun canCancel_registeredUser_returnsNull() {
        assertNull(Registration.canCancel(true))
    }

    @Test
    fun canCancel_notRegistered_returnsError() {
        assertEquals("Você não está inscrito neste evento", Registration.canCancel(false))
    }
}