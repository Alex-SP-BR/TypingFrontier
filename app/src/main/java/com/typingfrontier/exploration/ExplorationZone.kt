package com.typingfrontier.exploration

data class ExplorationZone(
    val id: String,
    val nome: String,
    val descricao: String,
    val nivelMinimo: Int,
    val riscoBase: Int, // 0 a 100
    val atributoPrincipal: String,
    val atributoSecundario: String? = null,
    val atributoTerciario: String? = null,
    val recompensaBaseXp: Int,
    val recompensaBaseDinheiro: Int,
    val chanceItemRaro: Int, // 1 a 100
    val ambiente: String = "no local",
    val regiao: String = "sao_paulo"
) {
    // Propriedade de compatibilidade para evitar erros em outras partes do código
    val atributoFoco: String get() = atributoPrincipal
}

object ExplorationZoneRepository {
    val zonas = listOf(
        ExplorationZone(
            id = "parque",
            nome = "🌳 Parque da Cidade",
            descricao = "Um local tranquilo, ideal para iniciantes. Pouco risco, mas pouca recompensa.",
            nivelMinimo = 1,
            riscoBase = 5,
            atributoPrincipal = "INTELIGENCIA",
            atributoSecundario = "VELOCIDADE",
            recompensaBaseXp = 11,
            recompensaBaseDinheiro = 33,
            chanceItemRaro = 1,
            ambiente = "na praça",
            regiao = "sao_paulo"
        ),
        ExplorationZone(
            id = "centro",
            nome = "🏙️ Centro Comercial",
            descricao = "Movimentado e imprevisível. Requer boa comunicação e atenção.",
            nivelMinimo = 5,
            riscoBase = 20,
            atributoPrincipal = "CARISMA",
            atributoSecundario = "FORCA",
            recompensaBaseXp = 30,
            recompensaBaseDinheiro = 67,
            chanceItemRaro = 3,
            ambiente = "nas lojas",
            regiao = "sao_paulo"
        ),
        ExplorationZone(
            id = "suburbio",
            nome = "🏘️ Subúrbio Industrial",
            descricao = "Área rústica e perigosa. Muitos perigos físicos aguardam os descuidados.",
            nivelMinimo = 10,
            riscoBase = 45,
            atributoPrincipal = "INTELIGENCIA",
            atributoSecundario = "RESISTENCIA",
            recompensaBaseXp = 60,
            recompensaBaseDinheiro = 135,
            chanceItemRaro = 6,
            ambiente = "nas fábricas",
            regiao = "sao_paulo"
        ),
        ExplorationZone(
            id = "beco",
            nome = "🌑 Beco Escuro",
            descricao = "Somente os mais corajosos ou tolos entram aqui. Risco altíssimo, tesouros raros.",
            nivelMinimo = 15,
            riscoBase = 70,
            atributoPrincipal = "INTELIGENCIA",
            atributoSecundario = "FORCA",
            recompensaBaseXp = 135,
            recompensaBaseDinheiro = 262,
            chanceItemRaro = 10,
            ambiente = "nas sombras",
            regiao = "sao_paulo"
        ),
        ExplorationZone(
            id = "laboratorio",
            nome = "🧪 Lab Abandonado",
            descricao = "Gases tóxicos e segredos científicos. Exige alta capacidade mental.",
            nivelMinimo = 20,
            riscoBase = 85,
            atributoPrincipal = "INTELIGENCIA",
            atributoSecundario = "RESISTENCIA",
            atributoTerciario = "VELOCIDADE",
            recompensaBaseXp = 262,
            recompensaBaseDinheiro = 450,
            chanceItemRaro = 15,
            ambiente = "nas bancadas",
            regiao = "sao_paulo"
        ),
        ExplorationZone(
            id = "cassino",
            nome = "🎭 Cassino Clandestino",
            descricao = "Onde a lábia vale mais que o ouro. Um erro pode ser desastroso.",
            nivelMinimo = 30,
            riscoBase = 100,
            atributoPrincipal = "CARISMA",
            atributoSecundario = "FORCA",
            atributoTerciario = "VELOCIDADE",
            recompensaBaseXp = 375,
            recompensaBaseDinheiro = 825,
            chanceItemRaro = 20,
            ambiente = "entre as mesas",
            regiao = "sao_paulo"
        ),
        ExplorationZone(
            id = "esgotos",
            nome = "🐀 Esgotos Profundos",
            descricao = "O verdadeiro pesadelo. Poucos voltaram para contar a história.",
            nivelMinimo = 45,
            riscoBase = 120,
            atributoPrincipal = "INTELIGENCIA",
            atributoSecundario = "VELOCIDADE",
            atributoTerciario = "RESISTENCIA",
            recompensaBaseXp = 900,
            recompensaBaseDinheiro = 1350,
            chanceItemRaro = 25,
            ambiente = "nos túneis",
            regiao = "sao_paulo"
        ),
        ExplorationZone(
            id = "sp_norte_investigacao",
            nome = "Incidente na Ponte Estaiada",
            descricao = "Um acidente interrompeu o movimento na região da Ponte Estaiada. Há feridos, testemunhas com relatos diferentes e sinais de que o ocorrido pode não ter sido tão simples quanto parece.",
            nivelMinimo = 55,
            riscoBase = 130,
            atributoPrincipal = "INTELIGENCIA",
            atributoSecundario = "VELOCIDADE",
            atributoTerciario = "RESISTENCIA",
            recompensaBaseXp = 1200,
            recompensaBaseDinheiro = 1800,
            chanceItemRaro = 30,
            ambiente = "na Ponte Estaiada",
            regiao = "sao_paulo_norte"
        ),
        ExplorationZone(
            id = "rio_construcao",
            nome = "🚄 São Paulo e Rio",
            descricao = "[ EM CONSTRUÇÃO ] — Esta conexão será disponibilizada em uma futura expansão.",
            nivelMinimo = 1,
            riscoBase = 0,
            atributoPrincipal = "INTELIGENCIA",
            recompensaBaseXp = 0,
            recompensaBaseDinheiro = 0,
            chanceItemRaro = 0,
            regiao = "sao_paulo"
        )
    )

    fun getZona(id: String) = zonas.find { it.id == id }

    fun getZonasPorRegiao(regiao: String) = zonas.filter { it.regiao == regiao }
}
