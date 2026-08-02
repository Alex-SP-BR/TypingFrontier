package com.typingfrontier

import com.typingfrontier.economy.Equipment

/**
 * Definição de todos os comandos possíveis no jogo.
 */
sealed class GameAction {
    object Work : GameAction()
    object Eat : GameAction()
    object Sleep : GameAction()
    object Rest : GameAction()

    data class Train(val attribute: String, val intensity: String) : GameAction()
    data class Explore(val zoneId: String) : GameAction()
    
    // Novas ações para centralização total
    data class StudyError(val attribute: String) : GameAction()
    data class CollectRewards(val xp: Int, val money: Int) : GameAction()
    data class BuyItem(val item: Equipment) : GameAction()
    data class CompleteMission(val xp: Int, val money: Int) : GameAction()
}
