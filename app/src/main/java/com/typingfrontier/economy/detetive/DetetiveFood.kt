package com.typingfrontier.economy.detetive

import com.typingfrontier.Player

object DetetiveFood {

    fun comerLanche(player: Player): String {

        if (player.profissao != "Detetive") {
            return "Apenas detetives podem usar esta ação."
        }

        val custo = 8

        if (player.dinheiro < custo) {
            return "Dinheiro insuficiente para comer."
        }

        player.dinheiro -= custo
        player.energia += 20
        player.cansacoMental -= 10

        player.energia = player.energia.coerceAtMost(100)
        player.cansacoMental = player.cansacoMental.coerceAtLeast(0)

        return "🥪 Você comeu um lanche.\nEnergia +20\nCansaço mental -10"
    }
}

