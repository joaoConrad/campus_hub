package com.example.campushub

import java.util.Date
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val date = Date(0L) // 01/01/1970 00:00 UTC

    @Test
    fun date_formatsDayMonthYear() {
        assertEquals("01/01/1970", Formatters.date(date, fuso = utc))
    }

    @Test
    fun date_null_returnsDefaultText() {
        assertEquals("a definir", Formatters.date(null))
    }

    @Test
    fun time_formatsHourMinute() {
        assertEquals("00:00", Formatters.time(date, fuso = utc))
    }

    @Test
    fun time_null_returnsDefaultText() {
        assertEquals("a definir", Formatters.time(null))
    }

    @Test
    fun dateTime_formatsWithAs() {
        assertEquals("01/01/1970 às 00:00", Formatters.dateTime(date, fuso = utc))
    }

    @Test
    fun dateTime_null_returnsDefaultText() {
        assertEquals("Data a definir", Formatters.dateTime(null))
    }

    @Test
    fun commentDate_formatsDate() {
        assertEquals("01/01/1970 00:00", Formatters.commentDate(date, utc))
    }

    @Test
    fun commentDate_null_returnsEmpty() {
        assertEquals("", Formatters.commentDate(null))
    }

    @Test
    fun seats_withSeats_returnsCount() {
        assertEquals("5 vagas", Formatters.seats(5))
    }

    @Test
    fun seats_noSeats_returnsSoldOut() {
        assertEquals("Esgotado", Formatters.seats(0))
    }

    @Test
    fun averageText_noRatings_returnsNoRatingsText() {
        assertEquals("Avaliação média: sem avaliações", Formatters.averageText(null, 0))
    }

    @Test
    fun averageText_withRatings_usesComma() {
        assertEquals("Avaliação média: 4,5 (2 avaliações)", Formatters.averageText(4.5, 2))
    }
}