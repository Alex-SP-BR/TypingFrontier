package com.typingfrontier

import com.typingfrontier.economy.Equipment
import com.typingfrontier.economy.ProfessionManager
import com.typingfrontier.utils.CurrencyUtils

/**
 * O Cérebro do Jogo. Centraliza todas as regras de negócio e validações.
 */
object GameEngine {

    fun dispatch(action: GameAction): EngineResult {
        return try {
            val result = when (action) {
                is GameAction.Work -> processWork()
                is GameAction.Overtime -> processOvertime()
                is GameAction.Eat -> processEat()
                is GameAction.Sleep -> processSleep()
                is GameAction.Rest -> processRest()
                is GameAction.Train -> processTrain(action.attribute, action.intensity)
                is GameAction.Explore -> processExplore(action.zoneId)
                is GameAction.StudyError -> processStudyError(action.attribute)
                is GameAction.CollectRewards -> processCollectRewards(action.xp, action.money)
                is GameAction.BuyItem -> processBuyItem(action.item)
                is GameAction.CompleteMission -> processCompleteMission(action.xp, action.money)
                is GameAction.EquipItem -> processEquipItem(action.itemId)
                is GameAction.UnequipItem -> processUnequipItem(action.slot)
            }
            verifyIntegrity()
            
            // 🏆 GATILHO DE CONQUISTA: ECONOMIA
            com.typingfrontier.collection.AchievementManager.checkEconomy(TypingFrontierApp.getAppContext())
            
            return result
        } catch (e: Exception) {
            EngineResult.Failure("Erro na Engine: ${e.message}")
        }
    }

    private fun processWork(): EngineResult {
        val p = PlayerManager.player
        if (!TimeManager.podeAgir()) return EngineResult.Failure("Muito tarde para trabalhar! Vá dormir.")
        if (p.trabalhouHoje) return EngineResult.Failure("Você já trabalhou hoje. Volte amanhã!")
        
        val custoEnergia = 35 // Invariante: Custo > Recuperação Comida
        
        if (p.energia < custoEnergia) {
            val msg = if (TimeManager.podeAgir()) "Energia insuficiente ($custoEnergia necessária). Que tal fazer um lanche?" else "Energia insuficiente ($custoEnergia necessária)."
            return EngineResult.Failure(msg)
        }
        if (p.cansacoMental >= p.cansacoMax * 0.95) return EngineResult.Failure("Energia Mental esgotada! Faça uma pausa ou vá dormir.")

        p.trabalhouHoje = true
        p.energia -= custoEnergia
        p.cansacoMental += 11

        val ganho = ProfessionManager.calcularSalario(p)
        p.dinheiro += ganho

        var extraMsg: String? = null
        if (p.cansacoMental >= p.cansacoMax || p.energia <= 0) {
            extraMsg = ProfessionManager.hospitalizar(p)
        }

        return EngineResult.Success("💼 Trabalho concluído! Ganhou ${CurrencyUtils.formatar(ganho)}.", extraMsg)
    }

