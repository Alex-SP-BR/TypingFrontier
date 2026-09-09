package com.typingfrontier.mental

data class MathQuestion(
    val pergunta: String,
    val respostaCorreta: Double,
    val explicacao: String,
    val nivelMin: Int,
    val tipo: MathExerciseType,
    val operandos: List<Double> = emptyList(),
    val subTipo: Int = 0
)