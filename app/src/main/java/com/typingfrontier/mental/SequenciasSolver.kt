package com.typingfrontier.mental

/**
 * Resolvedor específico para a categoria SEQUENCIAS.
 * Atualmente foca em Progressões Aritméticas (P.A.) crescentes e decrescentes.
 */
object SequenciasSolver : CategorySolver {

    override fun solve(question: MathQuestion): MathSolution {
        if (question.tipo != MathExerciseType.SEQUENCIAS) {
            throw IllegalArgumentException("O SequenciasSolver suporta apenas a categoria SEQUENCIAS.")
        }

        if (question.operandos.size < 4) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Dados insuficientes para detalhar a sequência.")
            )
        }

        val a1 = question.operandos[0]
        val a2 = question.operandos[1]
        val a3 = question.operandos[2]
        val a4 = question.operandos[3]

        // Validação defensiva de finitude
        if (!a1.isFinite() || !a2.isFinite() || !a3.isFinite() || !a4.isFinite()) {
            return MathSolution(
                resultadoFinal = question.respostaCorreta,
                passos = listOf("Erro: Valores da sequência inválidos.")
            )
        }

        val passos = mutableListOf<String>()
        passos.add("Operação: Sequência")

        when (question.subTipo) {
            0 -> {
                // Progressão Aritmética (P.A.)
                val r = a2 - a1
                
                // Valida se os termos seguintes seguem a mesma razão (P.A. válida)
                if (a3 - a2 != r || a4 - a3 != r) {
                    return MathSolution(
                        resultadoFinal = question.respostaCorreta,
                        passos = listOf("A sequência fornecida não é uma Progressão Aritmética válida.")
                    )
                }

                val resultado = a4 + r
                
                val sA1 = formatar(a1)
                val sA2 = formatar(a2)
                val sA3 = formatar(a3)
                val sA4 = formatar(a4)
                val sR = formatar(r)
                val sRes = formatar(resultado)

                passos.add("$sA1, $sA2, $sA3, $sA4, ?")
                
                if (r > 0) {
                    passos.add("Cada número aumenta em $sR.")
                } else {
                    passos.add("Cada número diminui em ${formatar(Math.abs(r))}.")
                }
                
                val simbolo = if (r >= 0) "+" else "-"
                val rAbs = formatar(Math.abs(r))
                passos.add("$sA4 $simbolo $rAbs = $sRes")

                return MathSolution(
                    resultadoFinal = resultado,
                    passos = passos
                )
            }
            else -> {
                return MathSolution(
                    resultadoFinal = question.respostaCorreta,
                    passos = listOf("Tipo de sequência não reconhecido (Subtipo: ${question.subTipo}).")
                )
            }
        }
    }
}
