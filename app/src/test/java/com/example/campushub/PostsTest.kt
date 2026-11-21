package com.example.campushub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostsTest {
    @Test fun rotulo_foto() = assertEquals("Foto", Posts.rotulo(PostType.FOTO))
    @Test fun rotulo_audio() = assertEquals("Áudio", Posts.rotulo(PostType.AUDIO))
    @Test fun rotulo_video() = assertEquals("Vídeo", Posts.rotulo(PostType.VIDEO))
    @Test fun rotulo_desconhecido() = assertEquals("Publicação", Posts.rotulo("outro"))

    @Test fun podeExcluir_autor() = assertTrue(Posts.podeExcluir("u1", "u1"))
    @Test fun podeExcluir_outroUsuario() = assertFalse(Posts.podeExcluir("u1", "u2"))
    @Test fun podeExcluir_semLogin() = assertFalse(Posts.podeExcluir("", ""))

    @Test fun validate_ok() = assertNull(Posts.validate(PostType.FOTO, "/dados/foto.jpg"))
    @Test fun validate_tipoInvalido() =
        assertEquals("Tipo de publicação inválido", Posts.validate("gif", "/dados/a.gif"))
    @Test fun validate_semCaminho() =
        assertEquals("Arquivo não encontrado", Posts.validate(PostType.VIDEO, " "))
}