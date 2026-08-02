package com.typingfrontier.economy.policial

import com.typingfrontier.Player

object PolicialFood {

    fun comer(player: Player): String {

        val custo = 8

        if (player.dinheiro < custo) {
            return "Dinheiro insuficiente para comer."
        }

        player.dinheiro -= custo
        player.energia += 18
        player.resistencia += 1

        if (player.energia > 100) player.energia = 100

        return "🍔 Refeição reforçada de policial.\nEnergia +18\nResistência +1"
    }
}