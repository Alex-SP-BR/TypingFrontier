package com.typingfrontier.mental

import kotlin.math.pow

/**
 * Resolvedor específico para a categoria POTENCIACAO.
 * Explica a potência através da multiplicação repetida da base.
 */
object PotenciacaoSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.POTENCIACAO) {
            throw IllegalArgumentException("O PotenciacaoSolver suporta apenas a categoria POTENCIACAO.")
        }

        if (question.operandos.size < 2) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar a potenciação.")
            )
        }

        val base = question.operandos[0]
        val expoente = question.operandos[1]
        val resultado = base.pow(expoente)

        val passos = mutableListOf<String>()
        passos.add("Operação: Potenciação")

        val sBase = formatar(base)
        val nExp = expoente.toInt()
        val expSimbolo = when (nExp) {
            2 -> "²"
            3 -> "³"
            4 -> "⁴"
            else -> "^$nExp"
        }

        // 1. Apresentação da multiplicação repetida
        val listaMultiplicacao = List(nExp) { sBase }
        passos.add("$sBase$expSimbolo = ${listaMultiplicacao.joinToString(" x ")}")

        // 2. Detalhamento dos cálculos intermediários
        if (nExp == 2) {
            passos.add("$sBase x $sBase = ${formatar(resultado)}")
        } else if (nExp > 2) {
            var acumulado = base
            for (i in 2..nExp) {
                val anterior = acumulado
                acumulado *= base
                passos.add("${formatar(anterior)} x $sBase = ${formatar(acumulado)}")
            }
        }

        return MathSolution(
            resultadoFinal = resultado,
            passos = passos
        )
    }
}
