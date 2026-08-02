package com.typingfrontier.exploration

import com.typingfrontier.Player

object ExplorationResolver {

    fun resolver(player: Player, opcao: Opcao): String {

        val valor = when (opcao.atributo) {
            "FORCA" -> player.forca
            "INT" -> player.inteligencia
            "CARISMA" -> player.carisma
            "RESISTENCIA" -> player.resistencia
            else -> 0
        }

        val dificuldade = (5..15).random()

        return if (valor >= dificuldade) {
            "✅ Sucesso!"
        } else {
            "❌ Falhou!"
        }
    }
}