package com.typingfrontier.mental

/**
 * Resolvedor específico para a categoria EQUACAO.
 * Atualmente trata equações de primeiro grau simples no formato: x + b = resultado.
 */
object EquationSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.EQUACAO) {
            throw IllegalArgumentException("O EquationSolver suporta apenas a categoria EQUACAO.")
        }

        if (question.operandos.size < 2) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar a equação.")
            )
        }

        // De acordo com o contrato do MathGenerator:
        // operandos[0] = b
        // operandos[1] = resultado da soma (x + b)
        val b = question.operandos[0]
        val total = question.operandos[1]

        val passos = mutableListOf<String>()
        passos.add("Operação: Equação")

        when (question.subTipo) {
            0 -> {
                // x + b = total -> x = total - b
                val x = total - b
                
                val sB = formatar(b)
                val sTotal = formatar(total)
                val sX = formatar(x)

                passos.add("x + $sB = $sTotal")
                passos.add("x = $sTotal - $sB")
                passos.add("x = $sX")

                return MathSolution(
                    resultadoFinal = x,
                    passos = passos
                )
            }
            else -> {
                return MathSolution(
                    resultadoFinal = question.respostaCorreta,
                    passos = listOf("Formato de equação não reconhecido (Subtipo: ${question.subTipo}).")
                )
            }
        }
    }
}
