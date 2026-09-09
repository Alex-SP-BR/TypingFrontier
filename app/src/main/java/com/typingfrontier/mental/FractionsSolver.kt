package com.typingfrontier.mental

import kotlin.math.abs

/**
 * Resolvedor específico para a categoria FRACOES.
 * Atualmente foca em soma e subtração de frações com denominadores iguais.
 */
object FractionsSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.FRACOES) {
            throw IllegalArgumentException("O FractionsSolver suporta apenas a categoria FRACOES.")
        }

        if (question.operandos.size < 4) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar a operação com frações.")
            )
        }

        val numA = question.operandos[0]
        val denA = question.operandos[1]
        val numB = question.operandos[2]
        val denB = question.operandos[3]

        // Validação defensiva
        if (denA == 0.0 || denB == 0.0 || !numA.isFinite() || !denA.isFinite() || !numB.isFinite() || !denB.isFinite()) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Erro: Operandos inválidos ou denominador zero.")
            )
        }

        val passos = mutableListOf<String>()
        passos.add("Operação: Frações")

        val subTipo = question.subTipo
        val resultadoFinal: Double

        // Representação visual inicial
        val simbolo = if (subTipo == 0) "+" else "-"
        passos.add("${formatar(numA)}/${formatar(denA)} $simbolo ${formatar(numB)}/${formatar(denB)}")

        if (denA == denB) {
            val acao = if (subTipo == 0) "somamos" else "subtraímos"
            passos.add("Como os denominadores são iguais, $acao os numeradores:")

            val resNum = if (subTipo == 0) numA + numB else numA - numB
            val resDen = denA
            resultadoFinal = resNum / resDen

            val calcTxt = if (subTipo == 0) "${formatar(numA)} + ${formatar(numB)}" else "${formatar(numA)} - ${formatar(numB)}"
            passos.add("($calcTxt) / ${formatar(denA)} = ${formatar(resNum)}/${formatar(resDen)}")

            // Simplificação para a explicação (opcional se resNum/resDen já for simples)
            val mdc = calcularMDC(resNum.toLong(), resDen.toLong())
            if (mdc > 1) {
                val simNum = resNum / mdc
                val simDen = resDen / mdc
                passos.add("Simplificando: ${formatar(resNum)}/${formatar(resDen)} = ${formatar(simNum)}/${formatar(simDen)}")
            }

            // Conversão final
            passos.add("${formatar(resNum)} ÷ ${formatar(resDen)} = ${formatar(resultadoFinal)}")

        } else {
            // Caso para expansão futura (denominadores diferentes)
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Operação com denominadores diferentes ainda não suportada pelo Solver.")
            )
        }

        return MathSolution(
            resultadoFinal = resultadoFinal,
            passos = passos
        )
    }

    /**
     * Calcula o Máximo Divisor Comum (MDC) para simplificação visual.
     */
    private fun calcularMDC(a: Long, b: Long): Long {
        var n1 = abs(a)
        var n2 = abs(b)
        while (n2 != 0L) {
            val temp = n1 % n2
            n1 = n2
            n2 = temp
        }
        return n1
    }
}
