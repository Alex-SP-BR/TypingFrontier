package com.typingfrontier.economy.medico

import com.typingfrontier.Player

object MedicoFood {

    fun comer(player: Player): String {

        val custo = 8

        if (player.dinheiro < custo) {
            return "Dinheiro insuficiente para comer."
        }

        player.dinheiro -= custo
        player.energia += 17
        player.inteligencia += 1

        if (player.energia > 100) player.energia = 100

        return "🥗 Refeição saudável de médico.\nEnergia +17\nInteligência +1"
    }
}