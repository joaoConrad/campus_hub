package com.example.campushub

object Ratings {
    fun average(notas: List<Long>): Double? =
        if (notas.isEmpty()) null else notas.average()
}