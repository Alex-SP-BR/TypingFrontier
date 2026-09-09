package com.typingfrontier.mental

object MathExerciseSelector {

    private var ultimoTipo: MathExerciseType? = null
    private val cicloAtual = mutableListOf<MathExerciseType>()

    fun escolher(atributo: Int): MathExerciseType {
        // 1. Obtenção dos Desbloqueados (Progressão Cumulativa com a nova estrutura de 13 categorias)
        val desbloqueados = obterDesbloqueados(atributo)

        // 2. Lógica de Ciclo Embaralhado com Pesos (Núcleo: 3, Intermediárias: 2, Avançadas: 1)
        if (cicloAtual.isEmpty()) {
            val poolComPesos = mutableListOf<MathExerciseType>()
            
            desbloqueados.forEach { tipo ->
                val peso = when (tipo) {
                    // NÚCLEO RÁPIDO
                    MathExerciseType.SOMA, MathExerciseType.SUBTRACAO, MathExerciseType.MULTIPLICACAO,
                    MathExerciseType.DIVISAO, MathExerciseType.RADICIACAO, MathExerciseType.EQUACAO -> 3
                    
                    // INTERMEDIÁRIAS
                    MathExerciseType.POTENCIACAO, MathExerciseType.SEQUENCIAS, MathExerciseType.OPERACOES_COMBINADAS -> 2
                    
                    // AVANÇADAS
                    MathExerciseType.PORCENTAGEM, MathExerciseType.DECIMAIS, MathExerciseType.FRACOES, MathExerciseType.PROBLEMAS -> 1
                    
                    else -> 1
                }
                repeat(peso) { poolComPesos.add(tipo) }
            }

            cicloAtual.addAll(poolComPesos.shuffled())
            
            // Proteção contra repetição imediata entre o fim de um ciclo e início do outro
            if (cicloAtual.size > 1 && cicloAtual[0] == ultimoTipo) {
                val repetido = cicloAtual.removeAt(0)
                cicloAtual.add(repetido)
            }
        }

        val escolhido = if (cicloAtual.isNotEmpty()) {
            cicloAtual.removeAt(0)
        } else {
            desbloqueados.random()
        }

        ultimoTipo = escolhido
        return escolhido
    }

    private fun obterDesbloqueados(n: Int): List<MathExerciseType> {
        val list = mutableListOf(MathExerciseType.SOMA)
        if (n >= 5) list.add(MathExerciseType.SUBTRACAO)
        if (n >= 10) list.add(MathExerciseType.MULTIPLICACAO)
        if (n >= 15) list.add(MathExerciseType.DIVISAO)
        if (n >= 25) list.add(MathExerciseType.RADICIACAO)
        if (n >= 35) list.add(MathExerciseType.EQUACAO)
        if (n >= 45) list.add(MathExerciseType.POTENCIACAO)
        if (n >= 60) list.add(MathExerciseType.SEQUENCIAS)
        if (n >= 75) list.add(MathExerciseType.OPERACOES_COMBINADAS)
        if (n >= 100) list.add(MathExerciseType.PORCENTAGEM)
        if (n >= 120) list.add(MathExerciseType.DECIMAIS)
        if (n >= 150) list.add(MathExerciseType.FRACOES)
        if (n >= 180) list.add(MathExerciseType.PROBLEMAS)
        return list
    }
}
