package com.typingfrontier.exploration

import com.typingfrontier.Player

object ExplorationGenerator {

    private val pessoas = listOf(
        "Um aluno", "Um suspeito", "Um trabalhador", "Um desconhecido",
        "Um policial", "Um médico", "Um professor", "Um engenheiro",
        "Um vendedor", "Um morador", "Um jovem", "Um idoso",
        "Um entregador", "Um motorista", "Um segurança", "Um turista",
        "Um criminoso", "Um empresário", "Um pedestre", "Um estudante universitário"
    )

    private val situacoes = listOf(
        "está confuso", "parece nervoso", "está escondendo algo", "precisa de ajuda",
        "está sendo agressivo", "está discutindo com alguém", "está ferido",
        "está pedindo informação", "está tentando fugir", "está observando você",
        "está perdido", "está trabalhando intensamente", "está com pressa",
        "está com medo", "está gritando", "está chorando",
        "está tentando enganar alguém", "está em perigo",
        "está investigando algo", "está carregando algo suspeito"
    )

    private val locais = listOf(
        "na rua", "na escola", "na obra", "no hospital",
        "em um beco", "em uma praça", "em um mercado",
        "em um prédio abandonado", "em um escritório",
        "em um estacionamento", "em um ponto de ônibus",
        "em um shopping", "em uma delegacia", "em uma farmácia",
        "em uma estação de metrô", "em um restaurante",
        "em um bar", "em uma casa", "em um condomínio",
        "em uma avenida movimentada"
    )

    private val acoes = listOf(
        "e te chama", "e te encara", "e pede sua ajuda",
        "e tenta te evitar", "e se aproxima rapidamente",
        "e parece suspeitar de você", "e começa a falar com você",
        "e reage à sua presença", "e te observa em silêncio",
        "e toma uma atitude inesperada"
    )

    fun gerar(player: Player): EventoExploracao {

        // ------------------------------------------------
        // 🔗 CONTINUAR HISTÓRIA PARALELA
        // ------------------------------------------------
        val historiaAtiva = player.historiasAtivas.entries.randomOrNull()

        if (historiaAtiva != null && (1..100).random() <= 40) {
            return gerarHistoria(player, historiaAtiva.key, historiaAtiva.value)
        }

        // ------------------------------------------------
        // 🔗 HISTÓRIA ENCADEADA (SUSPEITO)
        // ------------------------------------------------
        if (player.eventoEncadeadoId != null) {
            return gerarEventoEncadeado(player)
        }

        val pessoa = pessoas.random()
        val situacao = situacoes.random()
        val local = locais.random()
        val acao = acoes.random()

        var descricao = "$pessoa $situacao $local $acao."

        var atributoIdeal = "INTELIGENCIA"
        var atributoSecundario = "CARISMA"

        // ------------------------------------------------
        // 🔥 INICIAR HISTÓRIA POR PROFISSÃO
        // ------------------------------------------------
        if ((1..100).random() <= 20) {

            when (player.profissao) {

                "Policial" -> player.historiasAtivas["assalto"] = 1
                "Médico" -> player.historiasAtivas["paciente"] = 1
                "Engenheiro" -> player.historiasAtivas["obra"] = 1
                "Professor" -> player.historiasAtivas["aluno"] = 1
                "Detetive" -> player.historiasAtivas["suspeito"] = 1
            }

            descricao += "\n📖 Uma situação maior começa a se desenrolar..."
        }

        // ------------------------------------------------
        // 🔥 EVENTOS BASEADOS EM REPUTAÇÃO
        // ------------------------------------------------
        when {
            player.reputacao >= 50 -> {
                descricao += "\n✨ Você é reconhecido."
                atributoIdeal = "CARISMA"
                atributoSecundario = "INTELIGENCIA"
            }
            player.reputacao <= -20 -> {
                descricao += "\n⚠️ Situação tensa."
                atributoIdeal = "FORCA"
                atributoSecundario = "RESISTENCIA"
            }
        }

        val opcoes = listOf(
            Opcao("💪 Resolver na força", "FORCA", xp = 15, dinheiro = 10, dano = 10, energia = -10),
            Opcao("🧠 Analisar", "INTELIGENCIA", xp = 12, dinheiro = 5, energia = -5),
            Opcao("🗣️ Conversar", "CARISMA", xp = 10, dinheiro = 8, energia = -5),
            Opcao("🏃 Fugir", "VELOCIDADE", energia = -3)
        )

        return EventoExploracao(
            descricao = descricao,
            opcoes = opcoes,
            atributoIdeal = atributoIdeal,
            atributoSecundario = atributoSecundario
        )
    }

