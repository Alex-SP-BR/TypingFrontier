package com.typingfrontier.economy

import com.typingfrontier.Player
import com.typingfrontier.PlayerManager

data class Equipment(
    val id: String,
    val nome: String,
    val preco: Int,
    val atributoAlvo: String,
    val bonus: Int,
    val descricao: String
)

data class ProfessionConfig(
    val nome: String,
    val salarioBase: Int,
    val atributoPrincipal: String,
    val bonusAtributoInicial: String,
    val equipamentoInicial: Equipment,
    val custoComida: Int,
    val ganhoEnergiaComida: Int,
    val ganhoSaciedade: Int,
    val bonusTreino: String // Atributo que progride mais rápido
)

object ProfessionManager {
    
    val EQUIP_POLICIAL = Equipment("police_vest", "Colete Balístico", 200, "RESISTENCIA", 10, "Aumenta sua defesa em patrulhas.")
    val EQUIP_MEDICO = Equipment("stetho", "Estetoscópio de Elite", 250, "INTELIGENCIA", 8, "Melhora diagnósticos e ganhos.")
    val EQUIP_PROFESSOR = Equipment("rare_book", "Livro Raro de Retórica", 150, "CARISMA", 12, "Aumenta o impacto das suas palavras.")
    val EQUIP_ENGENHEIRO = Equipment("toolkit", "Maleta de Ferramentas", 180, "RESISTENCIA", 8, "Facilita reparos e construção.")
    val EQUIP_DETETIVE = Equipment("magnifier", "Lupa Profissional", 120, "VELOCIDADE", 15, "Aumenta chance de achar pistas.")

    private val todosEquipamentos = listOf(
        EQUIP_POLICIAL, EQUIP_MEDICO, EQUIP_PROFESSOR, EQUIP_ENGENHEIRO, EQUIP_DETETIVE,
        Equipment("police_vest_2", "Colete Tático Avançado", 450, "RESISTENCIA", 25, "Proteção pesada para missões críticas."),
        Equipment("stetho_2", "Monitor Cardíaco Portátil", 500, "INTELIGENCIA", 20, "Tecnologia médica de ponta."),
        Equipment("rare_book_2", "Enciclopédia de Filosofia", 400, "CARISMA", 25, "Domine a arte da persuasão."),
        Equipment("toolkit_2", "Maleta de Ferramentas Pro", 380, "RESISTENCIA", 18, "Tudo o que um engenheiro precisa."),
        Equipment("drone", "Drone de Vigilância", 600, "VELOCIDADE", 30, "Visão aérea total para o detetive.")
    )

    fun getEquipment(id: String?): Equipment? = todosEquipamentos.find { it.id == id }

    fun getItemsForShop(profissao: String): List<Equipment> {
        return todosEquipamentos.filter { equip ->
            val config = getConfig(profissao)
            equip.atributoAlvo == config?.atributoPrincipal || equip.atributoAlvo == config?.bonusAtributoInicial
        }
    }

    private val profissoes = mapOf(
        // Ajuste Definitivo: Trabalho = Sustento, Treino = Progresso, Exploração = Riqueza.
        "Policial" to ProfessionConfig("Policial", 5, "FORCA", "RESISTENCIA", EQUIP_POLICIAL, 15, 25, 30, "FORCA"),
        "Médico" to ProfessionConfig("Médico", 5, "INTELIGENCIA", "INTELIGENCIA", EQUIP_MEDICO, 15, 25, 25, "INTELIGENCIA"),
        "Engenheiro" to ProfessionConfig("Engenheiro", 5, "INTELIGENCIA", "RESISTENCIA", EQUIP_ENGENHEIRO, 15, 25, 35, "RESISTENCIA"),
        "Professor" to ProfessionConfig("Professor", 5, "CARISMA", "CARISMA", EQUIP_PROFESSOR, 15, 25, 20, "CARISMA"),
        "Detetive" to ProfessionConfig("Detetive", 5, "INTELIGENCIA", "VELOCIDADE", EQUIP_DETETIVE, 15, 25, 30, "VELOCIDADE")
    )

    fun getConfig(nome: String): ProfessionConfig? = profissoes[nome]

