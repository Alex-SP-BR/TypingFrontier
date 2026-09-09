package com.typingfrontier.mental

/**
 * Resolvedor específico para a categoria PORCENTAGEM.
 * Trata o cálculo de percentuais sobre valores base.
 */
object PercentageSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.PORCENTAGEM) {
            throw IllegalArgumentException("O PercentageSolver suporta apenas a categoria PORCENTAGEM.")
        }

        if (question.operandos.size < 2) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar o cálculo de porcentagem.")
            )
        }

        val percentual = question.operandos[0]
        val valorBase = question.operandos[1]

        val passos = mutableListOf<String>()
        val resultadoFinal: Double

        when (question.subTipo) {
            0 -> {
                // Cálculo independente: (valor * perc) / 100
                val fator = percentual / 100.0
                resultadoFinal = valorBase * fator

                val sPerc = formatar(percentual)
                val sValor = formatar(valorBase)
                val sFator = formatar(fator)
                val sRes = formatar(resultadoFinal)

                passos.add("Operação: Porcentagem")
                passos.add("$sPerc% de $sValor")
                passos.add("$sPerc% = $sPerc ÷ 100 = $sFator")
                passos.add("$sFator x $sValor = $sRes")
            }
            else -> {
                return MathSolution(
                    resultadoFinal = question.respostaCorreta,
                    passos = listOf("Subtipo de porcentagem não reconhecido.")
                )
            }
        }

        return MathSolution(
            resultadoFinal = resultadoFinal,
            passos = passos
        )
    }
}
