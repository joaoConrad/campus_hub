package com.example.campushub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.Assert.assertTrue

class CommentTest {

    @Test
    fun defaultConstructor_hasEmptyFields() {
        val comment = Comment()
        assertEquals("", comment.id)
        assertEquals("", comment.texto)
        assertNull(comment.data)
    }

    @Test
    fun copy_changesOnlyRequestedField() {
        val comment = Comment(eventId = "e1", autorId = "u1", autorNome = "Ana", texto = "oi")
        val edited = comment.copy(texto = "tchau")
        assertEquals("tchau", edited.texto)
        assertEquals("u1", edited.autorId)
    }

    @Test
    fun equality_comparesByValue() {
        assertEquals(Comment(texto = "a"), Comment(texto = "a"))
        assertNotEquals(Comment(texto = "a"), Comment(texto = "b"))
    }

    @Test
    fun copy_keepsEqualityAndHashCode() {
        val comment = Comment(id = "1", texto = "oi")
        assertEquals(comment, comment.copy())
        assertEquals(comment.hashCode(), comment.copy().hashCode())
    }

    @Test
    fun toString_containsText() {
        assertTrue(Comment(texto = "oi").toString().contains("texto=oi"))
    }
}