package com.typingfrontier.mental

/**
 * Resolvedor específico para a categoria PROBLEMAS.
 * Resolve problemas narrativos de uma única operação básica.
 */
object ProblemasSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.PROBLEMAS) {
            throw IllegalArgumentException("O ProblemasSolver suporta apenas a categoria PROBLEMAS.")
        }

        if (question.operandos.size < 2) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar o problema.")
            )
        }

        val a = question.operandos[0]
        val b = question.operandos[1]

        val passos = mutableListOf<String>()
        passos.add("Operação: Problema")

        val resultadoFinal: Double
        val sA = formatar(a)
        val sB = formatar(b)

        when (question.subTipo) {
            0 -> { // Soma
                resultadoFinal = a + b
                passos.add("Identificamos uma soma de valores.")
                passos.add("$sA + $sB = ${formatar(resultadoFinal)}")
            }
            1 -> { // Subtração
                resultadoFinal = a - b
                passos.add("Identificamos uma subtração (gasto/perda).")
                passos.add("$sA - $sB = ${formatar(resultadoFinal)}")
            }
            2 -> { // Multiplicação
                resultadoFinal = a * b
                passos.add("Identificamos uma multiplicação (preço x quantidade).")
                passos.add("$sA x $sB = ${formatar(resultadoFinal)}")
            }
            3 -> { // Divisão
                if (b == 0.0) {
                    return MathSolution(resultadoFinal = 0.0, passos = listOf("Erro: Divisão por zero."))
                }
                resultadoFinal = a / b
                passos.add("Identificamos uma divisão de valores.")
                passos.add("$sA ÷ $sB = ${formatar(resultadoFinal)}")
            }
            else -> {
                return MathSolution(
                    resultadoFinal = question.respostaCorreta,
                    passos = listOf("Tipo de problema não reconhecido.")
                )
            }
        }

        return MathSolution(
            resultadoFinal = resultadoFinal,
            passos = passos
        )
    }
}
