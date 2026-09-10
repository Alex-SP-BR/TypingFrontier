package com.typingfrontier.exploration

import com.typingfrontier.Player

object ExplorationResolver {

    fun resolver(player: Player, opcao: Opcao): String {

        val valor = when (opcao.atributo) {
            "FORCA" -> player.forcaEfetiva
            "INT" -> player.inteligenciaEfetiva
            "CARISMA" -> player.carismaEfetiva
            "RESISTENCIA" -> player.resistenciaEfetiva
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