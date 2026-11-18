package com.example.campushub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    const val MAX_BASE64_CHARS = 700_000
    private const val MAX_LADO = 1024

    // Reduz a imagem e converte pra Base64. Retorna null se não conseguir caber no limite.
    fun toBase64(context: Context, uri: Uri): String? {
        val original = carregar(context, uri) ?: return null
        var lado = MAX_LADO
        while (lado >= 256) {
            val reduzida = redimensionar(original, lado)
            var qualidade = 85
            while (qualidade >= 40) {
                val texto = comprimir(reduzida, qualidade)
                if (texto.length <= MAX_BASE64_CHARS) return texto
                qualidade -= 15
            }
            lado /= 2
        }
        return null
    }

    // Converte o texto Base64 de volta em imagem (null se estiver vazio ou inválido)
    fun toBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun carregar(context: Context, uri: Uri): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, limites)
        }
        var amostra = 1
        while (limites.outWidth / amostra > MAX_LADO * 2 || limites.outHeight / amostra > MAX_LADO * 2) {
            amostra *= 2
        }
        val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opcoes)
        }
    }

    private fun redimensionar(bmp: Bitmap, lado: Int): Bitmap {
        val maior = maxOf(bmp.width, bmp.height)
        if (maior <= lado) return bmp
        val escala = lado.toFloat() / maior
        return Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * escala).toInt().coerceAtLeast(1),
            (bmp.height * escala).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun comprimir(bmp: Bitmap, qualidade: Int): String {
        val saida = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, qualidade, saida)
        return Base64.encodeToString(saida.toByteArray(), Base64.NO_WRAP)
    }
}