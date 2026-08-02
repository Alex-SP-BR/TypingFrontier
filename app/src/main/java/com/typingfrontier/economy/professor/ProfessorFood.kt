package com.typingfrontier.economy.professor

import com.typingfrontier.Player

object ProfessorFood {

    fun comer(player: Player): String {

        val custo = 8

        if (player.dinheiro < custo) {
            return "Dinheiro insuficiente para comer."
        }

        player.dinheiro -= custo
        player.energia += 15
        player.carisma += 1

        if (player.energia > 100) player.energia = 100

        return "☕ Lanche rápido de professor.\nEnergia +15\nCarisma +1"
    }
}