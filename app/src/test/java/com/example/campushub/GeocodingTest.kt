package com.example.campushub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeocodingTest {

    @Test
    fun parse_respostaValida_returnsCoordinates() {
        val json = """[{"place_id":1,"lat":"-29.7894","lon":"-55.7919","display_name":"Santiago"}]"""
        assertEquals(Pair(-29.7894, -55.7919), Geocoding.parse(json))
    }

    @Test
    fun parse_listaVazia_returnsNull() {
        assertNull(Geocoding.parse("[]"))
    }

    @Test
    fun parse_textoInvalido_returnsNull() {
        assertNull(Geocoding.parse("erro"))
    }
}