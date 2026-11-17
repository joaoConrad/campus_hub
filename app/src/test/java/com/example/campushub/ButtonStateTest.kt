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
    fun registration_noSeats_canJoinWaitlist() {
        assertEquals(ButtonState("Entrar na lista de espera", true), ButtonState.registration(false, false, 0))
    }

    @Test
    fun registration_firstInQueueWithSeat_canConfirm() {
        assertEquals(ButtonState("Confirmar inscrição", true), ButtonState.registration(false, false, 1, 1, true, true))
    }

    @Test
    fun registration_waitingNotFirst_canLeave() {
        assertEquals(ButtonState("Sair da lista de espera", true), ButtonState.registration(false, false, 1, 2, true, false))
    }
}