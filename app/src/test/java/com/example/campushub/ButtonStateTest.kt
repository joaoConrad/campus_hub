package com.example.campushub

import org.junit.Assert.assertEquals
import org.junit.Test

class ButtonStateTest {

    @Test
    fun registration_closedEvent_isDisabled() {
        assertEquals(ButtonState("Evento encerrado", false), ButtonState.registration(true, false, 10))
    }

    @Test
    fun registration_closedEvent_winsOverRegistered() {
        assertEquals(ButtonState("Evento encerrado", false), ButtonState.registration(true, true, 10))
    }

    @Test
    fun registration_registeredUser_canCancel() {
        assertEquals(ButtonState("Cancelar inscrição", true), ButtonState.registration(false, true, 0))
    }

    @Test
    fun registration_seatsAvailable_canRegister() {
        assertEquals(ButtonState("Inscrever-se", true), ButtonState.registration(false, false, 3))
    }

    @Test
    fun registration_noSeats_isSoldOut() {
        assertEquals(ButtonState("Esgotado", false), ButtonState.registration(false, false, 0))
    }
}