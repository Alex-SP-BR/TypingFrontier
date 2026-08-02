package com.typingfrontier.npc

object NpcDialogos {

    fun falaInicial(npc: NpcVendedor): String {
        return when (npc.personalidade) {

            PersonalidadeNpc.CALMO ->
                "${npc.nome}: Fique à vontade. Trabalho sério exige boas ferramentas."

            PersonalidadeNpc.DESCONFIADO ->
                "${npc.nome}: Não vendo pra qualquer um… você parece saber o que faz."

            PersonalidadeNpc.AMIGAVEL ->
                "${npc.nome}: Ah, um colega detetive! Tenho coisas boas hoje."

            PersonalidadeNpc.IRONICO ->
                "${npc.nome}: Investigação? Espero que saiba no que está se metendo."

            PersonalidadeNpc.MISTERIOSO ->
                "${npc.nome}: Algumas ferramentas não fazem perguntas…"
        }
    }

    fun falaCompra(npc: NpcVendedor): String {
        return when (npc.personalidade) {

            PersonalidadeNpc.CALMO ->
                "${npc.nome}: Boa escolha. Isso vai te poupar tempo."

            PersonalidadeNpc.DESCONFIADO ->
                "${npc.nome}: Espero que saiba usar isso direito."

            PersonalidadeNpc.AMIGAVEL ->
                "${npc.nome}: Vai te ajudar bastante, confia!"

            PersonalidadeNpc.IRONICO ->
                "${npc.nome}: Se isso não resolver, nada resolve."

            PersonalidadeNpc.MISTERIOSO ->
                "${npc.nome}: Nem tudo que se compra deve ser explicado."
        }
    }
}