package com.typingfrontier.mental

import com.typingfrontier.mental.PortugueseExerciseType

data class PortugueseQuestion(
    val pergunta: String,
    val respostaCorreta: String,
    val explicacao: String,
    val nivelMin: Int,
    val tipo: PortugueseExerciseType  // ✅ adicionado
)