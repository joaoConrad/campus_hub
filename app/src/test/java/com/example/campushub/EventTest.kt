package com.example.campushub

import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventTest {
    private val oneDay = 24 * 60 * 60 * 1000L

    @Test
    fun isClosed_pastEvent_returnsTrue() {
        val event = Event(dataHora = Timestamp(Date(System.currentTimeMillis() - oneDay)))
        assertTrue(event.estaEncerrado())
    }

    @Test
    fun isClosed_futureEvent_returnsFalse() {
        val event = Event(dataHora = Timestamp(Date(System.currentTimeMillis() + oneDay)))
        assertFalse(event.estaEncerrado())
    }

    @Test
    fun isClosed_eventWithoutDate_returnsFalse() {
        assertFalse(Event().estaEncerrado())
    }

    @Test
    fun isClosed_usesGivenTime() {
        val event = Event(dataHora = Timestamp(Date(1_000L)))
        assertTrue(event.estaEncerrado(Date(2_000L)))
        assertFalse(event.estaEncerrado(Date(500L)))
    }

    @Test
    fun defaultConstructor_hasEmptyFields() {
        val event = Event()
        assertEquals("", event.titulo)
        assertEquals(0, event.vagas)
        assertEquals(event, event.copy())
    }

    @Test
    fun copy_keepsEqualityAndHashCode() {
        val event = Event(id = "1", titulo = "T", latitude = 1.0)
        assertEquals(event, event.copy())
        assertEquals(event.hashCode(), event.copy().hashCode())
    }

    @Test
    fun toString_containsTitle() {
        assertTrue(Event(titulo = "T").toString().contains("titulo=T"))
    }
}