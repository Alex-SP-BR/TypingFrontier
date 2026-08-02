package com.typingfrontier.missions

data class DetetiveMission(
    val titulo: String,
    val descricao: String,
    val escolhaA: String,
    val escolhaB: String,
    val correta: Char, // 'A' ou 'B'
    val recompensaXP: Int,
    val recompensaDinheiro: Int,
    val explicacao: String
)