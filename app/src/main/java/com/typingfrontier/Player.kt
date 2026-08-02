package com.typingfrontier

data class Player(
    var nome: String = "",
    var sexo: String = "",
    var profissao: String = "",
    var cidadeNascimento: String = "São Paulo",

    // 🔹 PROGRESSÃO
    var nivel: Int = 1,
    var experienciaAtual: Int = 0,
    var experienciaParaProximoNivel: Int = 20,

    // ❤️ VIDA (RPG REAL)
    var vida: Int = 100,
    var vidaMax: Int = 100,

    // 🔹 STATUS BASE
    var energia: Int = 100,
    var energiaMax: Int = 100,

    var reputacao: Int = 0,

    // 🧠 MEMÓRIA DO JOGO
    var ajudouAlguem: Boolean = false,
    var falhouMuito: Boolean = false,
    var entrouEmConfusao: Boolean = false,

    var eventosPositivos: Int = 0,
    var eventosNegativos: Int = 0,

    // 🔗 HISTÓRIA ENCADEADA
    var eventoEncadeadoId: String? = null,
    var etapaEvento: Int = 0,

    // 🔗 HISTÓRIAS PARALELAS
    var historiasAtivas: MutableMap<String, Int> = mutableMapOf(),

    // 🔥 DECISÃO DO JOGADOR
    var escolhaAnterior: String? = null,

    // 💰 ECONOMIA
    var dinheiro: Int = 100,

    // 🔹 ATRIBUTOS
    var forca: Int = 1,
    var velocidade: Int = 1,
    var resistencia: Int = 1,
    var inteligencia: Int = 1,
    var carisma: Int = 1,

    // 🔹 ESPECIALIZAÇÃO
    var especializacaoPortugues: EspecializacaoPortugues? = null,

    // 🧠 ENERGIA MENTAL (Internamente cansacoMental)
    var cansacoMental: Int = 0,
    var cansacoMax: Int = 50, // Reduzido de 100 para 50 como base (Concentração mais limitada)

    // 📊 PROGRESSO DE ATRIBUTOS
    var progressoInteligencia: Int = 0,
    var progressoCarisma: Int = 0,
    var progressoForca: Int = 0,
    var progressoResistencia: Int = 0,
    var progressoVelocidade: Int = 0,
    
    var progressoInteligenciaMax: Int = 100,
    var progressoCarismaMax: Int = 100,
    var progressoForcaMax: Int = 100,
    var progressoResistenciaMax: Int = 100,
    var progressoVelocidadeMax: Int = 100,

    // 🎒 EQUIPAMENTOS (ID do equipamento equipado)
    var equipamentoId: String? = null,

    // 🕊️ TIBIA SYSTEM: BLESSINGS (Proteção contra morte)
    var temBlessing: Boolean = false,

    // 🎬 CINEMATIC INTRO
    var introConcluida: Boolean = false,

    // ⚡ RESTRIÇÃO DIÁRIA
    var trabalhouHoje: Boolean = false,
    var pausouHoje: Boolean = false, // Nova restrição diária

    // ⏰ TEMPO
    var dia: Int = 1,
    var hora: Int = 8,
    var minuto: Int = 0,

    // 🩹 SISTEMA DE TRAUMAS
    var traumasAcumulados: Int = 0,
    var diasParaRecuperarTrauma: Int = 0

) {

    // ------------------------------------------------
    // 🔹 PROPRIEDADES CALCULADAS (RPG)
    // ------------------------------------------------

    /**
     * Define quantas hospitalizações o corpo suporta antes de um colapso (Estado Crítico).
     * Fórmula: Base 2 + (Vida/300) + (Resistencia/25).
     * Jogador Lvl 1: Limite 2.
     * Policial Lvl 50 (~600 HP, 40 Res): Limite 5.
     */
    val limiteTraumas: Int
        get() = 2 + (vidaMax / 300) + (resistencia / 25)

    val defesa: Int
        get() {
            val equip = com.typingfrontier.economy.ProfessionManager.getEquipment(equipamentoId)
            val bonusDefesa = if (equip?.atributoAlvo == "RESISTENCIA") equip.bonus else 0
            return bonusDefesa + resistencia
        }

    var xp: Int
        get() = experienciaAtual
        set(value) {
            experienciaAtual = value
        }

    // ------------------------------------------------
    // 🔹 GARANTIAS DE SEGURANÇA (ANTI BUG)
    // ------------------------------------------------
    fun ajustarVida() {
        if (vida > vidaMax) vida = vidaMax
        if (vida < 0) vida = 0
    }
}

enum class EspecializacaoPortugues {
    RETORICA,
    ESCRITA,
    ORATORIA
}