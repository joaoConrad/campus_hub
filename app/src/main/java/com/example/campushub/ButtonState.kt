package com.example.campushub

data class ButtonState(val text: String, val enabled: Boolean) {
    companion object {
        fun registration(
            encerrado: Boolean, inscrito: Boolean, vagas: Int,
            fila: Int = 0, naFila: Boolean = false, primeiroDaFila: Boolean = false
        ): ButtonState = when {
            encerrado -> ButtonState("Evento encerrado", false)
            inscrito -> ButtonState("Cancelar inscrição", true)
            naFila && primeiroDaFila && vagas > 0 -> ButtonState("Confirmar inscrição", true)
            naFila -> ButtonState("Sair da lista de espera", true)
            vagas - fila > 0 -> ButtonState("Inscrever-se", true)
            else -> ButtonState("Entrar na lista de espera", true)
        }
    }
}