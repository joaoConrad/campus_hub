package com.example.campushub

import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class EventFilterTest {
    private val oneDay = 24 * 60 * 60 * 1000L
    private val now = Date(1_800_000_000_000L)

    private val lecture = Event(
        id = "1", titulo = "Palestra de IA", categoria = "Palestra",
        dataHora = Timestamp(Date(now.time + oneDay))
    )
    private val workshop = Event(
        id = "2", titulo = "Oficina de Kotlin", categoria = "Oficina",
        dataHora = Timestamp(Date(now.time - oneDay))
    )
    private val noDate = Event(id = "3", titulo = "Evento sem data", categoria = "Palestra")

    private val all = listOf(lecture, workshop, noDate)

    private fun filter(query: String = "", category: String = "Todas", status: String = "Todos") =
        EventFilter.apply(all, query, category, status, now).map { it.id }

    @Test
    fun apply_noFilters_returnsAll() {
        assertEquals(listOf("1", "2", "3"), filter())
    }

    @Test
    fun apply_searchByTitle_ignoresCase() {
        assertEquals(listOf("2"), filter(query = "kotlin"))
    }

    @Test
    fun apply_search_ignoresSurroundingSpaces() {
        assertEquals(listOf("1"), filter(query = "  IA  "))
    }

    @Test
    fun apply_searchWithoutMatch_returnsEmpty() {
        assertEquals(emptyList<String>(), filter(query = "xyz"))
    }

    @Test
    fun apply_category_filtersByCategory() {
        assertEquals(listOf("1", "3"), filter(category = "Palestra"))
    }

    @Test
    fun apply_category_ignoresCase() {
        assertEquals(listOf("2"), filter(category = "oficina"))
    }

    @Test
    fun apply_upcoming_excludesClosedEvents() {
        assertEquals(listOf("1", "3"), filter(status = EventFilter.PROXIMOS))
    }

    @Test
    fun apply_closed_returnsOnlyClosedEvents() {
        assertEquals(listOf("2"), filter(status = EventFilter.ENCERRADOS))
    }

    @Test
    fun apply_combinedFilters() {
        assertEquals(listOf("1"), filter(query = "palestra", category = "Palestra", status = EventFilter.PROXIMOS))
    }
}