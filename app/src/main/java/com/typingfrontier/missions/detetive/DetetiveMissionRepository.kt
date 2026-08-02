package com.typingfrontier.missions.detetive

import com.typingfrontier.missions.DetetiveMission

object DetetiveMissionRepository {

    private val missoes = listOf(
        DetetiveMission(
            titulo = "Depoimento Contraditório",
            descricao = "Uma testemunha muda o horário do crime duas vezes.",
            escolhaA = "Confrontar imediatamente",
            escolhaB = "Analisar antes as inconsistências",
            correta = 'B',
            recompensaXP = 20,
            recompensaDinheiro = 50,
            explicacao = "Um bom detetive coleta evidências antes de confrontar."
        ),

        DetetiveMission(
            titulo = "Cena do Crime",
            descricao = "Você encontra uma faca sem digitais.",
            escolhaA = "Descartar a faca",
            escolhaB = "Enviar para perícia",
            correta = 'B',
            recompensaXP = 25,
            recompensaDinheiro = 70,
            explicacao = "Mesmo sem digitais, resíduos podem ser analisados."
        )
    )

    fun getMissao(): DetetiveMission {
        return missoes.random()
    }
}


