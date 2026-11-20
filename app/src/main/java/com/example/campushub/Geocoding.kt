package com.example.campushub

object Geocoding {
    private val LAT = Regex("\"lat\"\\s*:\\s*\"(-?[0-9]+(?:\\.[0-9]+)?)\"")
    private val LON = Regex("\"lon\"\\s*:\\s*\"(-?[0-9]+(?:\\.[0-9]+)?)\"")

    // Lê latitude e longitude da resposta do Nominatim (null se o endereço não foi encontrado)
    fun parse(json: String): Pair<Double, Double>? {
        val lat = LAT.find(json)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        val lon = LON.find(json)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
        return Pair(lat, lon)
    }
}