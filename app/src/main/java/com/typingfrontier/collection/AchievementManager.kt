package com.typingfrontier.collection

import android.content.Context
import com.typingfrontier.PlayerManager

object AchievementManager {

    fun checkExploration(context: Context, zoneId: String) {
        val p = PlayerManager.player
        p.zonasExploradas.add(zoneId)
        val count = p.zonasExploradas.size

        if (count >= 1) PlayerManager.desbloquearConquista(context, "exp_1")
        if (count >= 2) PlayerManager.desbloquearConquista(context, "exp_2")
        if (count >= 3) PlayerManager.desbloquearConquista(context, "exp_3")
        if (count >= 4) PlayerManager.desbloquearConquista(context, "exp_4")
        if (count >= 5) PlayerManager.desbloquearConquista(context, "exp_5")
        if (count >= 6) PlayerManager.desbloquearConquista(context, "exp_6")
        if (count >= 7) PlayerManager.desbloquearConquista(context, "exp_7")
        
        // exp_8: Jornada Concluída (Todas as 7 iniciais)
        if (count >= 7) PlayerManager.desbloquearConquista(context, "exp_8")

        checkSupremas(context)
    }

    fun checkPhysical(context: Context) {
        val p = PlayerManager.player
        val min = minOf(p.forca, p.resistencia, p.velocidade)

        if (min >= 5) PlayerManager.desbloquearConquista(context, "fis_10")
        if (min >= 10) PlayerManager.desbloquearConquista(context, "fis_10_2")
        if (min >= 30) PlayerManager.desbloquearConquista(context, "fis_30")
        if (min >= 50) PlayerManager.desbloquearConquista(context, "fis_50")
        if (min >= 100) PlayerManager.desbloquearConquista(context, "fis_100")
        if (min >= 200) PlayerManager.desbloquearConquista(context, "fis_200")
        if (min >= 300) PlayerManager.desbloquearConquista(context, "fis_300")
        if (min >= 500) PlayerManager.desbloquearConquista(context, "fis_500")
        if (min >= 1000) PlayerManager.desbloquearConquista(context, "fis_1000")

        val avg = (p.forca + p.resistencia + p.velocidade) / 3
        if (avg >= 15) PlayerManager.desbloquearConquista(context, "fis_consistente")
        if (avg >= 25) PlayerManager.desbloquearConquista(context, "fis_disciplina")
        if (avg >= 75) PlayerManager.desbloquearConquista(context, "fis_evolucao")

        checkSupremas(context)
    }

    fun checkMental(context: Context, isCorrect: Boolean, subject: String) {
        val p = PlayerManager.player
        if (isCorrect) {
            p.mentalStreak++
            val streak = p.mentalStreak

            if (streak >= 5) PlayerManager.desbloquearConquista(context, "men_1")
            if (streak >= 10) PlayerManager.desbloquearConquista(context, "men_10")
            if (streak >= 20) PlayerManager.desbloquearConquista(context, "men_20")
            if (streak >= 30) PlayerManager.desbloquearConquista(context, "men_30")
            if (streak >= 50) PlayerManager.desbloquearConquista(context, "men_50")
            if (streak >= 100) PlayerManager.desbloquearConquista(context, "men_100")
            if (streak >= 200) PlayerManager.desbloquearConquista(context, "men_200")
            if (streak >= 300) PlayerManager.desbloquearConquista(context, "men_300")
            if (streak >= 500) PlayerManager.desbloquearConquista(context, "men_500")
            if (streak >= 1000) PlayerManager.desbloquearConquista(context, "men_1000")
            if (streak >= 150) PlayerManager.desbloquearConquista(context, "men_focus")
            if (streak >= 250) PlayerManager.desbloquearConquista(context, "men_precision")
        } else {
            p.mentalStreak = 0
        }

        if (p.inteligencia >= 50) PlayerManager.desbloquearConquista(context, "men_math")
        if (p.carisma >= 50) PlayerManager.desbloquearConquista(context, "men_port")
        if (p.inteligencia >= 100 && p.carisma >= 100) PlayerManager.desbloquearConquista(context, "men_knowledge")

        checkSupremas(context)
    }

    fun checkEconomy(context: Context) {
        val p = PlayerManager.player
        val grana = p.dinheiro

        if (grana >= 10000) PlayerManager.desbloquearConquista(context, "eco_1")
        if (grana >= 100000) PlayerManager.desbloquearConquista(context, "eco_100k")
        if (grana >= 500000) PlayerManager.desbloquearConquista(context, "eco_500k")
        if (grana >= 1000000) PlayerManager.desbloquearConquista(context, "eco_1m")
        if (grana >= 5000000) PlayerManager.desbloquearConquista(context, "eco_5m")
        if (grana >= 10000000) PlayerManager.desbloquearConquista(context, "eco_10m")

        checkSupremas(context)
    }

    fun checkSupremas(context: Context) {
        val p = PlayerManager.player
        val unlocks = p.conquistasDesbloqueadas

        // 1. Explorador Supremo (Todas as 16 de exp)
        val expAll = (1..16).all { unlocks.contains("exp_$it") }
        if (expAll) PlayerManager.desbloquearConquista(context, "sup_explora")

        // 2. Treinador Supremo
        val fisAll = listOf("fis_10", "fis_10_2", "fis_30", "fis_50", "fis_100", "fis_200", "fis_300", "fis_500", "fis_1000").all { unlocks.contains(it) }
        if (fisAll) PlayerManager.desbloquearConquista(context, "sup_fisico")

        // 3. Mestre Mental
        val menAll = listOf("men_1", "men_10", "men_20", "men_30", "men_50", "men_100", "men_200", "men_300", "men_500", "men_1000").all { unlocks.contains(it) }
        if (menAll) PlayerManager.desbloquearConquista(context, "sup_mental")

        // 4. Mestre da Economia
        val ecoAll = listOf("eco_1", "eco_100k", "eco_500k", "eco_1m", "eco_5m", "eco_10m").all { unlocks.contains(it) }
        if (ecoAll) PlayerManager.desbloquearConquista(context, "sup_economia")

        // 5. Excelência Geral (Todos os supremos de categoria)
        if (unlocks.contains("sup_explora") && unlocks.contains("sup_fisico") && 
            unlocks.contains("sup_mental") && unlocks.contains("sup_economia")) {
            PlayerManager.desbloquearConquista(context, "sup_excelencia")
        }

        // 8. Conquista Máxima
        if (unlocks.contains("sup_excelencia") && unlocks.size >= 50) {
            PlayerManager.desbloquearConquista(context, "sup_maxima")
        }
    }
}
