package com.typingfrontier.mental

object PortuguesePerformance {

    private val erros = mutableMapOf<PortugueseExerciseType, Int>()

    fun registrarErro(tipo: PortugueseExerciseType) {
        erros[tipo] = erros.getOrDefault(tipo, 0) + 1
    }

    fun registrarAcerto(tipo: PortugueseExerciseType) {
        erros[tipo] = (erros.getOrDefault(tipo, 0) - 1).coerceAtLeast(0)
    }

    fun tipoMaisFraco(): PortugueseExerciseType? {
        return erros.maxByOrNull { it.value }?.key
    }
}