    /**
     * Lógica central para a execução da Hora Extra.
     * Consome recursos e avança o tempo. Recompensa financeira será adicionada na Etapa 5.
     */
    private fun processOvertime(): EngineResult {
        val p = PlayerManager.player
        
        // 1. Verificações de Condição
        if (!p.trabalhouHoje) return EngineResult.Failure("Você precisa concluir o trabalho normal antes.")
        
        val duracaoMinutos = getMaiorDuracaoOvertimeDisponivel() ?: return EngineResult.Failure("Sem tempo suficiente antes das 22h.")
        
        val numeroHE = p.horasExtrasFeitasHoje + 1
        val percentual = Math.min(numeroHE * 0.05, 0.5)
        
        // Cálculos de Custo (Modelo B Híbrido)
        val custoEnergia = (5 + (p.energiaMax * percentual)).toInt()
        val gastoMente = (2.5 + (p.cansacoMax * percentual)).toInt()

        // 2. Validação de Recursos
        if (p.energia < custoEnergia) return EngineResult.Failure("Energia física insuficiente ($custoEnergia necessária).")
        if (p.cansacoMax - p.cansacoMental < gastoMente) return EngineResult.Failure("Capacidade mental insuficiente ($gastoMente necessária).")

        // 3. Execução
        val salarioNormal = ProfessionManager.calcularSalario(p)
        val duracaoHoras = duracaoMinutos / 60.0
        val fatorEficiencia = Math.max(0.5, 1.0 - (p.horasExtrasFeitasHoje * 0.1))
        val recompensa = (salarioNormal / 8.0) * duracaoHoras * 2.0 * fatorEficiencia

        p.energia -= custoEnergia
        p.cansacoMental += gastoMente
        p.dinheiro += recompensa.toInt()

        TimeManager.avancarTempo(minutos = duracaoMinutos)
        p.horasExtrasFeitasHoje++
        
        return EngineResult.Success("Hora Extra #$numeroHE concluída com sucesso! Ganhou ${CurrencyUtils.formatar(recompensa.toInt())}.")
    }

    /**
     * Retorna a maior duração possível (em minutos) que cabe até as 22:00.
     */
    fun getMaiorDuracaoOvertimeDisponivel(): Int? {
        val p = PlayerManager.player
        val minutosAtuais = p.hora * 60 + p.minuto
        val minutosLimite = 22 * 60 // 1320 minutos
        val tempoRestante = minutosLimite - minutosAtuais

        val opcoes = listOf(330, 300, 270, 240, 210, 180, 150, 120, 60, 30)
        return opcoes.find { it <= tempoRestante }
    }

    /**
     * Retorna a quantidade de anúncios necessários para a próxima Hora Extra.
     */
    fun getAnunciosNecessariosOvertime(duracaoMinutos: Int): Int {
        return when (duracaoMinutos) {
            330, 300 -> 1 // 5h30, 5h
            270 -> 2      // 4h30
            else -> 3     // 4h e abaixo (até 30min)
        }
    }

    /**
     * Calcula em qual horário a Hora Extra terminará.
     */
    fun calcularHorarioTerminoOvertime(duracaoMinutos: Int): Pair<Int, Int> {
        val p = PlayerManager.player
        var h = p.hora
        var m = p.minuto + duracaoMinutos
        
        h += m / 60
        m %= 60
        
        if (h > 22) {
            h = 22
            m = 0
        }
        
        return h to m
    }

    private fun processEat(): EngineResult {
        val p = PlayerManager.player
        val config = ProfessionManager.getConfig(p.profissao) ?: return EngineResult.Failure("Erro de perfil.")

        if (p.dinheiro < config.custoComida) return EngineResult.Failure("Frons insuficientes (${CurrencyUtils.formatar(config.custoComida)}).")
        if (p.energia >= p.energiaMax) return EngineResult.Failure("Sua Energia já está no máximo! Não precisa comer agora.")
        if (!TimeManager.podeAgir()) return EngineResult.Failure("Lanchonetes fechadas. Vá dormir.")

        p.dinheiro -= config.custoComida

        p.energia = (p.energia + 20).coerceAtMost(p.energiaMax)
        return EngineResult.Success("🍴 Você comeu uma refeição de ${config.nome}.", "Energia +20")
    }

