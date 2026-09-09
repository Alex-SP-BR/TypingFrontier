package com.typingfrontier.mental

import kotlin.math.sqrt

/**
 * Resolvedor específico para a categoria RADICIACAO.
 * Atualmente foca em raízes quadradas exatas.
 */
object RadiciacaoSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.RADICIACAO) {
            throw IllegalArgumentException("O RadiciacaoSolver suporta apenas a categoria RADICIACAO.")
        }

        if (question.operandos.isEmpty()) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar a radiciação.")
            )
        }

        val radicando = question.operandos[0]

        // Tratamento defensivo para valores inválidos para raiz quadrada real
        if (radicando < 0 || !radicando.isFinite()) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Cálculo impossível com os dados fornecidos.")
            )
        }

        val raiz = sqrt(radicando)
        val passos = mutableListOf<String>()

        val sRad = formatar(radicando)
        val sRaiz = formatar(raiz)

        passos.add("Operação: Radiciação")
        passos.add("√$sRad = $sRaiz")
        passos.add("Porque $sRaiz x $sRaiz = $sRad")

        return MathSolution(
            resultadoFinal = raiz,
            passos = passos
        )
    }
}
