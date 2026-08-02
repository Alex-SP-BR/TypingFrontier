package com.typingfrontier.mental

object MathGenerator {

    fun gerar(atributo: Int): MathQuestion {

        val tipoFraco = MathPerformance.tipoMaisFraco()

        val tipo = if (tipoFraco != null && (1..100).random() <= 40) {
            tipoFraco
        } else {
            escolherPorNivel(atributo)
        }

        return gerarPorTipo(tipo)
    }

    private fun escolherPorNivel(atributo: Int): MathExerciseType {
        return when {
            atributo < 10 -> MathExerciseType.SOMA
            atributo < 20 -> MathExerciseType.SUBTRACAO
            atributo < 30 -> MathExerciseType.MULTIPLICACAO
            atributo < 40 -> MathExerciseType.DIVISAO
            else -> MathExerciseType.EQUACAO
        }
    }

    private fun gerarPorTipo(tipo: MathExerciseType): MathQuestion {

        return when (tipo) {

            MathExerciseType.SOMA -> {
                val a = (1..10).random()
                val b = (1..10).random()

                MathQuestion(
                    "$a + $b = ?",
                    a + b,
                    "Soma básica.",
                    1,
                    tipo
                )
            }

            MathExerciseType.SUBTRACAO -> {
                val a = (5..20).random()
                val b = (1..a).random()

                MathQuestion(
                    "$a - $b = ?",
                    a - b,
                    "Subtração básica.",
                    10,
                    tipo
                )
            }

            MathExerciseType.MULTIPLICACAO -> {
                val a = (2..10).random()
                val b = (2..10).random()

                MathQuestion(
                    "$a x $b = ?",
                    a * b,
                    "Multiplicação.",
                    20,
                    tipo
                )
            }

            MathExerciseType.DIVISAO -> {
                val b = (2..10).random()
                val resultado = (2..10).random()
                val a = b * resultado

                MathQuestion(
                    "$a / $b = ?",
                    resultado,
                    "Divisão exata.",
                    30,
                    tipo
                )
            }

            MathExerciseType.EQUACAO -> {
                val x = (1..10).random()
                val b = (1..10).random()
                val resultado = x + b

                MathQuestion(
                    "x + $b = $resultado. Qual o valor de x?",
                    x,
                    "Equação simples.",
                    40,
                    tipo
                )
            }
        }
    }
}