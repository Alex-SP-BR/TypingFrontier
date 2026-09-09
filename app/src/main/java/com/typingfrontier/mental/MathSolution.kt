package com.typingfrontier.mental

/**
 * Representa a resolução estruturada de uma questão matemática.
 */
data class MathSolution(
    val resultadoFinal: Double,
    val passos: List<String>
)
