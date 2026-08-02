package com.typingfrontier.exploration

import com.typingfrontier.Player
import com.typingfrontier.economy.ProfessionManager
import kotlin.random.Random

object ExplorationManager {

    fun calcularSucesso(player: Player, zona: ExplorationZone, etapa: Int): Boolean {
        // Agora suportamos múltiplos atributos, priorizando o foco principal
        val v1 = getValorAtributo(player, zona.atributoPrincipal)
        val v2 = getValorAtributo(player, zona.atributoSecundario)
        val v3 = getValorAtributo(player, zona.atributoTerciario)
        
        // Atributo total = 100% do principal + 30% do secundário + 20% do terciário
        // Isso valoriza o mental (principal) e dá bônus físico estratégico (apoio)
        val atributoValor = (v1 + (v2 * 0.3) + (v3 * 0.2)).toInt().coerceAtLeast(1)

        // Dificuldade reduzida na Etapa 1 para ser mais intuitivo
        // Dificuldade = Base da zona + (etapa * 8) - bônus inicial de etapa 1
        var dificuldade = zona.riscoBase + (etapa * 8)
        if (etapa == 1) dificuldade -= 10 // Torna a primeira etapa muito fácil
        
        // Mecânica Noturna: Mais perigoso à noite (após as 18:00)
        if (player.hora >= 18 || player.hora < 6) {
            dificuldade += 15
        }

        // Poder do jogador: Atributo + Sorte (1-20)
        // A FÓRMULA DE SUCESSO PERMANECE IDÊNTICA
        val sorte = Random.nextInt(1, 21)
        val poderTotal = (atributoValor * 2) + sorte

        // Passiva Policial: 20% de chance de converter falha em sucesso por instinto
        if (player.profissao == "Policial" && poderTotal < dificuldade) {
            if (Random.nextInt(1, 101) <= 20) return true
        }

        return poderTotal >= dificuldade
    }

    private fun getValorAtributo(player: Player, nome: String?): Int {
        return when (nome) {
            "FORCA" -> player.forca
            "INTELIGENCIA" -> player.inteligencia
            "CARISMA" -> player.carisma
            "RESISTENCIA" -> player.resistencia
            "VELOCIDADE" -> player.velocidade
            else -> 0
        }
    }

    fun gerarRecompensa(player: Player, zona: ExplorationZone, etapa: Int): Map<String, Int> {
        // Multiplicador Progressivo: 
        // Etapa 1: 1.0x
        // Etapa 2: 1.5x
        // Etapa 3: 2.2x
        // Etapa 4: 3.2x
        // Etapa 5: 4.5x
        val multiplicador = when (etapa) {
            1 -> 1.0
            2 -> 1.5
            3 -> 2.2
            4 -> 3.2
            5 -> 4.5
            else -> 1.0
        }
        
        var xp = (zona.recompensaBaseXp * multiplicador).toInt()
        var dinheiro = (zona.recompensaBaseDinheiro * multiplicador).toInt()

        // Bônus Noturno: +50% XP e Dinheiro
        if (player.hora >= 18 || player.hora < 6) {
            xp = (xp * 1.5).toInt()
            dinheiro = (dinheiro * 1.5).toInt()
        }

        // Passiva Médico: Ganha 20% mais XP em exploração
        if (player.profissao == "Médico") xp = (xp * 1.2).toInt()
        
        // Passiva Professor: Ganha 30% mais dinheiro (negociação)
        if (player.profissao == "Professor") dinheiro = (dinheiro * 1.3).toInt()

        return mapOf("xp" to xp, "dinheiro" to dinheiro)
    }

    fun verificarItemRaro(player: Player, zona: ExplorationZone, etapa: Int): String? {
        var chance = zona.chanceItemRaro + (etapa * 2)
        
        // Passiva Detetive: Dobra a chance de achar itens raros
        if (player.profissao == "Detetive") chance *= 2
        
        return if (Random.nextInt(1, 101) <= chance) {
            "Relíquia de ${zona.nome}" // Por enquanto um nome genérico
        } else null
    }

