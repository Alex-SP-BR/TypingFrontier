package com.typingfrontier.mental

object ErrorMemory {

    val errosMatematica = mutableListOf<MathQuestion>()
    val errosPortugues = mutableListOf<PortugueseQuestion>()

    fun registrarErroMath(q: MathQuestion) {
        if (!errosMatematica.contains(q)) {
            errosMatematica.add(q)
        }
    }

    fun registrarErroPortugues(q: PortugueseQuestion) {
        if (!errosPortugues.contains(q)) {
            errosPortugues.add(q)
        }
    }

    fun pegarErroMath(): MathQuestion? {
        if (errosMatematica.isEmpty()) return null
        return if ((1..100).random() <= 30) {
            errosMatematica.random()
        } else null
    }

    fun pegarErroPortugues(): PortugueseQuestion? {
        if (errosPortugues.isEmpty()) return null
        return if ((1..100).random() <= 30) {
            errosPortugues.random()
        } else null
    }

    fun acertouMath(q: MathQuestion) {
        errosMatematica.remove(q)
    }

    fun acertouPortugues(q: PortugueseQuestion) {
        errosPortugues.remove(q)
    }
}