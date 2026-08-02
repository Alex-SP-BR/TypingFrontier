package com.typingfrontier.npc

import com.typingfrontier.economy.Equipment

data class NpcVendedor(
    val nome: String,
    val cidade: String,
    val personalidade: PersonalidadeNpc,
    val estoque: List<Equipment>
)
