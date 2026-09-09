package com.typingfrontier.mental

/**
 * Resolvedor específico para a categoria SUBTRACAO.
 */
object SubtractionSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.SUBTRACAO) {
            throw IllegalArgumentException("O SubtractionSolver suporta apenas a categoria SUBTRACAO.")
        }

        if (question.operandos.size < 2) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar a subtração.")
            )
        }

        val a = question.operandos[0]
        val b = question.operandos[1]
        val resultado = a - b

        // Geração de passos didáticos simples
        val passos = mutableListOf<String>()
        
        // Formatação seguindo o padrão centralizado
        val sA = formatar(a)
        val sB = formatar(b)
        val sRes = formatar(resultado)

        passos.add("Operação: Subtração")
        passos.add("$sA - $sB = $sRes")
        
        // Lógica Pedagógica Simples: Identificação de Empréstimo ou Negativo
        if (a % 1 == 0.0 && b % 1 == 0.0 && a >= 0 && b >= 0 && a >= 10 && b >= 1) {
            val uA = a.toInt() % 10
            val uB = b.toInt() % 10
            if (uA < uB && a > b) {
                passos.add("\nDica: Como $uA é menor que $uB, na conta armada precisaríamos pedir emprestado da dezena.")
            }
        }
        
        if (resultado < 0) {
            passos.add("\nNota: O resultado é negativo porque subtraímos um valor maior de um menor.")
        }

        return MathSolution(
            resultadoFinal = resultado,
            passos = passos
        )
    }
}
