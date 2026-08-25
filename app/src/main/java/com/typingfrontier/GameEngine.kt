package com.typingfrontier

import com.typingfrontier.economy.Equipment
import com.typingfrontier.economy.ProfessionManager

/**
 * O Cérebro do Jogo. Centraliza todas as regras de negócio e validações.
 */
object GameEngine {

    fun dispatch(action: GameAction): EngineResult {
        return try {
            val result = when (action) {
                is GameAction.Work -> processWork()
                is GameAction.Eat -> processEat()
                is GameAction.Sleep -> processSleep()
                is GameAction.Rest -> processRest()
                is GameAction.Train -> processTrain(action.attribute, action.intensity)
                is GameAction.Explore -> processExplore(action.zoneId)
                is GameAction.StudyError -> processStudyError(action.attribute)
                is GameAction.CollectRewards -> processCollectRewards(action.xp, action.money)
                is GameAction.BuyItem -> processBuyItem(action.item)
                is GameAction.CompleteMission -> processCompleteMission(action.xp, action.money)
            }
            verifyIntegrity()
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
        
        if (p.energia < custoEnergia) return EngineResult.Failure("Energia insuficiente ($custoEnergia necessária).")
        if (p.cansacoMental >= p.cansacoMax * 0.95) return EngineResult.Failure("Mente esgotada! Você precisa descansar.")

        p.trabalhouHoje = true
        p.energia -= custoEnergia
        p.cansacoMental += 11

        val ganho = ProfessionManager.calcularSalario(p)
        p.dinheiro += ganho

        var extraMsg: String? = null
        if (p.cansacoMental >= p.cansacoMax || p.energia <= 0) {
            extraMsg = ProfessionManager.hospitalizar(p)
        }

        return EngineResult.Success("💼 Trabalho concluído! Ganhou R$ $ganho.", extraMsg)
    }

    private fun processEat(): EngineResult {
        val p = PlayerManager.player
        val config = ProfessionManager.getConfig(p.profissao) ?: return EngineResult.Failure("Erro de perfil.")

        if (p.dinheiro < config.custoComida) return EngineResult.Failure("Dinheiro insuficiente (R$ ${config.custoComida}).")
        if (p.energia >= p.energiaMax) return EngineResult.Failure("Você já está satisfeito.")
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
            return EngineResult.Failure("Você ainda tem energia! Aproveite para estudar mais um pouco e cansar sua mente antes de dormir. (O esforço extra consumirá 10% de seus recursos máximos).")
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
            return EngineResult.Success("🏚️ Você dormiu na rua por falta de dinheiro.", "Energia: 30% | Vida: -20 HP. Dia ${p.dia} começou.")
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
        if (p.dinheiro < custoDinheiro) return EngineResult.Failure("Dinheiro insuficiente (R$ $custoDinheiro).")
        if (p.energia < gastoEnergia) return EngineResult.Failure("Energia insuficiente ($gastoEnergia necessária).")
        
        if (p.cansacoMax - p.cansacoMental < gastoMente) return EngineResult.Failure("Mente exausta! Você não consegue focar no treino.")

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
        return EngineResult.Success("${if (isMental) "Estudo" else "Treino"} concluído!", "+$ganhoXP XP")
    }

    private fun processExplore(zoneId: String): EngineResult {
        val p = PlayerManager.player

        if (!TimeManager.podeAgir()) return EngineResult.Failure("Muito tarde para explorar.")
        if (p.energia < 5) return EngineResult.Failure("Energia insuficiente (necessário 5).")
        if (p.cansacoMax - p.cansacoMental < 7) return EngineResult.Failure("Mente exausta! Você não consegue focar na exploração.")
        
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
        val itemAtual = ProfessionManager.getEquipment(p.equipamentoId)

        // 1. Proteção: Impede compra do mesmo item
        if (p.equipamentoId == item.id) {
            return EngineResult.Failure("Você já possui o item ${item.nome} equipado!")
        }

        // 2. Lógica de Crédito de Troca (40% do valor base do item atual)
        val valorNovo = if (item.id == "blessing") item.preco else EconomyManager.precoInflacionado(item.preco)
        
        var creditoTroca = 0
        if (item.id != "blessing" && itemAtual != null) {
            creditoTroca = (itemAtual.preco * 0.4).toInt()
        }

        val precoFinal = (valorNovo - creditoTroca).coerceAtLeast(0)

        // 3. Verificação de Saldo
        if (p.dinheiro < precoFinal) {
            val falta = precoFinal - p.dinheiro
            return EngineResult.Failure("Dinheiro insuficiente. Falta R$ $falta.")
        }

        // 4. Execução da Compra
        if (item.id == "blessing") {
            if (p.temBlessing) return EngineResult.Failure("Você já possui uma benção ativa.")
            p.temBlessing = true
            p.dinheiro -= precoFinal
            return EngineResult.Success("Benção adquirida por R$ $precoFinal!")
        } else {
            p.dinheiro -= precoFinal
            p.equipamentoId = item.id
            val msgSucesso = if (creditoTroca > 0) {
                "Equipado: ${item.nome}. Crédito de R$ $creditoTroca recebido pelo item antigo."
            } else {
                "Equipado: ${item.nome}!"
            }
            return EngineResult.Success(msgSucesso)
        }
    }

    private fun processCompleteMission(xp: Int, money: Int): EngineResult {
        PlayerManager.ganharXp(xp)
        PlayerManager.player.dinheiro += money
        return EngineResult.Success("Missão concluída com sucesso!")
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
    }

    private fun verifyIntegrity() {
        val p = PlayerManager.player
        p.dinheiro = p.dinheiro.coerceAtLeast(0)
        p.energia = p.energia.coerceIn(0, p.energiaMax)
        p.cansacoMental = p.cansacoMental.coerceIn(0, p.cansacoMax)
        p.vida = p.vida.coerceIn(0, p.vidaMax)
    }
}
