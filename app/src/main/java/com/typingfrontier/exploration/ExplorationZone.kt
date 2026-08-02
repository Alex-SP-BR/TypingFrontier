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
    val ambiente: String = "no local"
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
            recompensaBaseXp = 15,
            recompensaBaseDinheiro = 45,
            chanceItemRaro = 1,
            ambiente = "na praça"
        ),
        ExplorationZone(
            id = "centro",
            nome = "🏙️ Centro Comercial",
            descricao = "Movimentado e imprevisível. Requer boa comunicação e atenção.",
            nivelMinimo = 5,
            riscoBase = 20,
            atributoPrincipal = "CARISMA",
            atributoSecundario = "FORCA",
            recompensaBaseXp = 40,
            recompensaBaseDinheiro = 90,
            chanceItemRaro = 3,
            ambiente = "nas lojas"
        ),
        ExplorationZone(
            id = "suburbio",
            nome = "🏘️ Subúrbio Industrial",
            descricao = "Área rústica e perigosa. Muitos perigos físicos aguardam os descuidados.",
            nivelMinimo = 10,
            riscoBase = 45,
            atributoPrincipal = "INTELIGENCIA",
            atributoSecundario = "RESISTENCIA",
            recompensaBaseXp = 80,
            recompensaBaseDinheiro = 180,
            chanceItemRaro = 6,
            ambiente = "nas fábricas"
        ),
        ExplorationZone(
            id = "beco",
            nome = "🌑 Beco Escuro",
            descricao = "Somente os mais corajosos ou tolos entram aqui. Risco altíssimo, tesouros raros.",
            nivelMinimo = 15,
            riscoBase = 70,
            atributoPrincipal = "INTELIGENCIA",
            atributoSecundario = "FORCA",
            recompensaBaseXp = 180,
            recompensaBaseDinheiro = 350,
            chanceItemRaro = 10,
            ambiente = "nas sombras"
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
            recompensaBaseXp = 350,
            recompensaBaseDinheiro = 600,
            chanceItemRaro = 15,
            ambiente = "nas bancadas"
        ),
        ExplorationZone(
            id = "cassino",
            nome = "🎭 Cassino Clandestino",
            descricao = "Onde a lábia vale mais que o ouro. Um erro pode ser fatal.",
            nivelMinimo = 30,
            riscoBase = 100,
            atributoPrincipal = "CARISMA",
            atributoSecundario = "FORCA",
            atributoTerciario = "VELOCIDADE",
            recompensaBaseXp = 500,
            recompensaBaseDinheiro = 1100,
            chanceItemRaro = 20,
            ambiente = "entre as mesas"
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
            recompensaBaseXp = 1200,
            recompensaBaseDinheiro = 1800,
            chanceItemRaro = 25,
            ambiente = "nos túneis"
        )
    )

    fun getZona(id: String) = zonas.find { it.id == id }
}
