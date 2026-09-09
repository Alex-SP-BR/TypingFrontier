package com.typingfrontier.mental

/**
 * Resolvedor específico para a categoria OPERACOES_COMBINADAS.
 * Trata expressões com prioridade de operações (multiplicação vs parênteses).
 */
object CombinedOperationsSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.OPERACOES_COMBINADAS) {
            throw IllegalArgumentException("O CombinedOperationsSolver suporta apenas a categoria OPERACOES_COMBINADAS.")
        }

        if (question.operandos.size < 3) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar as operações combinadas.")
            )
        }

        val a = question.operandos[0]
        val b = question.operandos[1]
        val c = question.operandos[2]

        val passos = mutableListOf<String>()
        val resultadoFinal: Double

        when (question.subTipo) {
            1 -> {
                // a + b x c (Multiplicação primeiro)
                val mult = b * c
                resultadoFinal = a + mult

                passos.add("Operação: Ordem das operações")
                passos.add("Primeiro, resolvemos a multiplicação:")
                passos.add("${formatar(b)} x ${formatar(c)} = ${formatar(mult)}")
                passos.add("Agora fazemos a soma:")
                passos.add("${formatar(a)} + ${formatar(mult)} = ${formatar(resultadoFinal)}")
            }
            2 -> {
                // (a + b) x c (Parênteses primeiro)
                val soma = a + b
                resultadoFinal = soma * c

                passos.add("Operação: Parênteses")
                passos.add("Primeiro, resolvemos o que está dentro dos parênteses:")
                passos.add("${formatar(a)} + ${formatar(b)} = ${formatar(soma)}")
                passos.add("Agora fazemos a multiplicação:")
                passos.add("${formatar(soma)} x ${formatar(c)} = ${formatar(resultadoFinal)}")
            }
            else -> {
                return MathSolution(
                    resultadoFinal = question.respostaCorreta,
                    passos = listOf("Subtipo de operação combinada não reconhecido.")
                )
            }
        }

        return MathSolution(
            resultadoFinal = resultadoFinal,
            passos = passos
        )
    }
}
