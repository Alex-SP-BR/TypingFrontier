package com.typingfrontier.collection

import androidx.annotation.DrawableRes

enum class CollectionCategory {
    NIVEL,
    MATEMATICA,
    PORTUGUES,
    TREINO_MENTAL,
    TREINO_FISICO,
    VELOCIDADE,
    FORCA,
    RESISTENCIA,
    EXPLORACAO,
    ECONOMIA,
    ESPECIAL
}

data class Avatar(
    val id: String,
    val nome: String,
    val sexo: String, // "Masculino" ou "Feminino"
    val categoria: CollectionCategory,
    @DrawableRes val imagemRes: Int,
    val nivelRequisito: Int,
    val adsNecessarios: Int
)

data class Achievement(
    val id: String,
    val nome: String,
    val descricao: String,
    val categoria: CollectionCategory,
    val requisito: String,
    @androidx.annotation.DrawableRes val insigniaRes: Int,
    val recompensaDinheiro: Int = 0,
    val avatarAssociadoId: String? = null
)
