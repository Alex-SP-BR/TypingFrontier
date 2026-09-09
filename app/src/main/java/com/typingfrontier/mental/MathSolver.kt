package com.typingfrontier.mental

/**
 * Fachada central para resolução de questões matemáticas.
 * Coordena qual resolvedor específico deve ser utilizado.
 */
object MathSolver {

    fun solve(question: MathQuestion): MathSolution? {
        return when (question.tipo) {
            MathExerciseType.SOMA -> AdditionSolver.solve(question)
            MathExerciseType.SUBTRACAO -> SubtractionSolver.solve(question)
            MathExerciseType.MULTIPLICACAO -> MultiplicationSolver.solve(question)
            MathExerciseType.DIVISAO -> DivisionSolver.solve(question)
            MathExerciseType.RADICIACAO -> RadiciacaoSolver.solve(question)
            MathExerciseType.EQUACAO -> EquationSolver.solve(question)
            MathExerciseType.POTENCIACAO -> PotenciacaoSolver.solve(question)
            MathExerciseType.SEQUENCIAS -> SequenciasSolver.solve(question)
            MathExerciseType.OPERACOES_COMBINADAS -> CombinedOperationsSolver.solve(question)
            MathExerciseType.PORCENTAGEM -> PercentageSolver.solve(question)
            MathExerciseType.DECIMAIS -> DecimalSolver.solve(question)
            MathExerciseType.FRACOES -> FractionsSolver.solve(question)
            MathExerciseType.PROBLEMAS -> ProblemasSolver.solve(question)
            // Outras categorias ainda não possuem solver real.
            else -> null
        }
    }
}