    private fun processSleep(): EngineResult {
        val p = PlayerManager.player
        val custoVida = 60 + (p.nivel * 10)
        
        // NOVA REGRA: Bloqueio de sono estratégico
        val mentalEnergiaRestante = p.cansacoMax - p.cansacoMental
        val custoMenteEstudo = if (TimeManager.podeAgir()) 4 else (p.cansacoMax * 0.11).toInt().coerceAtLeast(1)
        val custoEnergiaEstudo = if (TimeManager.podeAgir()) 1 else (p.energiaMax * 0.1).toInt().coerceAtLeast(1)

        val temEnergiaParaQueimar = p.energia >= custoEnergiaEstudo && 
                                    mentalEnergiaRestante >= custoMenteEstudo && 
                                    p.dinheiro > custoVida
        
        if (temEnergiaParaQueimar) {
            val msg = if (TimeManager.podeAgir()) {
                "Você ainda tem disposição! Estude ou treine mais um pouco antes de dormir."
            } else {
                "Você ainda tem disposição! Estude mais um pouco antes de dormir."
            }
            return EngineResult.Failure(msg)
        }

        // REDE DE SEGURANÇA: Permite dormir se for tarde (22h) OU se estiver exausto 
        // OU se não houver recursos para NENHUMA ação produtiva (Trabalho ou Estudo).
        val podeTrabalhar = p.energia >= 35 && !p.trabalhouHoje && p.cansacoMental < p.cansacoMax * 0.95
        val podeEstudar = p.energia >= custoEnergiaEstudo && p.dinheiro >= 1 && mentalEnergiaRestante >= custoMenteEstudo
        val estaTravado = !podeTrabalhar && !podeEstudar && p.pausouHoje
        
        val podeDormir = !TimeManager.podeAgir() || p.energia < 5 || estaTravado
        
        if (!podeDormir) {
            return EngineResult.Failure("Você ainda tem fôlego! O dia só acaba às 22:00 (ou quando você não tiver mais como agir).")
        }
        
        // REDE DE SEGURANÇA 2: Sono na rua (Penalidade Real)
        if (p.dinheiro < custoVida) {
            p.trabalhouHoje = false
            p.pausouHoje = false
            p.energia = (p.energiaMax * 0.3).toInt() // Apenas 30% de energia
            p.vida = (p.vida - 20).coerceAtLeast(10) // Perda de HP pelo frio/desconforto
            p.cansacoMental = 0
            p.ajustarVida()
            TimeManager.resetarDia()
            return EngineResult.Success("🏚️ Você dormiu na rua por falta de Frons.", "Energia: 30% | Vida: -20 HP. Dia ${p.dia} começou.")
        }

        // Sono Normal
        p.trabalhouHoje = false
        p.dinheiro -= custoVida
        p.energia = p.energiaMax
        p.cansacoMental = 0
        p.pausouHoje = false
        p.ajustarVida()
        
        // 🩹 RECUPERAÇÃO DE TRAUMAS (2 dias por trauma)
        if (p.traumasAcumulados > 0) {
            p.diasParaRecuperarTrauma--
            if (p.diasParaRecuperarTrauma <= 0) {
                p.traumasAcumulados--
                if (p.traumasAcumulados > 0) {
                    p.diasParaRecuperarTrauma = 2
                }
            }
        }
        
        TimeManager.resetarDia() 
        return EngineResult.Success("😴 Você dormiu bem.", "Status Restaurados! Dia ${p.dia}")
    }

    private fun processRest(): EngineResult {
        val p = PlayerManager.player
        
        // 1. Requirement Gate
        if (p.pausouHoje) {
            return EngineResult.Failure("Você já fez sua pausa diária.")
        }

        if (p.cansacoMental <= 0) {
            return EngineResult.Failure("Sua mente está totalmente descansada e focada!")
        }
        
        // 2. Resource & Time Debit
        // 3. Core Execution
        p.pausouHoje = true
        p.cansacoMental = (p.cansacoMental - 20).coerceAtLeast(0)

        return EngineResult.Success("🧘 Pausa concluída.", "Cansaço Mental -20")
    }

