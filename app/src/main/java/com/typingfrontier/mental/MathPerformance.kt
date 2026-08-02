package com.typingfrontier.mental

object MathPerformance {

    private val erros = mutableMapOf<MathExerciseType, Int>()
    private val acertos = mutableMapOf<MathExerciseType, Int>()

    fun registrarErro(tipo: MathExerciseType) {
        erros[tipo] = erros.getOrDefault(tipo, 0) + 1
    }

    fun registrarAcerto(tipo: MathExerciseType) {
        acertos[tipo] = acertos.getOrDefault(tipo, 0) + 1
    }

    fun tipoMaisFraco(): MathExerciseType? {

        val tipos = MathExerciseType.values()

        return tipos.maxByOrNull { tipo ->
            val e = erros.getOrDefault(tipo, 0)
            val a = acertos.getOrDefault(tipo, 0)
            e - a
        }
    }
}