    fun aplicarBonusInicial(player: Player) {
        val config = getConfig(player.profissao) ?: return
        
        when (player.profissao) {
            "Policial" -> { player.forca = 6; player.resistencia = 6; player.inteligencia = 3; player.carisma = 2; player.velocidade = 3 }
            "Médico" -> { player.forca = 2; player.resistencia = 4; player.inteligencia = 7; player.carisma = 4; player.velocidade = 3 }
            "Engenheiro" -> { player.forca = 3; player.resistencia = 4; player.inteligencia = 7; player.carisma = 3; player.velocidade = 3 }
            "Professor" -> { player.forca = 2; player.resistencia = 3; player.inteligencia = 6; player.carisma = 6; player.velocidade = 3 }
            "Detetive" -> { player.forca = 4; player.resistencia = 4; player.inteligencia = 5; player.carisma = 3; player.velocidade = 5 }
        }

        when (config.bonusAtributoInicial) {
            "FORCA" -> player.forca += 2
            "INTELIGENCIA" -> player.inteligencia += 2
            "CARISMA" -> player.carisma += 2
            "RESISTENCIA" -> player.resistencia += 2
            "VELOCIDADE" -> player.velocidade += 2
        }

        player.equipamentoId = config.equipamentoInicial.id
        player.experienciaParaProximoNivel = PlayerManager.calcularXpParaProximoNivel(player.nivel)
    }

    fun calcularSalario(player: Player): Int {
        val config = getConfig(player.profissao) ?: return 5
        val nivelAtributo = when (config.atributoPrincipal) {
            "FORCA" -> player.forca
            "INTELIGENCIA" -> player.inteligencia
            "CARISMA" -> player.carisma
            "RESISTENCIA" -> player.resistencia
            "VELOCIDADE" -> player.velocidade
            else -> 1
        }
        // Fórmulas reduzidas para evitar acúmulo infinito de dinheiro no trabalho seguro.
        return config.salarioBase + (nivelAtributo * 2) + (player.nivel * 1)
    }

    fun comer(player: Player): String {
        // Agora processado via GameEngine.processEat()
        return "Método Depreciado. Use GameEngine."
    }

    fun hospitalizar(player: Player): String {
        // 1. O trauma físico ocorre sempre, a menos que o jogador consiga fugir (passiva policial processada antes)
        player.traumasAcumulados++
        
        // Se for o primeiro trauma ou estava recuperado, inicia o ciclo de 2 dias
        if (player.diasParaRecuperarTrauma <= 0) {
            player.diasParaRecuperarTrauma = 2
        }

        val limite = player.limiteTraumas
        val atingiuLimite = player.traumasAcumulados >= limite

        // 2. Recuperação de Status Base (Ocorre em todos os desmaios)
        player.vida = (player.vidaMax * 0.4).toInt()
        player.energia = (player.energiaMax * 0.2).toInt()
        player.cansacoMental = 0

        if (!atingiuLimite) {
            // PUNIÇÃO LEVE (Ainda tem resistência física)
            val perdaXP = (player.experienciaAtual * 0.15).toInt() // Perde 15% do XP atual do nível
            player.experienciaAtual -= perdaXP
            
            return "🚑 VOCÊ DESMAIOU!\nAcordou no hospital fraco. Seu corpo resistiu ao trauma, mas você perdeu $perdaXP XP e todo o lucro da exploração.\n\n⚠️ Desgaste: ${player.traumasAcumulados}/$limite traumas."
        } else {
            // PUNIÇÃO GRAVE: ESTADO CRÍTICO (COLAPSO)
            player.traumasAcumulados = 0 // Reseta o ciclo após o colapso
            player.diasParaRecuperarTrauma = 0

            if (player.temBlessing) {
                player.temBlessing = false
                player.vida = (player.vidaMax * 0.2).toInt()
                player.energia = 10
                return "🛡️ ESTADO CRÍTICO: A BENÇÃO TE SALVOU!\nSeu corpo entrou em colapso por traumas sucessivos, mas a proteção divina impediu a perda de níveis e atributos. A benção foi consumida."
            }

            var msgPenalidade = ""
            if (player.nivel > 1) {
                PlayerManager.reduzirNivel()
                msgPenalidade = "\n📉 NÍVEL REDUZIDO: Você voltou para o Nível ${player.nivel}!"
            }

            if (player.forca > 3) player.forca--
            if (player.inteligencia > 3) player.inteligencia--
            if (player.velocidade > 3) player.velocidade--
            if (player.carisma > 3) player.carisma--
            if (player.resistencia > 3) player.resistencia--

            return "🚨 COLAPSO CORPORAL! 🚨\nSeus traumas sucessivos levaram a um estado crítico (coma).\n$msgPenalidade\nSeus atributos físicos e mentais diminuíram permanentemente pelas sequelas."
        }
    }
}
