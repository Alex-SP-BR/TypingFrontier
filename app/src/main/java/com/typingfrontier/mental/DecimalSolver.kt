package com.typingfrontier.mental

/**
 * Resolvedor específico para a categoria DECIMAIS.
 * Trata operações de soma, subtração e multiplicação com números decimais.
 */
object DecimalSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.DECIMAIS) {
            throw IllegalArgumentException("O DecimalSolver suporta apenas a categoria DECIMAIS.")
        }

        if (question.operandos.size < 2) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar o cálculo decimal.")
            )
        }

        val a = question.operandos[0]
        val b = question.operandos[1]

        val (opNome, resultado, simbolo) = when (question.subTipo) {
            1 -> Triple("Soma Decimal", a + b, "+")
            2 -> Triple("Subtração Decimal", a - b, "-")
            3 -> Triple("Multiplicação Decimal", a * b, "x")
            else -> {
                return MathSolution(
                    resultadoFinal = question.respostaCorreta,
                    passos = listOf("Operação decimal não reconhecida (Subtipo: ${question.subTipo}).")
                )
            }
        }

        val passos = mutableListOf<String>()
        passos.add("Operação: $opNome")

        // Formatação controlada através da interface comum
        val sA = formatar(a)
        val sB = formatar(b)
        val sRes = formatar(resultado)

        passos.add("$sA $simbolo $sB = $sRes")

        return MathSolution(
            resultadoFinal = resultado,
            passos = passos
        )
    }
}
