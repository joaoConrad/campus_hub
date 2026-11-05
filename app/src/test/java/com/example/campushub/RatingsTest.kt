package com.example.campushub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatingsTest {

    @Test
    fun average_emptyList_returnsNull() {
        assertNull(Ratings.average(emptyList()))
    }

    @Test
    fun average_singleScore_returnsThatScore() {
        assertEquals(5.0, Ratings.average(listOf(5L))!!, 0.0001)
    }

    @Test
    fun average_multipleScores_returnsMean() {
        assertEquals(4.5, Ratings.average(listOf(4L, 5L))!!, 0.0001)
    }
}