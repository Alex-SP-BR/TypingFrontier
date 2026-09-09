package com.typingfrontier.mental

object MathGenerator {

    fun gerar(atributo: Int): MathQuestion {
        // Integração com o novo sistema de Ciclo e Desbloqueio Cumulativo
        val tipo = MathExerciseSelector.escolher(atributo)
        return gerarPorTipo(tipo, atributo)
    }

    private fun gerarPorTipo(tipo: MathExerciseType, atributo: Int): MathQuestion {

        // Característica: Números Negativos liberados a partir de inteligência 50
        val permitirNegativos = atributo >= 50

        return when (tipo) {

            MathExerciseType.SOMA -> {
                val a = if (permitirNegativos && (1..10).random() > 7) (-(5 + atributo/10)..(5 + atributo/10)).random() else (1..(10 + atributo/5)).random()
                val b = (1..(10 + atributo/5)).random()

                MathQuestion(
                    "${formatarSinal(a)} + ${formatarSinal(b)} = ?",
                    (a + b).toDouble(),
                    "Soma básica.",
                    1,
                    tipo,
                    listOf(a.toDouble(), b.toDouble()),
                    0
                )
            }

            MathExerciseType.SUBTRACAO -> {
                val a = (5..(20 + atributo/2)).random()
                val b = if (permitirNegativos && (1..10).random() > 6) (1..(a + 10)).random() else (1..a).random()

                MathQuestion(
                    "${formatarSinal(a)} - ${formatarSinal(b)} = ?",
                    (a - b).toDouble(),
                    "Subtração básica.",
                    5,
                    tipo,
                    listOf(a.toDouble(), b.toDouble()),
                    0
                )
            }

            MathExerciseType.MULTIPLICACAO -> {
                val baseRange = 2..(10 + atributo/20)
                val a = if (permitirNegativos && (1..10).random() > 8) (-(10)..(10)).random() else baseRange.random()
                val b = baseRange.random()

                MathQuestion(
                    "${formatarSinal(a)} x ${formatarSinal(b)} = ?",
                    (a * b).toDouble(),
                    "Multiplicação.",
                    10,
                    tipo,
                    listOf(a.toDouble(), b.toDouble()),
                    0
                )
            }

            MathExerciseType.DIVISAO -> {
                val divisor = (2..10).random()
                val tempRes = (2..(10 + atributo/20)).random()
                val res = if (permitirNegativos && (1..10).random() > 8) -tempRes else tempRes
                val a = divisor * res

                MathQuestion(
                    "${formatarSinal(a)} / ${formatarSinal(divisor)} = ?",
                    res.toDouble(),
                    "Divisão exata.",
                    15,
                    tipo,
                    listOf(a.toDouble(), divisor.toDouble()),
                    0
                )
            }

            MathExerciseType.RADICIACAO -> {
                val raiz = (2..(10 + atributo/50)).random()
                val radicando = raiz * raiz
                val resposta = raiz.toDouble()

                MathQuestion(
                    "√$radicando = ?",
                    resposta,
                    "$raiz x $raiz = $radicando\nPortanto, √$radicando = $raiz",
                    25,
                    tipo,
                    listOf(radicando.toDouble()),
                    0
                )
            }

            MathExerciseType.EQUACAO -> {
                // x + b = res -> x = res - b
                val x = if (permitirNegativos && (1..10).random() > 7) (-(10)..(20)).random() else (1..15).random()
                val b = (1..15).random()
                val total = x + b

                MathQuestion(
                    "x + ${formatarSinal(b)} = ${formatarSinal(total)}. Qual o valor de x?",
                    x.toDouble(),
                    "Equação simples.",
                    35,
                    tipo,
                    listOf(b.toDouble(), total.toDouble()),
                    0
                )
            }

            MathExerciseType.POTENCIACAO -> {
                val base = (2..10).random()
                val expoente = if (base > 5) (2..3).random() else (2..4).random()
                val resposta = Math.pow(base.toDouble(), expoente.toDouble())

                val expSimbolo = when (expoente) {
                    2 -> "²"
                    3 -> "³"
                    4 -> "⁴"
                    else -> "^$expoente"
                }

                val listaMultiplicacao = List(expoente) { base.toString() }
                val explicacaoStr = "${listaMultiplicacao.joinToString(" x ")} = ${if (resposta % 1 == 0.0) resposta.toInt().toString() else resposta.toString()}"

                MathQuestion(
                    "$base$expSimbolo = ?",
                    resposta,
                    explicacaoStr,
                    45,
                    tipo,
                    listOf(base.toDouble(), expoente.toDouble()),
                    0
                )
            }

            MathExerciseType.SEQUENCIAS -> {
                val a1 = (1..20).random()
                val r = listOf(-5, -4, -3, -2, 2, 3, 4, 5).random()
                
                val a2 = a1 + r
                val a3 = a2 + r
                val a4 = a3 + r
                val a5 = a4 + r
                
                val pergunta = "Complete a sequência:\n$a1, $a2, $a3, $a4, ?"
                val resposta = a5.toDouble()
                
                val acao = if (r > 0) "aumenta" else "diminui"
                val explicacaoStr = "Cada número $acao em ${Math.abs(r)}.\n$a4 + ($r) = $a5"

                MathQuestion(
                    pergunta,
                    resposta,
                    explicacaoStr,
                    60,
                    tipo,
                    listOf(a1.toDouble(), a2.toDouble(), a3.toDouble(), a4.toDouble()),
                    0
                )
            }

            MathExerciseType.OPERACOES_COMBINADAS -> {
                val a = (1..10).random()
                val b = (2..5).random()
                val c = (2..5).random()
                val op = (1..2).random()

                val (pergunta, resposta) = if (op == 1) {
                    val p = if (permitirNegativos && (1..10).random() > 8) "-$a + $b x $c" else "$a + $b x $c"
                    val r = if (p.startsWith("-")) -a + b * c else a + b * c
                    p to r
                } else {
                    "($a + $b) x $c" to ((a + b) * c)
                }

                MathQuestion(
                    "$pergunta = ?",
                    resposta.toDouble(),
                    "Respeite a precedência ou parênteses.",
                    75,
                    tipo,
                    listOf(a.toDouble(), b.toDouble(), c.toDouble()),
                    op
                )
            }

            MathExerciseType.PORCENTAGEM -> {
                val bases = listOf(10, 20, 25, 50)
                val perc = bases.random()
                val valor = when (perc) {
                    10 -> (1..20).random() * 10
                    20 -> (1..20).random() * 5
                    25 -> (1..20).random() * 4
                    else -> (1..20).random() * 2
                }
                val resposta = (valor * perc) / 100

                MathQuestion(
                    "$perc% de $valor = ?",
                    resposta.toDouble(),
                    "Cálculo de porcentagem simples.",
                    100,
                    tipo,
                    listOf(perc.toDouble(), valor.toDouble()),
                    0
                )
            }

            MathExerciseType.DECIMAIS -> {
                val op = (1..3).random()
                val (pergunta, resposta, operandosList) = when (op) {
                    1 -> { // Soma
                        val factor = 10.0
                        val a = (10..50).random() / factor
                        val b = (10..50).random() / factor
                        Triple("${formatarDecimal(a)} + ${formatarDecimal(b)}", a + b, listOf(a, b))
                    }
                    2 -> { // Subtração
                        val factor = 10.0
                        val aVal = (50..100).random()
                        val bVal = (10..(aVal - 5)).random()
                        val a = aVal / factor
                        val b = bVal / factor
                        Triple("${formatarDecimal(a)} - ${formatarDecimal(b)}", a - b, listOf(a, b))
                    }
                    else -> { // Multiplicação Simples
                        val a = (2..5).random().toDouble()
                        val b = (11..50).random() / 10.0
                        Triple("${a.toInt()} x ${formatarDecimal(b)}", a * b, listOf(a, b))
                    }
                }

                MathQuestion(
                    "$pergunta = ?",
                    resposta,
                    "Cálculo com números decimais.",
                    120,
                    tipo,
                    operandosList,
                    op
                )
            }

            MathExerciseType.FRACOES -> {
                val op = (0..1).random() // 0: Soma, 1: Subtração
                val den = listOf(2, 4, 5, 8, 10).random()
                
                val (numA, numB, resposta) = if (op == 0) {
                    // Soma: numA + numB < den para ser própria
                    val nA = (1 until den).random()
                    val nB = (1 until (den - nA + 1)).random().coerceAtMost(den - nA)
                    val res = (nA + nB).toDouble() / den
                    Triple(nA, nB, res)
                } else {
                    // Subtração: numA >= numB e numA < den
                    val nA = (1 until den).random()
                    val nB = (1..nA).random()
                    val res = (nA - nB).toDouble() / den
                    Triple(nA, nB, res)
                }

                val simbolo = if (op == 0) "+" else "-"
                val acao = if (op == 0) "somamos" else "subtraímos"
                val calc = if (op == 0) "$numA + $numB" else "$numA - $numB"
                val resNum = if (op == 0) numA + numB else numA - numB

                val explicacaoStr = "$numA/$den $simbolo $numB/$den\n" +
                        "Como os denominadores são iguais, $acao os numeradores:\n" +
                        "($calc) / $den = $resNum/$den\n" +
                        "$resNum ÷ $den = ${if (resposta % 1 == 0.0) resposta.toInt().toString() else resposta.toString().replace(".", ",")}"

                MathQuestion(
                    "$numA/$den $simbolo $numB/$den = ?",
                    resposta,
                    explicacaoStr,
                    150,
                    tipo,
                    listOf(numA.toDouble(), den.toDouble(), numB.toDouble(), den.toDouble()),
                    op
                )
            }

            MathExerciseType.PROBLEMAS -> {
                val op = (0..3).random()
                val a: Int
                val b: Int
                val pergunta: String
                val resposta: Double

                when (op) {
                    0 -> { // Soma
                        a = (10..50).random()
                        b = (5..30).random()
                        resposta = (a + b).toDouble()
                        pergunta = "Você recebeu $a Frons por uma missão e ganhou mais $b Frons. Quanto tem agora?"
                    }
                    1 -> { // Subtração
                        a = (30..100).random()
                        b = (5..a).random()
                        resposta = (a - b).toDouble()
                        pergunta = "Você tinha $a Frons e gastou $b Frons em um item. Quantos Frons sobraram?"
                    }
                    2 -> { // Multiplicação
                        a = (5..20).random() // Preço
                        b = (2..6).random()  // Quantidade
                        resposta = (a * b).toDouble()
                        pergunta = "Cada poção custa $a Frons. Se você comprar $b poções, quanto gastará?"
                    }
                    else -> { // Divisão
                        b = listOf(2, 3, 4, 5, 10).random() // Divisor
                        val tempRes = (5..20).random()
                        a = b * tempRes // Garante divisão exata
                        resposta = tempRes.toDouble()
                        pergunta = "Uma recompensa de $a Frons será dividida entre $b pessoas. Quanto cada um recebe?"
                    }
                }

                MathQuestion(
                    pergunta,
                    resposta,
                    "Problema narrativo simples.",
                    180,
                    tipo,
                    listOf(a.toDouble(), b.toDouble()),
                    op
                )
            }
        }
    }

    private fun formatarDecimal(v: Double): String {
        val s = if (v % 1 == 0.0) v.toInt().toString() else v.toString()
        return s.replace(".", ",")
    }

    private fun formatarSinal(n: Int): String {
        return if (n < 0) "($n)" else n.toString()
    }
}