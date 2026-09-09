package com.typingfrontier.mental

/**
 * Resolvedor específico para a categoria SOMA.
 */
object AdditionSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.SOMA) {
            throw IllegalArgumentException("O AdditionSolver suporta apenas a categoria SOMA.")
        }

        if (question.operandos.size < 2) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar a soma.")
            )
        }

        val a = question.operandos[0]
        val b = question.operandos[1]
        val resultado = a + b

        // Geração de passos didáticos simples
        val passos = mutableListOf<String>()
        
        // Formatação seguindo o padrão centralizado
        val sA = formatar(a)
        val sB = formatar(b)
        val sRes = formatar(resultado)

        passos.add("Operação: Soma")
        passos.add("$sA + $sB = $sRes")
        
        // No futuro, aqui poderemos adicionar algoritmos de colunas (unidade, dezena...)
        // Para este protótipo, mantemos uma explicação direta.

        return MathSolution(
            resultadoFinal = resultado,
            passos = passos
        )
    }
}
