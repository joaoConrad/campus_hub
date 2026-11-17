package com.example.campushub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WaitlistTest {
    @Test fun canJoin_eventoLotado_returnsNull() = assertNull(Waitlist.canJoin(false, false, false, 0))

    @Test fun canJoin_aindaTemVaga_returnsError() =
        assertEquals("Ainda há vagas, é só se inscrever", Waitlist.canJoin(false, false, false, 2))

    @Test fun canJoin_jaNaFila_returnsError() =
        assertEquals("Você já está na lista de espera", Waitlist.canJoin(false, false, true, 0))

    @Test fun canJoin_encerrado_returnsError() =
        assertEquals("Este evento já foi encerrado", Waitlist.canJoin(true, false, false, 0))

    @Test fun canLeave_naoEstaNaFila_returnsError() =
        assertEquals("Você não está na lista de espera", Waitlist.canLeave(false))

    @Test fun canConfirm_semVaga_returnsError() =
        assertEquals("Ainda não há vaga disponível", Waitlist.canConfirm(false, false, true, 0))

    @Test fun canConfirm_comVaga_returnsNull() = assertNull(Waitlist.canConfirm(false, false, true, 1))
}