package com.typingfrontier.mental

import java.util.Locale

/**
 * Interface comum para os resolvedores de categorias matemáticas.
 */
interface CategorySolver {
    fun solve(question: MathQuestion): MathSolution

    /**
     * Formata o valor Double seguindo a convenção do projeto:
     * - Remove .0 se for inteiro.
     * - Substitui ponto por vírgula.
     * - Limita a representação visual para evitar ruídos de ponto flutuante.
     */
    fun formatar(v: Double): String {
        if (v % 1 == 0.0) return v.toLong().toString()

        // Usa String.format com Locale.US para garantir ponto decimal antes da substituição por vírgula
        // Resolve o warning de Locale e garante consistência.
        val s = String.format(Locale.US, "%.2f", v)
        val formatted = if (s.contains(".")) {
            s.replace(Regex("0*$"), "").replace(Regex("\\.$"), "")
        } else {
            s
        }
        return formatted.replace(".", ",")
    }
}
