package com.example.campushub

data class ButtonState(val text: String, val enabled: Boolean) {
    companion object {
        fun registration(encerrado: Boolean, inscrito: Boolean, vagas: Int): ButtonState = when {
            encerrado -> ButtonState("Evento encerrado", false)
            inscrito -> ButtonState("Cancelar inscrição", true)
            vagas > 0 -> ButtonState("Inscrever-se", true)
            else -> ButtonState("Esgotado", false)
        }
    }
}