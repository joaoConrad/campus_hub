package com.example.campushub

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formatters {
    private val ptBr = Locale.forLanguageTag("pt-BR")

    private fun formatar(padrao: String, data: Date, fuso: TimeZone): String {
        val formato = SimpleDateFormat(padrao, ptBr)
        formato.timeZone = fuso
        return formato.format(data)
    }

    fun date(data: Date?, vazio: String = "a definir", fuso: TimeZone = TimeZone.getDefault()): String =
        data?.let { formatar("dd/MM/yyyy", it, fuso) } ?: vazio

    fun time(data: Date?, vazio: String = "a definir", fuso: TimeZone = TimeZone.getDefault()): String =
        data?.let { formatar("HH:mm", it, fuso) } ?: vazio

    fun dateTime(data: Date?, vazio: String = "Data a definir", fuso: TimeZone = TimeZone.getDefault()): String =
        data?.let { formatar("dd/MM/yyyy 'às' HH:mm", it, fuso) } ?: vazio

    fun commentDate(data: Date?, fuso: TimeZone = TimeZone.getDefault()): String =
        data?.let { formatar("dd/MM/yyyy HH:mm", it, fuso) } ?: ""

    fun seats(vagas: Int): String = if (vagas > 0) "$vagas vagas" else "Esgotado"

    fun averageText(media: Double?, total: Int): String {
        if (media == null) return "Avaliação média: sem avaliações"
        val texto = String.format(ptBr, "%.1f", media)
        return "Avaliação média: $texto ($total avaliações)"
    }
}