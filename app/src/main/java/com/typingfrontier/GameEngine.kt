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
                is GameAction.UseMedicine -> processUseMedicine()
                is GameAction.StudyError -> processStudyError(action.attribute)
                is GameAction.CollectRewards -> processCollectRewards(action.xp, action.money)
                is GameAction.BuyItem -> processBuyItem(action.item)
                is GameAction.SellItem -> processSellItem(action.item)
                is GameAction.CompleteMission -> processCompleteMission(action.xp, action.money)
                is GameAction.EquipItem -> processEquipItem(action.itemId)
                is GameAction.UnequipItem -> processUnequipItem(action.slot)
            }
            
            if (result is EngineResult.Success) {
                verifyIntegrity()
                // 🏆 GATILHO DE CONQUISTA: ECONOMIA
                com.typingfrontier.collection.AchievementManager.checkEconomy(TypingFrontierApp.getAppContext())
            }
            
            return result
        } catch (e: Exception) {
            EngineResult.Failure("Erro na Engine: ${e.message}")
        }
    }

    private fun processSellItem(item: Equipment): EngineResult {
        val p = PlayerManager.player

        // 1. Verificação de Disponibilidade para Venda
        // Regra: Equipamento equipado não pode ser vendido. Apenas o que está na mochila.
        val qtdNaMochila = p.mochila[item.id] ?: 0
        val isEquipado = p.slotsEquipados.values.contains(item.id) || p.equipamentoId == item.id

        // Bloqueio se não houver unidade disponível na mochila
        if (qtdNaMochila <= 0) {
            val msg = if (isEquipado) {
                "Este item está equipado! Desequipe-o primeiro para poder vender."
            } else {
                "Você não possui este item na mochila para vender."
            }
            return EngineResult.Failure(msg)
        }

        // 2. Cálculo do Valor de Venda (40% do preço inflacionado atual)
        val precoAtual = EconomyManager.precoInflacionado(item.preco)
        val valorVenda = Math.round(precoAtual * 0.40).toInt()

        // 3. Execução da Venda (Transação Atômica)
        // Remove uma unidade da mochila
        if (qtdNaMochila > 1) {
            p.mochila[item.id] = qtdNaMochila - 1
        } else {
            p.mochila.remove(item.id)
        }

        // Só altera o saldo após garantir a remoção do item da mochila
        p.dinheiro += valorVenda

        return EngineResult.Success("Vendido: ${item.nome} por ${CurrencyUtils.formatar(valorVenda)}!")
    }

    private fun processWork(): EngineResult {
        val p = PlayerManager.player
        if (!TimeManager.podeAgir()) return EngineResult.Failure("Muito tarde para trabalhar! Vá dormir.")
        if (p.trabalhouHoje) return EngineResult.Failure("Você já trabalhou hoje. Volte amanhã!")
        
        val custoEnergia = 35 // Invariante: Custo > Recuperação Comida
        
        // CORREÇÃO PREVENTIVA: Valida se o custo final causará desmaio
        if (p.energia - custoEnergia <= 0) {
            return EngineResult.Failure("Energia física insuficiente para trabalhar ($custoEnergia necessária).")
        }
        if (p.cansacoMental + 11 >= p.cansacoMax) {
            return EngineResult.Failure("Energia Mental insuficiente para trabalhar.")
        }

        p.trabalhouHoje = true
        p.energia -= custoEnergia
        p.cansacoMental += 11

        val ganho = ProfessionManager.calcularSalario(p)
        p.dinheiro += ganho

        var extraMsg: String? = null
        // Segurança extra: embora a validação acima deva impedir, mantemos o gatilho de hospitalização
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

        p.energia = (p.energia + config.ganhoEnergiaComida).coerceAtMost(p.energiaMax)
        return EngineResult.Success("🍴 Você comeu uma refeição de ${config.nome}.", "Energia +${config.ganhoEnergiaComida}")
    }

    private fun processSleep(): EngineResult {
        val p = PlayerManager.player
        val custoVida = 60 + (p.nivel * 10)
        
        val mentalEnergiaRestante = p.cansacoMax - p.cansacoMental
        val custoMenteEstudo = if (TimeManager.podeAgir()) 4 else (p.cansacoMax * 0.11).toInt().coerceAtLeast(1)
        val custoEnergiaEstudo = if (TimeManager.podeAgir()) 1 else (p.energiaMax * 0.1).toInt().coerceAtLeast(1)

        val temEnergiaParaQueimar = p.energia >= custoEnergiaEstudo && 
                                    mentalEnergiaRestante >= custoMenteEstudo && 
                                    p.dinheiro > custoVida

        // Identificação de ações possíveis para mensagem dinâmica
        val podeTrabalhar = p.energia - 35 > 0 && !p.trabalhouHoje && p.cansacoMental + 11 < p.cansacoMax && TimeManager.podeAgir()
        val podeEstudar = p.energia >= custoEnergiaEstudo && p.dinheiro >= 1 && mentalEnergiaRestante >= custoMenteEstudo
        val podeTreinar = TimeManager.podeAgir() && p.energia >= 10 && p.dinheiro >= 2 && mentalEnergiaRestante >= 8
        val podeExplorar = TimeManager.podeAgir() && p.energia >= 5 && mentalEnergiaRestante >= 7
        
        val listAcoes = mutableListOf<String>()
        if (podeTrabalhar) listAcoes.add("trabalhar")
        if (podeTreinar) listAcoes.add("treinar")
        if (podeEstudar) listAcoes.add("estudar")
        if (podeExplorar) listAcoes.add("explorar")
        
        val acoesSugeridas = when {
            listAcoes.isEmpty() -> null
            listAcoes.size == 1 -> listAcoes.first()
            else -> listAcoes.dropLast(1).joinToString(", ") + " ou " + listAcoes.last()
        }
        
        if (temEnergiaParaQueimar) {
            val sugestao = if (acoesSugeridas != null) " Que tal $acoesSugeridas mais um pouco antes de dormir?" else ""
            return EngineResult.Failure("Você ainda tem disposição!$sugestao")
        }

        val estaTravado = !podeTrabalhar && !podeEstudar && p.pausouHoje
        val podeDormir = !TimeManager.podeAgir() || p.energia < 5 || estaTravado
        
        if (!podeDormir) {
            val prefixo = "Você ainda tem fôlego!"
            val aviso = if (TimeManager.podeAgir()) {
                if (acoesSugeridas != null) " O dia só acaba às 22:00. Aproveite para $acoesSugeridas!" 
                else " O dia só acaba às 22:00. Aproveite o tempo restante!"
            } else {
                if (acoesSugeridas != null) " Tente $acoesSugeridas."
                else " Aproveite para descansar ou revisar seu status."
            }
            return EngineResult.Failure("$prefixo$aviso")
        }
        
        // REDE DE SEGURANÇA 2: Sono na rua (Penalidade Real)
        if (p.dinheiro < custoVida) {
            p.trabalhouHoje = false
            p.pausouHoje = false
            p.energia = (p.energiaMax * 0.3).toInt() 
            p.vida = (p.vida - 20).coerceAtLeast(10)
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
        var traumaRecuperado = false
        if (p.traumasAcumulados > 0) {
            p.diasParaRecuperarTrauma--
            if (p.diasParaRecuperarTrauma <= 0) {
                p.traumasAcumulados--
                traumaRecuperado = true
                if (p.traumasAcumulados > 0) {
                    p.diasParaRecuperarTrauma = 2
                }
            }
        }
        
        TimeManager.resetarDia() 

        val extra = StringBuilder("Energia e Energia Mental restauradas.")
        if (traumaRecuperado) {
            extra.append("\n")
            if (p.traumasAcumulados == 0) {
                extra.append("Seu corpo se recuperou completamente dos traumas.")
            } else {
                extra.append("Seu corpo se recuperou de um trauma. Ainda há ${p.traumasAcumulados} em recuperação.")
            }
        } else if (p.traumasAcumulados > 0) {
            extra.append("\nSeu corpo continua se recuperando (Faltam ${p.diasParaRecuperarTrauma} dias para o próximo).")
        }

        return EngineResult.Success("😴 Você dormiu bem. Pago ${CurrencyUtils.formatar(custoVida)} pela hospedagem.", extra.toString())
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

    private fun processUseMedicine(): EngineResult {
        val p = PlayerManager.player
        if (p.estoqueMedicamento <= 0) return EngineResult.Failure("Você não possui Medicamento no estoque.")
        if (p.traumasAcumulados <= 0) return EngineResult.Failure("Seu corpo não possui traumas para tratar com Medicamento.")

        val msg = applyMedicineTreatment(isAutomatic = false)
        return EngineResult.Success(msg)
    }

    /**
     * Aplica o tratamento de um Medicamento.
     * Consome 1 unidade, remove 1 trauma e recupera HP/XP/Atributos conforme regras atuais.
     */
    fun applyMedicineTreatment(isAutomatic: Boolean): String {
        val p = PlayerManager.player
        
        p.estoqueMedicamento--
        p.traumasAcumulados--
        
        val prefix = if (isAutomatic) "🛡️ Medicamento utilizado automaticamente!\n" else ""
        val sb = StringBuilder("${prefix}💊 1 Trauma foi tratado.")
        if (p.traumasAcumulados > 0) {
            sb.append(" (Restam ${p.traumasAcumulados})")
        } else {
            sb.append("\n✅ Todos os traumas foram curados!")
        }
        
        // 1. REGRA DE HP
        val vidaAntes = p.vida
        if (p.traumasAcumulados > 0) {
            val alvoHP = (p.vidaMax * 0.2).toInt()
            p.vida = Math.max(p.vida, alvoHP)
            if (p.vida > vidaAntes) {
                sb.append("\n❤️ Vida recuperada: +${p.vida - vidaAntes} HP.")
            }
            sb.append("\n⚠️ A recuperação total da Vida exige tratar os traumas restantes.")
        } else {
            p.vida = p.vidaMax
            sb.append("\n❤️ Vida totalmente restaurada! (+${p.vida - vidaAntes} HP)")
        }

        // 2. RECUPERAÇÃO DE XP (50% da perda real)
        val xpRecuperado = (p.perdaXpRecuperavel * 0.5).toInt()
        if (xpRecuperado > 0) {
            val niveisGanhos = PlayerManager.ganharXp(xpRecuperado)
            p.perdaXpRecuperavel -= xpRecuperado
            sb.append("\n✨ +${CurrencyUtils.formatar(xpRecuperado)} de Experiência recuperada.")
            if (niveisGanhos > 0) {
                sb.append("\n🎉 LEVEL UP! Você subiu para o Nível ${p.nivel}!")
            }
        }

        // 3. RECUPERAÇÃO DE ATRIBUTOS (50% da perda real individual)
        val atributosParaRecuperar = p.perdaAtribRecuperavel.filter { it.value > 0 }
        if (atributosParaRecuperar.isNotEmpty()) {
            sb.append("\n\n💪 Vigor dos Atributos:")
            atributosParaRecuperar.forEach { (nome, perda) ->
                val rec = (perda * 0.5).toInt()
                if (rec > 0) {
                    val valorAntes = when(nome) {
                        "FORCA" -> p.forca
                        "VELOCIDADE" -> p.velocidade
                        "RESISTENCIA" -> p.resistencia
                        "CARISMA" -> p.carisma
                        "INTELIGENCIA" -> p.inteligencia
                        else -> 0
                    }
                    
                    applyAttributeXP(nome, rec)
                    
                    val valorDepois = when(nome) {
                        "FORCA" -> p.forca
                        "VELOCIDADE" -> p.velocidade
                        "RESISTENCIA" -> p.resistencia
                        "CARISMA" -> p.carisma
                        "INTELIGENCIA" -> p.inteligencia
                        else -> 0
                    }
                    
                    val nomeExibicao = when(nome) {
                        "FORCA" -> "Força"
                        "VELOCIDADE" -> "Velocidade"
                        "RESISTENCIA" -> "Resistência"
                        "CARISMA" -> "Carisma"
                        "INTELIGENCIA" -> "Inteligência"
                        else -> nome
                    }
                    
                    if (valorDepois > valorAntes) {
                        sb.append("\n• $nomeExibicao: +${valorDepois - valorAntes} (Total: $valorDepois)")
                    } else {
                        sb.append("\n• $nomeExibicao: Reabilitado")
                    }
                    
                    p.perdaAtribRecuperavel[nome] = perda - rec
                }
            }
        }
        
        // Verificação final de dívida
        val xpRestante = p.perdaXpRecuperavel
        val atribRestante = p.perdaAtribRecuperavel.values.sum()
        
        if (xpRestante <= 0 && atribRestante <= 0) {
            if (xpRecuperado > 0 || atributosParaRecuperar.isNotEmpty()) {
                sb.append("\n\n✨ Todo o progresso perdido foi restaurado!")
            }
        } else {
            sb.append("\n\n💊 Mais doses podem ser usadas para recuperar o restante do progresso.")
        }
        
        return sb.toString()
    }

    private fun processStudyError(atributo: String): EngineResult {
        val p = PlayerManager.player
        p.energia -= 1
        p.cansacoMental += 1
        return EngineResult.Success("Erro no estudo. Cansaço acumulado.")
    }

    private fun processCollectRewards(xp: Int, money: Int): EngineResult {
        val p = PlayerManager.player
        val vidaAntiga = p.vida
        val energiaAntiga = p.energia
        val nivelAntigo = p.nivel
        
        val niveisGanhos = PlayerManager.ganharXp(xp)
        p.dinheiro += money
        
        if (niveisGanhos > 0) {
            val extra = "Você subiu de nível ($nivelAntigo → ${p.nivel})!\n" +
                        "❤️ Vida recuperada: $vidaAntiga → ${p.vida}\n" +
                        "⚡ Energia Física recuperada: $energiaAntiga → ${p.energia}\n" +
                        "🧠 Energia Mental recuperada parcialmente." +
                        (if (p.traumasAcumulados > 0) "\n🩹 Os traumas ainda exigem descanso." else "")
            
            return EngineResult.Success("🏆 Nível Up! +$niveisGanhos nível(is) e recompensa coletada.", extra)
        }
        
        return EngineResult.Success("Recompensas coletadas com sucesso!")
    }

    private fun processBuyItem(item: Equipment): EngineResult {
        val p = PlayerManager.player

        // 1. Verificação de Nível Mínimo
        if (p.nivel < item.nivelMinimo) {
            return EngineResult.Failure("Nível ${item.nivelMinimo} necessário para este equipamento.")
        }

        // Lógica de Preço (Itens especiais têm preço já calculado por nível, outros usam inflação)
        val precoFinal = if (item.id == "blessing" || item.id == "medicine") item.preco else EconomyManager.precoInflacionado(item.preco)

        // 2. Verificação de Saldo
        if (p.dinheiro < precoFinal) {
            val falta = precoFinal - p.dinheiro
            return EngineResult.Failure("Frons insuficientes. Falta ${CurrencyUtils.formatar(falta)}.")
        }

        // 3. Execução da Compra
        if (item.id == "blessing") {
            if (p.estoqueBencao >= p.limiteTraumas) return EngineResult.Failure("Limite de Seguros atingido (${p.limiteTraumas}).")
            p.estoqueBencao++
            p.dinheiro -= precoFinal
            return EngineResult.Success("Seguro de Equipamentos adquirido por ${CurrencyUtils.formatar(precoFinal)}!")
        } else if (item.id == "medicine") {
            if (p.estoqueMedicamento >= p.limiteTraumas) return EngineResult.Failure("Limite de Medicamentos atingido (${p.limiteTraumas}).")
            p.estoqueMedicamento++
            p.dinheiro -= precoFinal
            return EngineResult.Success("Medicamento adquirido por ${CurrencyUtils.formatar(precoFinal)}!")
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
        val p = PlayerManager.player
        val vidaAntiga = p.vida
        val nivelAntigo = p.nivel
        
        val niveisGanhos = PlayerManager.ganharXp(xp)
        p.dinheiro += money
        
        if (niveisGanhos > 0) {
            val extra = "🏆 Nível Up! ($nivelAntigo → ${p.nivel})\n" +
                        "❤️ Vida: $vidaAntiga → ${p.vida}\n" +
                        (if (p.traumasAcumulados > 0) "🩹 Traumas permanecem em recuperação." else "")
            return EngineResult.Success("Missão concluída com sucesso!", extra)
        }

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
        
        // Proteção extra para mochila
        val chavesInvalidas = p.mochila.filter { it.value < 0 }.keys
        chavesInvalidas.forEach { p.mochila.remove(it) }
    }
}
