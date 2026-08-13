package com.typingfrontier.mental

object PortugueseExerciseSelector {

    private var ultimoTipo: PortugueseExerciseType? = null
    private val cicloAtual = mutableListOf<PortugueseExerciseType>()

    fun escolher(atributo: Int): PortugueseExerciseType {
        // 1. Obtenção dos Desbloqueados (Progressão Cumulativa)
        val desbloqueados = obterDesbloqueados(atributo)

        // 2. Mecânica de Reforço (40% de chance) - Apenas se já desbloqueado
        val tipoFraco = PortuguesePerformance.tipoMaisFraco()
        if (tipoFraco != null && desbloqueados.contains(tipoFraco) && (1..100).random() <= 40) {
            ultimoTipo = tipoFraco
            return tipoFraco
        }

        // 3. Lógica de Ciclo Embaralhado (60% de chance ou fallback)
        if (cicloAtual.isEmpty()) {
            cicloAtual.addAll(desbloqueados.shuffled())
            
            // Proteção contra repetição imediata entre o fim de um ciclo e início do outro
            if (cicloAtual.size > 1 && cicloAtual[0] == ultimoTipo) {
                val repetido = cicloAtual.removeAt(0)
                cicloAtual.add(repetido)
            }
        }

        val escolhido = if (cicloAtual.isNotEmpty()) {
            cicloAtual.removeAt(0)
        } else {
            desbloqueados.random()
        }

        ultimoTipo = escolhido
        return escolhido
    }

    private fun obterDesbloqueados(n: Int): List<PortugueseExerciseType> {
        val list = mutableListOf(PortugueseExerciseType.SILABAS)
        if (n >= 5) list.add(PortugueseExerciseType.GENERO)
        if (n >= 10) list.add(PortugueseExerciseType.PLURAL)
        if (n >= 20) list.add(PortugueseExerciseType.SUJEITO)
        if (n >= 30) list.add(PortugueseExerciseType.OBJETO_DIRETO)
        if (n >= 40) { 
            list.add(PortugueseExerciseType.VERBO)
            list.add(PortugueseExerciseType.ORTOGRAFIA) 
        }
        if (n >= 50) list.add(PortugueseExerciseType.ACENTUACAO)
        if (n >= 60) list.add(PortugueseExerciseType.CLASSE_GRAMATICAL)
        if (n >= 70) list.add(PortugueseExerciseType.VOCABULARIO)
        if (n >= 80) list.add(PortugueseExerciseType.INTERPRETACAO)
        return list
    }
}
