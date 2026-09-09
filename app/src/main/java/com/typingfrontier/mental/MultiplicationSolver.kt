package com.typingfrontier.mental

/**
 * Resolvedor específico para a categoria MULTIPLICACAO.
 */
object MultiplicationSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.MULTIPLICACAO) {
            throw IllegalArgumentException("O MultiplicationSolver suporta apenas a categoria MULTIPLICACAO.")
        }

        if (question.operandos.size < 2) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar a multiplicação.")
            )
        }

        val a = question.operandos[0]
        val b = question.operandos[1]
        val resultado = a * b

        // Geração de passos didáticos simples (Nível 1 - Tabuada)
        val passos = mutableListOf<String>()
        
        // Formatação seguindo o padrão centralizado
        val sA = formatar(a)
        val sB = formatar(b)
        val sRes = formatar(resultado)

        passos.add("Operação: Multiplicação")
        passos.add("$sA x $sB = $sRes")

        return MathSolution(
            resultadoFinal = resultado,
            passos = passos
        )
    }
}