    private fun processTrain(atributo: String, intensidade: String): EngineResult {
        val p = PlayerManager.player
        val config = ProfessionManager.getConfig(p.profissao)
        val isMental = atributo == "INTELIGENCIA" || atributo == "CARISMA"

        // Treino Mental não para às 22h, Treino Físico sim.
        if (!isMental && !TimeManager.podeAgir()) return EngineResult.Failure("Hora de dormir! Volte amanhã.")

        // 1. Definição de Custos e Ganhos (Balanceamento)
        val custoDinheiro = if (isMental) 1 else when (intensidade) { 
            "LEVE" -> 2; "MEDIO" -> 5; else -> 10 
        }

        // LÓGICA DE ESTUDO NOTURNO (PROPORCIONAL)
        val isEstudoNoturno = isMental && !TimeManager.podeAgir()
        
        var gastoEnergia = if (isEstudoNoturno) {
            (p.energiaMax * 0.1).toInt().coerceAtLeast(1)
        } else if (isMental) {
            1
        } else {
            when (intensidade) { "LEVE" -> 10; "MEDIO" -> 25; else -> 45 }
        }

        var gastoMente = if (isEstudoNoturno) {
            (p.cansacoMax * 0.11).toInt().coerceAtLeast(1)
        } else if (isMental) {
            4
        } else {
            when (intensidade) { "LEVE" -> 8; "MEDIO" -> 15; else -> 23 }
        }

        // 2. Validação de Recursos
        if (p.dinheiro < custoDinheiro) return EngineResult.Failure("Frons insuficientes (${CurrencyUtils.formatar(custoDinheiro)}).")
        if (p.energia < gastoEnergia) {
            val msg = if (TimeManager.podeAgir()) "Energia insuficiente ($gastoEnergia necessária). Que tal fazer um lanche?" else "Energia insuficiente ($gastoEnergia necessária)."
            return EngineResult.Failure(msg)
        }
        
        if (p.cansacoMax - p.cansacoMental < gastoMente) return EngineResult.Failure("Energia Mental esgotada! Faça uma pausa ou vá dormir.")

        // 3. Execução e Tempo
        p.dinheiro -= custoDinheiro
        p.energia -= gastoEnergia
        p.cansacoMental += gastoMente

        if (!isMental) {
            val (horas, minutos) = when(intensidade) {
                "LEVE" -> 0 to 30
                "MEDIO" -> 0 to 45
                else -> 1 to 0
            }
            TimeManager.avancarTempo(horas, minutos)
        }

        // 4. Lógica de Falha no Treino Físico
        if (!isMental) {
            val factor = 3
            val bonusSucesso = p.nivel / factor

            val chanceBaseFalha = when (intensidade) {
                "LEVE" -> 10
                "MEDIO" -> 20
                "PESADO" -> 40
                else -> 10
            }

            var chanceFinalFalha = (chanceBaseFalha - bonusSucesso).coerceAtLeast(1)
            if (p.cansacoMental >= p.cansacoMax * 0.8) chanceFinalFalha += 10

            val sorteio = (1..100).random()
            if (sorteio <= chanceFinalFalha) {
                return EngineResult.Success("⚠️ Treino falhou!", "Você se lesionou ou não aguentou o peso. Sem ganho de XP.")
            }
        }
        
        // XP Reduzido para Físico (Mental continua 25 fixo)
        var ganhoXP = if (isMental) 25 else when (intensidade) { "LEVE" -> 6; "MEDIO" -> 15; else -> 34 }
        if (config?.bonusTreino == atributo) ganhoXP = (ganhoXP * 1.5).toInt()

        applyAttributeXP(atributo, ganhoXP)
        val msgSucesso = if (isMental) "Você se sente mais sábio! Ganhou $ganhoXP de Experiência." else "Treino concluído! +$ganhoXP XP"
        return EngineResult.Success(msgSucesso)
    }

    private fun processExplore(zoneId: String): EngineResult {
        val p = PlayerManager.player

        if (!TimeManager.podeAgir()) return EngineResult.Failure("Muito tarde para explorar.")
        if (p.energia < 5) {
            val msg = if (TimeManager.podeAgir()) "Energia insuficiente (necessário 5). Que tal fazer um lanche?" else "Energia insuficiente (necessário 5)."
            return EngineResult.Failure(msg)
        }
        if (p.cansacoMax - p.cansacoMental < 7) return EngineResult.Failure("Energia Mental esgotada! Faça uma pausa ou vá dormir.")
        
        p.energia -= 5
        p.cansacoMental += 7
        TimeManager.avancarTempo(horas = 1, minutos = 15)
        return EngineResult.Success("Avanço na exploração.")
    }