    fun processarFalhaCritica(player: Player): String {
        // Mensagem personalizada baseada em imperícia profissional
        val causaMorte = when(player.profissao) {
            "Engenheiro" -> listOf(
                "Um erro nos cálculos de carga fez a estrutura colapsar sobre você.",
                "Imperícia ao manipular circuitos causou uma explosão elétrica.",
                "Você ignorou um aviso de segurança e sofreu um acidente grave."
            ).random()
            "Policial" -> listOf(
                "Uma falha tática na abordagem deixou você vulnerável a uma emboscada.",
                "Seu instinto falhou e você foi cercado por criminosos.",
                "Um erro no procedimento de contenção resultou em ferimentos graves."
            ).random()
            "Médico" -> listOf(
                "Você se expôs a agentes biológicos letais sem a proteção devida.",
                "Um erro no diagnóstico do ambiente te levou a uma zona tóxica.",
                "Ao tentar ajudar, você se tornou a próxima vítima do incidente."
            ).random()
            "Detetive" -> listOf(
                "Você seguiu a pista falsa e caiu direto em uma armadilha mortal.",
                "Sua investigação te levou a pessoas que não queriam ser encontradas.",
                "Um detalhe ignorado foi o seu fim: você foi pego desprevenido."
            ).random()
            "Professor" -> listOf(
                "As palavras falharam e a multidão se tornou agressiva.",
                "Sua análise social estava errada e você entrou em um conflito sem saída.",
                "O estresse da negociação causou um colapso nervoso e físico."
            ).random()
            else -> "Você não teve habilidade suficiente para sobreviver ao perigo."
        }

        // Passiva Policial: Chance de fugir sem hospitalização
        if (player.profissao == "Policial" && Random.nextInt(1, 101) <= 40) {
            player.energia = (player.energia - 30).coerceAtLeast(1)
            return "🏃 $causaMorte\n\nMas seu treinamento policial permitiu que você fugisse antes do pior! Perdeu muita energia."
        }
        
        // Punição estilo Tibia
        val msgHospital = ProfessionManager.hospitalizar(player)
        return "💀 $causaMorte\n\n$msgHospital"
    }

    fun gerarDescricaoSucesso(profissao: String, etapa: Int, zona: ExplorationZone): String {
        val local = zona.ambiente
        return when (profissao) {
            "Engenheiro" -> when (etapa) {
                1 -> "Você encontrou um equipamento danificado $local e iniciou a análise."
                2 -> "Você identificou a falha elétrica $local e iniciou o reparo."
                3 -> "O equipamento voltou a funcionar $local e revelou componentes valiosos."
                4 -> "Você desmontou o restante da estrutura $local e recuperou mais materiais."
                5 -> "Você concluiu toda a manutenção $local e aproveitou tudo o que era útil."
                else -> "Você reparou um dispositivo $local e extraiu componentes caros."
            }
            "Policial" -> when (etapa) {
                1 -> "Você iniciou uma patrulha tática $local e identificou rastros suspeitos."
                2 -> "Você localizou o foco da atividade $local e planejou uma abordagem segura."
                3 -> "A intervenção foi um sucesso e você neutralizou as ameaças $local."
                4 -> "Você realizou uma varredura $local e apreendeu evidências valiosas."
                5 -> "O perímetro $local foi totalmente pacificado, permitindo uma retirada segura."
                else -> "Você neutralizou um perigo $local e coletou evidências."
            }
            "Médico" -> when (etapa) {
                1 -> "Você identificou sinais de contaminação $local e iniciou a triagem."
                2 -> "Você aplicou procedimentos $local e estabilizou os riscos imediatos."
                3 -> "Você coletou amostras raras $local que serão cruciais para a medicina."
                4 -> "Com a situação sob controle, você recuperou suprimentos $local."
                5 -> "Você concluiu a sanitização $local e salvou vidas antes de sair."
                else -> "Você encontrou amostras valiosas $local e manteve sua saúde."
            }
            "Professor" -> when (etapa) {
                1 -> "Você estabeleceu os primeiros contatos $local e ganhou a confiança de todos."
                2 -> "Você mediou conflitos $local entre grupos, obtendo informações cruciais."
                3 -> "Sua autoridade intelectual $local permitiu acesso a registros esquecidos."
                4 -> "Você instruiu os presentes $local sobre autodefesa e foi recompensado."
                5 -> "Seu impacto $local foi profundo, e você partiu deixando um legado de ordem."
                else -> "Você convenceu os locais e ganhou informações preciosas."
            }
            "Detetive" -> when (etapa) {
                1 -> "Você encontrou a primeira pista $local e começou a traçar um perfil da área."
                2 -> "Você conectou evidências $local e descobriu uma rota de acesso restrita."
                3 -> "Sua dedução te levou a um esconderijo de recursos escondido $local."
                4 -> "Você decifrou os enigmas $local, expondo segredos guardados há anos."
                5 -> "O caso $local foi encerrado com sucesso e você recuperou itens valiosos."
                else -> "Você ligou os pontos $local e achou um esconderijo de recursos."
            }
            else -> "Você obteve sucesso na sua busca $local."
        }
    }
}
