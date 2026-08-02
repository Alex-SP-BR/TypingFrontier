package com.typingfrontier.npc

import com.typingfrontier.economy.Equipment

object NpcRepository {

    private val vendedores = listOf(

        NpcVendedor(
            nome = "Carlos, o Investigador",
            cidade = "São Paulo",
            personalidade = PersonalidadeNpc.CALMO,
            estoque = listOf(

                Equipment(
                    id = "lupa_basica",
                    nome = "Lupa Básica",
                    preco = 50,
                    atributoAlvo = "VELOCIDADE",
                    bonus = 5,
                    descricao = "Ajuda a identificar pistas simples."
                ),

                Equipment(
                    id = "gravador_simples",
                    nome = "Gravador Simples",
                    preco = 80,
                    atributoAlvo = "INTELIGENCIA",
                    bonus = 5,
                    descricao = "Registra depoimentos importantes."
                ),

                Equipment(
                    id = "colete_leve",
                    nome = "Colete Leve",
                    preco = 180,
                    atributoAlvo = "RESISTENCIA",
                    bonus = 10,
                    descricao = "Reduz o impacto de ataques."
                )
            )
        )
    )

    fun getVendedorDaCidade(cidade: String): NpcVendedor? {
        return vendedores.find { it.cidade == cidade }
    }
}