    private fun processStudyError(atributo: String): EngineResult {
        val p = PlayerManager.player
        p.energia -= 1
        p.cansacoMental += 1
        return EngineResult.Success("Erro no estudo. Cansaço acumulado.")
    }

    private fun processCollectRewards(xp: Int, money: Int): EngineResult {
        val p = PlayerManager.player
        val niveisGanhos = PlayerManager.ganharXp(xp)
        p.dinheiro += money
        
        val msg = if (niveisGanhos > 0) "Recompensas coletadas! +$niveisGanhos níveis!" else "Recompensas coletadas com sucesso!"
        return EngineResult.Success(msg)
    }

    private fun processBuyItem(item: Equipment): EngineResult {
        val p = PlayerManager.player

        // 1. Verificação de Nível Mínimo
        if (p.nivel < item.nivelMinimo) {
            return EngineResult.Failure("Nível ${item.nivelMinimo} necessário para este equipamento.")
        }

        // Lógica de Preço (Blessing tem preço fixo por nível, outros usam inflação)
        val precoFinal = if (item.id == "blessing") item.preco else EconomyManager.precoInflacionado(item.preco)

        // 2. Verificação de Saldo
        if (p.dinheiro < precoFinal) {
            val falta = precoFinal - p.dinheiro
            return EngineResult.Failure("Frons insuficientes. Falta ${CurrencyUtils.formatar(falta)}.")
        }

        // 3. Execução da Compra
        if (item.id == "blessing") {
            if (p.temBlessing) return EngineResult.Failure("Você já possui uma benção ativa.")
            p.temBlessing = true
            p.dinheiro -= precoFinal
            return EngineResult.Success("Benção adquirida por ${CurrencyUtils.formatar(precoFinal)}!")
        } else {
            // VERIFICAÇÃO DE CAPACIDADE DA MOCHILA (Unidade real por item)
            val ocupacaoAtual = p.mochila.values.sum()
            if (ocupacaoAtual >= p.capacidadeMochila) {
                return EngineResult.Failure("Mochila cheia ($ocupacaoAtual/${p.capacidadeMochila}).")
            }

            p.dinheiro -= precoFinal
            // Adiciona à mochila em vez de equipar automaticamente
            p.mochila[item.id] = (p.mochila[item.id] ?: 0) + 1
            
            return EngineResult.Success("Comprado: ${item.nome}. Item enviado para a mochila!")
        }
    }

    private fun processCompleteMission(xp: Int, money: Int): EngineResult {
        PlayerManager.ganharXp(xp)
        PlayerManager.player.dinheiro += money
        return EngineResult.Success("Missão concluída com sucesso!")
    }

    private fun processEquipItem(itemId: String): EngineResult {
        val p = PlayerManager.player
        val equip = ProfessionManager.getEquipment(itemId) ?: return EngineResult.Failure("Equipamento não encontrado.")

        // 1. Verificação de Posse
        val qtdNaMochila = p.mochila[itemId] ?: 0
        if (qtdNaMochila <= 0) return EngineResult.Failure("Você não possui este item na mochila.")

        // 2. Verificação de Slot
        val targetSlot = equip.slot ?: return EngineResult.Failure("Este item não pode ser equipado.")
        if (!p.slotsEquipados.containsKey(targetSlot)) return EngineResult.Failure("Slot de equipamento inválido.")

        // 3. Devolver item atual do slot para a mochila (se houver)
        val currentInSlotId = p.slotsEquipados[targetSlot]
        if (currentInSlotId != null) {
            p.mochila[currentInSlotId] = (p.mochila[currentInSlotId] ?: 0) + 1
        }

        // 4. Equipar novo item
        p.mochila[itemId] = qtdNaMochila - 1
        p.slotsEquipados[targetSlot] = itemId
        
        // 5. Ponte de compatibilidade: Atualiza equipamentoId para que a UI antiga reconheça como equipado
        p.equipamentoId = itemId

        return EngineResult.Success("Equipado: ${equip.nome}!")
    }

