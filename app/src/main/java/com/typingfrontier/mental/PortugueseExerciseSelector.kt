package com.typingfrontier.mental

object PortugueseExerciseSelector {

    private var ultimoTipo: PortugueseExerciseType? = null

    fun escolher(atributo: Int): PortugueseExerciseType {

        val tipoFraco = PortuguesePerformance.tipoMaisFraco()

        if (tipoFraco != null && (1..100).random() <= 40) {
            ultimoTipo = tipoFraco
            return tipoFraco
        }

        val tiposPossiveis = when {

            atributo < 10 -> listOf(
                PortugueseExerciseType.SILABAS
            )

            atributo < 20 -> listOf(
                PortugueseExerciseType.SILABAS,
                PortugueseExerciseType.PLURAL,
                PortugueseExerciseType.GENERO
            )

            atributo < 40 -> listOf(
                PortugueseExerciseType.SUJEITO,
                PortugueseExerciseType.PLURAL,
                PortugueseExerciseType.GENERO
            )

            atributo < 60 -> listOf(
                PortugueseExerciseType.VERBO,
                PortugueseExerciseType.SUJEITO,
                PortugueseExerciseType.OBJETO_DIRETO
            )

            atributo < 80 -> listOf(
                PortugueseExerciseType.CLASSE_GRAMATICAL,
                PortugueseExerciseType.ACENTUACAO,
                PortugueseExerciseType.VERBO
            )

            else -> listOf(
                PortugueseExerciseType.INTERPRETACAO,
                PortugueseExerciseType.VOCABULARIO,
                PortugueseExerciseType.CLASSE_GRAMATICAL
            )
        }

        val filtrados = tiposPossiveis.filter { it != ultimoTipo }

        val escolhido = if (filtrados.isNotEmpty()) {
            filtrados.random()
        } else {
            tiposPossiveis.random() // fallback seguro
        }

        ultimoTipo = escolhido

        return escolhido
    }
}