package com.typingfrontier.economy.engenheiro

import com.typingfrontier.Player

object EngenheiroFood {

    fun comer(player: Player): String {

        val custo = 8

        if (player.dinheiro < custo) {
            return "Dinheiro insuficiente para comer."
        }

        player.dinheiro -= custo
        player.energia += 16
        player.inteligencia += 1

        if (player.energia > 100) player.energia = 100

        return "🍲 Refeição de engenheiro.\nEnergia +16\nInteligência +1"
    }
}