    private fun processUnequipItem(slot: String): EngineResult {
        val p = PlayerManager.player
        if (!p.slotsEquipados.containsKey(slot)) return EngineResult.Failure("Slot inválido.")

        val itemId = p.slotsEquipados[slot] ?: return EngineResult.Failure("Slot já está vazio.")
        val equip = ProfessionManager.getEquipment(itemId) ?: return EngineResult.Failure("Erro ao localizar equipamento.")

        // Devolver para a mochila
        p.mochila[itemId] = (p.mochila[itemId] ?: 0) + 1
        p.slotsEquipados[slot] = null

        // Ponte de compatibilidade: se era o item apontado, limpa
        if (p.equipamentoId == itemId) {
            p.equipamentoId = null
        }

        return EngineResult.Success("Desequipado: ${equip.nome}!")
    }

    private fun applyAttributeXP(atributo: String, ganho: Int) {
        val p = PlayerManager.player

        // Curva Leve de Progressão: XP necessário = 100 * (1 + nível * 0.05)
        fun calcularLimite(nivel: Int) = (100 * (1 + nivel * 0.05)).toInt()

        when (atributo) {
            "FORCA" -> {
                p.progressoForca += ganho
                while (p.progressoForca >= calcularLimite(p.forca)) {
                    p.progressoForca -= calcularLimite(p.forca)
                    p.forca++
                }
                p.progressoForcaMax = calcularLimite(p.forca)
            }
            "RESISTENCIA" -> {
                p.progressoResistencia += ganho
                while (p.progressoResistencia >= calcularLimite(p.resistencia)) {
                    p.progressoResistencia -= calcularLimite(p.resistencia)
                    p.resistencia++
                }
                p.progressoResistenciaMax = calcularLimite(p.resistencia)
            }
            "VELOCIDADE" -> {
                p.progressoVelocidade += ganho
                while (p.progressoVelocidade >= calcularLimite(p.velocidade)) {
                    p.progressoVelocidade -= calcularLimite(p.velocidade)
                    p.velocidade++
                }
                p.progressoVelocidadeMax = calcularLimite(p.velocidade)
            }
            "INTELIGENCIA" -> {
                p.progressoInteligencia += ganho
                while (p.progressoInteligencia >= calcularLimite(p.inteligencia)) {
                    p.progressoInteligencia -= calcularLimite(p.inteligencia)
                    p.inteligencia++
                }
                p.progressoInteligenciaMax = calcularLimite(p.inteligencia)
            }
            "CARISMA" -> {
                p.progressoCarisma += ganho
                while (p.progressoCarisma >= calcularLimite(p.carisma)) {
                    p.progressoCarisma -= calcularLimite(p.carisma)
                    p.carisma++
                }
                p.progressoCarismaMax = calcularLimite(p.carisma)
            }
        }

        // 🏆 GATILHO DE CONQUISTA: ATRIBUTOS FÍSICOS
        if (atributo == "FORCA" || atributo == "RESISTENCIA" || atributo == "VELOCIDADE") {
            com.typingfrontier.collection.AchievementManager.checkPhysical(TypingFrontierApp.getAppContext())
        }
    }

    private fun verifyIntegrity() {
        val p = PlayerManager.player
        p.dinheiro = p.dinheiro.coerceAtLeast(0)
        p.energia = p.energia.coerceIn(0, p.energiaMax)
        p.cansacoMental = p.cansacoMental.coerceIn(0, p.cansacoMax)
        p.vida = p.vida.coerceIn(0, p.vidaMax)
    }
}
