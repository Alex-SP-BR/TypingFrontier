package com.typingfrontier.mental

/**
 * Resolvedor específico para a categoria DIVISAO.
 */
object DivisionSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.DIVISAO) {
            throw IllegalArgumentException("O DivisionSolver suporta apenas a categoria DIVISAO.")
        }

        if (question.operandos.size < 2) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar a divisão.")
            )
        }

        val a = question.operandos[0]
        val b = question.operandos[1]

        if (b == 0.0) {
            return MathSolution(
                resultadoFinal = 0.0,
                passos = listOf("Erro: Divisão por zero não é permitida.")
            )
        }

        val resultado = a / b

        // Geração de passos didáticos simples (Nível 1)
        val passos = mutableListOf<String>()
        
        // Formatação seguindo o padrão centralizado
        val sA = formatar(a)
        val sB = formatar(b)
        val sRes = formatar(resultado)

        passos.add("Operação: Divisão")
        passos.add("$sA ÷ $sB = $sRes")

        return MathSolution(
            resultadoFinal = resultado,
            passos = passos
        )
    }
}