    // ------------------------------------------------
    // 🔗 GERADOR DE HISTÓRIAS PARALELAS
    // ------------------------------------------------
    private fun gerarHistoria(player: Player, id: String, etapa: Int): EventoExploracao {

        return when (id) {

            "assalto" -> gerarHistoriaAssalto(player, etapa)
            "paciente" -> gerarHistoriaMedico(player, etapa)
            "obra" -> gerarHistoriaEngenheiro(player, etapa)
            "aluno" -> gerarHistoriaProfessor(player, etapa)
            "suspeito" -> gerarEventoEncadeado(player)

            else -> gerar(player)
        }
    }

    // ------------------------------------------------
    // 👮 ASSALTO (POLICIAL)
    // ------------------------------------------------
    private fun gerarHistoriaAssalto(player: Player, etapa: Int): EventoExploracao {

        return when (etapa) {

            1 -> EventoExploracao(
                descricao = "🚨 Um assalto está acontecendo.",
                opcoes = listOf(
                    Opcao("💪 Intervir", "FORCA", xp = 20),
                    Opcao("🏃 Perseguir", "VELOCIDADE", xp = 15),
                    Opcao("🧠 Observar", "INTELIGENCIA", xp = 10)
                ),
                atributoIdeal = "FORCA",
                atributoSecundario = "VELOCIDADE"
            )

            else -> {
                player.historiasAtivas.remove("assalto")
                gerar(player)
            }
        }
    }

    // ------------------------------------------------
    // ⚕️ MÉDICO
    // ------------------------------------------------
    private fun gerarHistoriaMedico(player: Player, etapa: Int): EventoExploracao {

        return when (etapa) {

            1 -> EventoExploracao(
                descricao = "⚕️ Um paciente está em estado crítico.",
                opcoes = listOf(
                    Opcao("🧠 Diagnosticar", "INTELIGENCIA", xp = 20),
                    Opcao("💪 Estabilizar", "RESISTENCIA", xp = 15)
                ),
                atributoIdeal = "INTELIGENCIA",
                atributoSecundario = "RESISTENCIA"
            )

            else -> {
                player.historiasAtivas.remove("paciente")
                gerar(player)
            }
        }
    }

    // ------------------------------------------------
    // 🏗️ ENGENHEIRO
    // ------------------------------------------------
    private fun gerarHistoriaEngenheiro(player: Player, etapa: Int): EventoExploracao {

        return when (etapa) {

            1 -> EventoExploracao(
                descricao = "🏗️ Estrutura apresenta falha.",
                opcoes = listOf(
                    Opcao("🧠 Analisar", "INTELIGENCIA", xp = 20),
                    Opcao("💪 Reforçar", "RESISTENCIA", xp = 15)
                ),
                atributoIdeal = "INTELIGENCIA",
                atributoSecundario = "RESISTENCIA"
            )

            else -> {
                player.historiasAtivas.remove("obra")
                gerar(player)
            }
        }
    }

    // ------------------------------------------------
    // 📚 PROFESSOR
    // ------------------------------------------------
    private fun gerarHistoriaProfessor(player: Player, etapa: Int): EventoExploracao {

        return when (etapa) {

            1 -> EventoExploracao(
                descricao = "📚 Um aluno está causando problemas.",
                opcoes = listOf(
                    Opcao("🗣️ Conversar", "CARISMA", xp = 20),
                    Opcao("🧠 Orientar", "INTELIGENCIA", xp = 15)
                ),
                atributoIdeal = "CARISMA",
                atributoSecundario = "INTELIGENCIA"
            )

            else -> {
                player.historiasAtivas.remove("aluno")
                gerar(player)
            }
        }
    }

    // ------------------------------------------------
    // 🔗 HISTÓRIA ENCADEADA ORIGINAL
    // ------------------------------------------------
    private fun gerarEventoEncadeado(player: Player): EventoExploracao {

        return when (player.etapaEvento) {

            1 -> EventoExploracao(
                descricao = "O suspeito reage à sua abordagem...",
                opcoes = listOf(
                    Opcao("🧠 Investigar melhor", "INTELIGENCIA", xp = 20),
                    Opcao("🗣️ Tentar diálogo", "CARISMA", xp = 15),
                    Opcao("💪 Pressionar", "FORCA", dano = 10)
                ),
                atributoIdeal = "INTELIGENCIA",
                atributoSecundario = "CARISMA"
            )

            else -> {
                player.eventoEncadeadoId = null
                player.etapaEvento = 0
                gerar(player)
            }
        }
    }
}