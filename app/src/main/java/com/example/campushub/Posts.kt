package com.example.campushub

object Posts {

    fun rotulo(tipo: String): String = when (tipo) {
        PostType.FOTO -> "Foto"
        PostType.AUDIO -> "Áudio"
        PostType.VIDEO -> "Vídeo"
        else -> "Publicação"
    }

    // só o autor pode excluir a própria publicação
    fun podeExcluir(autorId: String, uid: String): Boolean =
        uid.isNotEmpty() && autorId == uid

    fun validate(tipo: String, caminho: String): String? {
        if (tipo != PostType.FOTO && tipo != PostType.AUDIO && tipo != PostType.VIDEO) {
            return "Tipo de publicação inválido"
        }
        if (caminho.isBlank()) return "Arquivo não encontrado"
        return null
    }
}