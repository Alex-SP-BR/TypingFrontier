package com.typingfrontier.exploration

data class EventoExploracao(
    val descricao: String,
    val opcoes: List<Opcao>,

    // 🔥 INTELIGÊNCIA DO EVENTO
    val atributoIdeal: String,
    val atributoSecundario: String
)

data class Opcao(
    val texto: String,
    val atributo: String,

    val xp: Int = 0,
    val dinheiro: Int = 0,
    val dano: Int = 0,
    val energia: Int = 0
)