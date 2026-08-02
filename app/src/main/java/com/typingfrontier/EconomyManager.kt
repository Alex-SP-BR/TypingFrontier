package com.typingfrontier

import com.typingfrontier.economy.ProfessionManager

object EconomyManager {

    fun fatorInflacao(): Double {
        val p = PlayerManager.player

        val fatorNivel = 1.0 + (p.nivel * 0.05)
        
        // Novo cálculo baseado no equipamento atual
        val equip = ProfessionManager.getEquipment(p.equipamentoId)
        val fatorEquipamento = if (equip != null) 0.2 else 0.0

        return fatorNivel + fatorEquipamento
    }

    fun precoInflacionado(precoBase: Int): Int {
        return (precoBase * fatorInflacao()).toInt()
    }

    fun recompensaInflacionada(valorBase: Int): Int {
        return (valorBase * (fatorInflacao() * 0.8)).toInt()
    }
}
