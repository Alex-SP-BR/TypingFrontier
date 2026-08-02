package com.typingfrontier.mental

data class MathQuestion(
    val pergunta: String,
    val respostaCorreta: Int,
    val explicacao: String,
    val nivelMin: Int,
    val tipo: MathExerciseType
)