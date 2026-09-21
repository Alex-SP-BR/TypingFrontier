package com.typingfrontier.economy

import com.typingfrontier.Player
import com.typingfrontier.PlayerManager
import com.typingfrontier.R

data class Equipment(
    val id: String,
    val nome: String,
    val preco: Int,
    val atributoAlvo: String,
    val bonus: Int,
    val descricao: String,
    val imagemRes: Int? = null,
    val profissao: String? = null,
    val tipo: String = "GERAL",
    val raridade: String = "COMUM",
    val nivelMinimo: Int = 1,
    val origem: String = "LOJA",
    val slot: String? = null
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
    
    val EQUIP_POLICIAL = Equipment("police_vest", "Colete Balístico", 200, "RESISTENCIA", 10, "Aumenta sua defesa em patrulhas.", R.drawable.equipment_colete_balistico, "Policial", "COLETE", slot = "CORPO")
    val EQUIP_MEDICO = Equipment("stetho", "Estetoscópio de Elite", 250, "INTELIGENCIA", 8, "Melhora diagnósticos e ganhos.", R.drawable.equipment_estetoscopio_elite, "Médico", "ESTETOSCOPIO", slot = "PESCOÇO")
    val EQUIP_PROFESSOR = Equipment("rare_book", "Livro Raro de Retórica", 150, "CARISMA", 12, "Aumenta o impacto das suas palavras.", R.drawable.equipment_livro_retorica, "Professor", "LIVRO", slot = "MÃO")
    val EQUIP_ENGENHEIRO = Equipment("toolkit", "Maleta de Ferramentas", 180, "RESISTENCIA", 8, "Facilita reparos e construção.", R.drawable.equipment_maleta_ferramentas, "Engenheiro", "MALETA", slot = "MÃO")
    val EQUIP_DETETIVE = Equipment("magnifier", "Lupa Profissional", 120, "VELOCIDADE", 15, "Aumenta chance de achar pistas.", R.drawable.equipment_lupa_profissional, "Detetive", "LUPA", slot = "MÃO")

    // Novos Equipamentos Policial
    val POLICE_HELMET = Equipment("police_helmet", "Capacete", 100, "RESISTENCIA", 5, "Proteção básica para a cabeça.", R.drawable.policial_capacete, "Policial", "CAPACETE", slot = "CABEÇA")
    val POLICE_MIC = Equipment("police_lapel_microphone", "Microfone de Lapela", 90, "CARISMA", 4, "Melhora a comunicação via rádio.", R.drawable.policial_microfone_lapela, "Policial", "MICROFONE", slot = "PESCOÇO")
    val POLICE_TONFA = Equipment("police_tonfa", "Tonfa", 110, "FORCA", 5, "Bastão tático para combate próximo.", R.drawable.policial_tonfa, "Policial", "TONFA", slot = "MÃO")
    val POLICE_CUFFS = Equipment("police_handcuffs", "Algemas", 80, "FORCA", 4, "Essencial para realizar prisões.", R.drawable.policial_algemas, "Policial", "ALGEMAS", slot = "ACESSÓRIO")
    val POLICE_BOOTS = Equipment("police_boots", "Bota", 100, "RESISTENCIA", 5, "Calçado resistente para longas patrulhas.", R.drawable.policial_bota, "Policial", "BOTA", slot = "PÉS")

    // Novos Equipamentos Médico
    val MED_HELMET = Equipment("medico_helmet", "Capacete Médico", 100, "INTELIGENCIA", 5, "Proteção leve e higiênica.", R.drawable.medico_capacete, "Médico", "CAPACETE", slot = "CABEÇA")
    val MED_ID = Equipment("medico_cracha", "Crachá Médico", 80, "INTELIGENCIA", 4, "Identificação profissional indispensável.", R.drawable.medico_cracha, "Médico", "CRACHA", slot = "PESCOÇO")
    val MED_COAT = Equipment("medico_jaleco", "Jaleco Médico", 100, "INTELIGENCIA", 5, "Uniforme padrão de alta qualidade.", R.drawable.medico_jaleco, "Médico", "JALECO", slot = "CORPO")
    val MED_STETHO = Equipment("medico_estetoscopio", "Estetoscópio", 110, "INTELIGENCIA", 5, "Instrumento básico para diagnósticos.", R.drawable.medico_estetoscopio, "Médico", "ESTETOSCOPIO", slot = "MÃO")
    val MED_BAG = Equipment("medico_bolsa", "Bolsa Médica", 100, "INTELIGENCIA", 5, "Espaço para kits de primeiros socorros.", R.drawable.medico_bolsa_medica, "Médico", "BOLSA", slot = "ACESSÓRIO")
    val MED_SHOES = Equipment("medico_sapato", "Sapato Médico", 80, "INTELIGENCIA", 4, "Conforto para longos plantões.", R.drawable.medico_sapato, "Médico", "SAPATO", slot = "PÉS")

    // Novos Equipamentos Engenheiro
    val ENG_HELMET = Equipment("engenheiro_helmet", "Capacete de Engenheiro", 100, "RESISTENCIA", 5, "Segurança essencial no canteiro.", R.drawable.engenheiro_capacete, "Engenheiro", "CAPACETE", slot = "CABEÇA")
    val ENG_RADIO = Equipment("engenheiro_radio", "Rádio", 80, "INTELIGENCIA", 4, "Comunicação clara com a equipe.", R.drawable.engenheiro_radio, "Engenheiro", "RADIO", slot = "PESCOÇO")
    val ENG_VEST = Equipment("engenheiro_colete", "Colete de Engenheiro", 100, "RESISTENCIA", 5, "Alta visibilidade e proteção.", R.drawable.engenheiro_colete, "Engenheiro", "COLETE", slot = "CORPO")
    val ENG_NOTEBOOK = Equipment("engenheiro_notebook", "Notebook de Engenharia", 110, "INTELIGENCIA", 5, "Poder de processamento para cálculos.", R.drawable.engenheiro_notebook, "Engenheiro", "NOTEBOOK", slot = "MÃO")
    val ENG_METER = Equipment("engenheiro_medidor", "Medidor Digital", 100, "INTELIGENCIA", 5, "Precisão milimétrica em medições.", R.drawable.engenheiro_medidor_digital, "Engenheiro", "MEDIDOR", slot = "ACESSÓRIO")
    val ENG_BOOTS = Equipment("engenheiro_boots", "Bota de Segurança", 100, "RESISTENCIA", 5, "Proteção contra impactos e detritos.", R.drawable.engenheiro_bota, "Engenheiro", "BOTA", slot = "PÉS")

    // Novos Equipamentos Professor
    val PROF_GLASSES = Equipment("professor_glasses", "Óculos", 100, "INTELIGENCIA", 5, "Melhora o foco na leitura.", R.drawable.professor_oculos, "Professor", "OCULOS", slot = "CABEÇA")
    val PROF_ID = Equipment("professor_cracha", "Crachá", 80, "CARISMA", 4, "Identificação formal na instituição.", R.drawable.professor_cracha, "Professor", "CRACHA", slot = "PESCOÇO")
    val PROF_SUIT = Equipment("professor_terno", "Terno", 100, "CARISMA", 5, "Aparência profissional e respeitável.", R.drawable.professor_terno, "Professor", "TERNO", slot = "CORPO")
    val PROF_RULER = Equipment("professor_regua", "Régua", 80, "INTELIGENCIA", 4, "Ferramenta básica para geometria.", R.drawable.professor_regua, "Professor", "REGUA", slot = "MÃO")
    val PROF_PHONE = Equipment("professor_phone", "Celular", 100, "CARISMA", 5, "Conectividade e pesquisa rápida.", R.drawable.professor_celular, "Professor", "CELULAR", slot = "ACESSÓRIO")
    val PROF_SHOES = Equipment("professor_shoes", "Sapato", 100, "CARISMA", 5, "Elegância e conforto em sala.", R.drawable.professor_sapato, "Professor", "SAPATO", slot = "PÉS")

    // Novos Equipamentos Detetive
    val DET_HAT = Equipment("detetive_hat", "Chapéu de Detetive", 100, "INTELIGENCIA", 5, "Estilo clássico de investigação.", R.drawable.detetive_chapeu, "Detetive", "CHAPEU", slot = "CABEÇA")
    val DET_TIE = Equipment("detetive_tie", "Gravata", 80, "INTELIGENCIA", 4, "Disfarce formal para infiltração.", R.drawable.detetive_gravata, "Detetive", "GRAVATA", slot = "PESCOÇO")
    val DET_COAT = Equipment("detetive_coat", "Sobretudo", 110, "VELOCIDADE", 5, "Oculta equipamentos e protege.", R.drawable.detetive_sobretudo, "Detetive", "SOBRETUDO", slot = "CORPO")
    val DET_NOTEBOOK = Equipment("detetive_notebook", "Caderno de Investigação", 80, "VELOCIDADE", 4, "Anotações cruciais sobre o caso.", R.drawable.detetive_caderno_investigacao, "Detetive", "CADERNO", slot = "MÃO")
    val DET_FLASHLIGHT = Equipment("detetive_flashlight", "Lanterna", 100, "INTELIGENCIA", 5, "Ilumina pistas em locais escuros.", R.drawable.detetive_lanterna, "Detetive", "LANTERNA", slot = "ACESSÓRIO")
    val DET_SHOES = Equipment("detetive_shoes", "Sapato Social", 100, "VELOCIDADE", 5, "Agilidade com discrição.", R.drawable.detetive_sapato, "Detetive", "SAPATO", slot = "PÉS")

    private val todosEquipamentos = listOf(
        EQUIP_POLICIAL, EQUIP_MEDICO, EQUIP_PROFESSOR, EQUIP_ENGENHEIRO, EQUIP_DETETIVE,
        POLICE_HELMET, POLICE_MIC, POLICE_TONFA, POLICE_CUFFS, POLICE_BOOTS,
        MED_HELMET, MED_ID, MED_COAT, MED_STETHO, MED_BAG, MED_SHOES,
        ENG_HELMET, ENG_RADIO, ENG_VEST, ENG_NOTEBOOK, ENG_METER, ENG_BOOTS,
        PROF_GLASSES, PROF_ID, PROF_SUIT, PROF_RULER, PROF_PHONE, PROF_SHOES,
        DET_HAT, DET_TIE, DET_COAT, DET_NOTEBOOK, DET_FLASHLIGHT, DET_SHOES,
        Equipment("police_vest_2", "Colete Tático Avançado", 450, "RESISTENCIA", 25, "Proteção pesada para missões críticas.", R.drawable.equipment_colete_tatico_avancado, "Policial", "COLETE", slot = "CORPO"),
        Equipment("stetho_2", "Monitor Cardíaco Portátil", 500, "INTELIGENCIA", 20, "Tecnologia médica de ponta.", R.drawable.equipment_monitor_cardiaco, "Médico", "ESTETOSCOPIO", slot = "PESCOÇO"),
        Equipment("rare_book_2", "Enciclopédia de Filosofia", 400, "CARISMA", 25, "Domine a arte da persuasão.", R.drawable.equipment_enciclopedia_filosofia, "Professor", "LIVRO", slot = "MÃO"),
        Equipment("toolkit_2", "Maleta de Ferramentas Pro", 380, "RESISTENCIA", 18, "Tudo o que um engenheiro precisa.", R.drawable.equipment_maleta_ferramentas_pro, "Engenheiro", "MALETA", slot = "MÃO"),
        Equipment("drone", "Drone de Vigilância", 600, "VELOCIDADE", 30, "Visão aérea total para o detetive.", R.drawable.equipment_drone_vigilancia, "Detetive", "DRONE", slot = "ACESSÓRIO")
    )

    fun getAllEquipments(): List<Equipment> = todosEquipamentos

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
            "FORCA" -> player.forcaEfetiva
            "INTELIGENCIA" -> player.inteligenciaEfetiva
            "CARISMA" -> player.carismaEfetiva
            "RESISTENCIA" -> player.resistenciaEfetiva
            "VELOCIDADE" -> player.velocidadeEfetiva
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
        // 1. Verificar se o jogador já possuía Medicamento antes do trauma para uso automático
        val hadMedicine = player.estoqueMedicamento > 0
        var medicineMsg = ""

        // O trauma físico ocorre sempre, a menos que o jogador consiga fugir (passiva policial processada antes)
        player.traumasAcumulados++
        
        // Se possuía Medicamento, aplica o tratamento automático imediatamente
        if (hadMedicine) {
            medicineMsg = "\n\n" + com.typingfrontier.GameEngine.applyMedicineTreatment(isAutomatic = true)
        }

        // Se for o primeiro trauma ou estava recuperado, inicia o ciclo de 2 dias
        if (player.diasParaRecuperarTrauma <= 0) {
            player.diasParaRecuperarTrauma = 2
        }

        val limite = player.limiteTraumas
        // Verifica se atingiu o limite considerando o tratamento automático já aplicado
        val atingiuLimite = player.traumasAcumulados >= limite

        // 2. Recuperação de Status Base (Ocorre em todos os desmaios)
        player.vida = (player.vidaMax * 0.4).toInt()
        player.energia = (player.energiaMax * 0.2).toInt()
        player.cansacoMental = (player.cansacoMax * 0.6).toInt() // Energia Mental fica em 40%

        if (!atingiuLimite) {
            // PUNIÇÃO LEVE (Ainda tem resistência física)
            val perdaXP = (player.experienciaAtual * 0.15).toInt() // Perde 15% do XP atual do nível
            player.experienciaAtual -= perdaXP
            
            return "🚑 VOCÊ DESMAIOU!\nAcordou no hospital fraco. Seu corpo resistiu ao trauma, mas parte da Experiência adquirida foi perdida e você perdeu todo o lucro da exploração.\n\n⚠️ Traumas: ${player.traumasAcumulados}/$limite.$medicineMsg"
        } else {
            // PUNIÇÃO GRAVE: ESTADO CRÍTICO (COLAPSO)
            player.traumasAcumulados = 0 // Reseta o ciclo após o colapso
            player.diasParaRecuperarTrauma = 0

            var msgPenalidade = ""
            val nivelOriginal = player.nivel
            
            val xpPerdido = PlayerManager.aplicarPenalidadeXpColapso()
            
            if (player.nivel < nivelOriginal) {
                msgPenalidade = "\n📉 NÍVEL REDUZIDO: Você perdeu nível!\nSeus limites de Vida, Energia e Mente foram reduzidos."
            } else {
                msgPenalidade = "\n📉 PROGRESSO PERDIDO: Parte da sua Experiência acumulada foi perdida."
            }

            // Atributos base sofrem sequelas (Novo sistema de perda de progresso)
            PlayerManager.aplicarPenalidadeAtributoColapso("FORCA")
            PlayerManager.aplicarPenalidadeAtributoColapso("INTELIGENCIA")
            PlayerManager.aplicarPenalidadeAtributoColapso("VELOCIDADE")
            PlayerManager.aplicarPenalidadeAtributoColapso("CARISMA")
            PlayerManager.aplicarPenalidadeAtributoColapso("RESISTENCIA")

            // SEGURO DE EQUIPAMENTOS
            val itensEquipados = player.slotsEquipados.filter { it.value != null }
            var msgEquipamento = ""

            if (itensEquipados.isNotEmpty()) {
                val tinhaSeguro = player.estoqueBencao > 0
                
                // Consumo do Seguro (Sempre ocorre se houver seguro e itens equipados durante um Colapso)
                if (tinhaSeguro) {
                    player.estoqueBencao--
                    msgEquipamento = "\n\n🛡️ Seu Seguro de Equipamentos foi utilizado."
                }
                
                // Sorteio de Perda (25% de chance)
                val sorteioPerda = (1..100).random()
                if (sorteioPerda <= 25 && !tinhaSeguro) {
                    // Perda definitiva (Sorteio desfavorável E Sem Seguro)
                    val slotAleatorio = itensEquipados.keys.random()
                    val itemID = player.slotsEquipados[slotAleatorio]
                    val itemNome = ProfessionManager.getEquipment(itemID)?.nome ?: "Item"
                    
                    player.slotsEquipados[slotAleatorio] = null
                    if (player.equipamentoId == itemID) player.equipamentoId = null
                    
                    msgEquipamento = "\n\n⚠️ Você perdeu um equipamento devido ao Colapso.\nEquipamento perdido: $itemNome"
                }
            }

            return "🚨 COLAPSO CORPORAL! 🚨\nSeus traumas sucessivos levaram a um estado crítico (coma).$msgPenalidade\nAlguns atributos sofreram sequelas permanentes.$msgEquipamento$medicineMsg"
        }
    }
}
