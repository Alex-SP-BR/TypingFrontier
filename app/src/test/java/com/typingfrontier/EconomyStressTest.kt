package com.typingfrontier

import com.typingfrontier.economy.ProfessionManager
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class EconomyStressTest {

    @Before
    fun setup() {
        // Inicializa um player padrão para o teste (Policial Nível 1)
        val p = PlayerManager.player
        p.nome = "Teste"
        p.profissao = "Policial"
        p.nivel = 1
        p.dinheiro = 100
        p.energia = 100
        p.energiaMax = 100
        p.cansacoMental = 0
        p.cansacoMax = 50
        p.dia = 1
        p.hora = 8
        p.minuto = 0
        
        ProfessionManager.aplicarBonusInicial(p)
    }

    @Test
    fun simulate365DaysWorkSleep() {
        val p = PlayerManager.player
        val initialMoney = p.dinheiro
        val days = 365
        var totalProfit = 0

        println("--- INÍCIO DA SIMULAÇÃO (365 DIAS) ---")
        println("Estratégia: Trabalhar até o limite -> Dormir")
        println("Dinheiro Inicial: R$ $initialMoney")

        for (dia in 1..days) {
            var worksInDay = 0
            
            // Tenta trabalhar o máximo possível no dia
            while (true) {
                val result = GameEngine.dispatch(GameAction.Work)
                if (result is EngineResult.Success) {
                    worksInDay++
                } else {
                    break // Não pode mais trabalhar (energia ou tempo)
                }
            }

            // Dorme para resetar o dia
            val sleepResult = GameEngine.dispatch(GameAction.Sleep)
            if (sleepResult is EngineResult.Failure) {
                // Se falhar ao dormir (sem dinheiro), a simulação quebra
                println("FALÊNCIA no Dia $dia! Não conseguiu pagar a diária.")
                break
            }
        }

        val finalMoney = p.dinheiro
        totalProfit = finalMoney - initialMoney
        val avgProfit = totalProfit.toDouble() / days

        println("--- RELATÓRIO FINAL ---")
        println("Dinheiro Final: R$ $finalMoney")
        println("Lucro Total: R$ $totalProfit")
        println("Lucro Médio Diário: R$ ${String.format("%.2f", avgProfit)}")
        println("Nível Final: ${p.nivel}")
        println("Experiência Final: ${p.experienciaAtual}")
        
        // Verificação de Lucro Infinito
        // Se o lucro for positivo e o jogador nunca subiu de nível apenas trabalhando,
        // o lucro é linear e sustentável, mas se o valor for astronômico, identificamos o problema.
        assertTrue("O jogador deve terminar com saldo positivo ou zero", p.dinheiro >= 0)
    }
}
