package com.typingfrontier

import android.content.Context
import android.content.SharedPreferences

object PlayerManager {
    var player = Player()

    private const val PREFS_NAME = "typing_frontier_save"
    private const val CURRENT_SAVE_VERSION = 6

    // Constantes para a fórmula de XP (Opção C: Polinomial Híbrida)
    private const val XP_BASE = 20
    private const val XP_LINEAR_MULT = 15
    private const val XP_EXP_MULT = 8
    private const val XP_EXPONENT = 1.5

    fun calcularXpParaProximoNivel(nivel: Int): Int {
        val calculo = XP_BASE + (nivel * XP_LINEAR_MULT) + (Math.pow(nivel.toDouble(), XP_EXPONENT) * XP_EXP_MULT)
        return calculo.toInt()
    }

    /**
     * Adiciona experiência ao jogador e processa subida de nível.
     * Retorna o número de níveis ganhos.
     */
    fun ganharXp(xp: Int): Int {
        player.experienciaAtual += xp
        var niveisGanhos = 0

        while (player.experienciaAtual >= player.experienciaParaProximoNivel) {
            player.experienciaAtual -= player.experienciaParaProximoNivel
            player.nivel++
            niveisGanhos++
            
            aplicarConsequenciasLevelUp()
        }
        
        return niveisGanhos
    }

    /**
     * Reduz o nível do jogador (Punição Tibiana).
     */
    fun reduzirNivel() {
        if (player.nivel > 1) {
            player.nivel--
            player.experienciaAtual = 0
            
            // Recalcula o limite para o nível menor
            player.experienciaParaProximoNivel = calcularXpParaProximoNivel(player.nivel)
        }
    }

    /**
     * Aplica os bônus de status ao subir de nível.
     */
    private fun aplicarConsequenciasLevelUp() {
        // Recalcula o próximo limite
        player.experienciaParaProximoNivel = calcularXpParaProximoNivel(player.nivel)

        evoluirAtributosMaximos()

        // Efeitos imediatos do Level Up (Diferente da migração)
        player.vida = player.vidaMax
        player.energia = player.energiaMax
        
        // Correção de Exploit: Level Up não reseta mais o cansaço mental para 0.
        // Agora ele apenas reduz em 20% do máximo, recompensando o jogador sem quebrar o ciclo de sono/descanso.
        player.cansacoMental = (player.cansacoMental - (player.cansacoMax * 0.2).toInt()).coerceAtLeast(0)
    }

    /**
     * Centraliza a lógica de ganho de atributos por nível para evitar duplicação.
     * Usado tanto no Level Up quanto na Migração de Save.
     */
    private fun evoluirAtributosMaximos() {
        val ganhoVida = if (player.profissao == "Policial") 15 else 10
        player.vidaMax += ganhoVida

        val ganhoEnergia = if (player.profissao == "Policial" || player.profissao == "Engenheiro") 10 else 6
        player.energiaMax += ganhoEnergia

        // A Mente agora cresce sempre na proporção de ~50% da Energia
        // Médico/Professor ganham 5, as demais profissões ganham a metade do ganho de energia (3 ou 5)
        val ganhoMente = if (player.profissao == "Médico" || player.profissao == "Professor") 5 else (ganhoEnergia / 2)
        player.cansacoMax += ganhoMente
    }

    fun save(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putInt("saveVersion", CURRENT_SAVE_VERSION)

        editor.putString("nome", player.nome)
        editor.putString("sexo", player.sexo)
        editor.putString("profissao", player.profissao)
        editor.putString("cidadeNascimento", player.cidadeNascimento)

        editor.putInt("nivel", player.nivel)
        editor.putInt("experienciaAtual", player.experienciaAtual)
        editor.putInt("experienciaParaProximoNivel", player.experienciaParaProximoNivel)

        editor.putInt("vida", player.vida)
        editor.putInt("vidaMax", player.vidaMax)
        editor.putInt("energia", player.energia)
        editor.putInt("energiaMax", player.energiaMax)
        editor.putInt("reputacao", player.reputacao)

        editor.putBoolean("ajudouAlguem", player.ajudouAlguem)
        editor.putBoolean("falhouMuito", player.falhouMuito)
        editor.putBoolean("entrouEmConfusao", player.entrouEmConfusao)
        editor.putInt("eventosPositivos", player.eventosPositivos)
        editor.putInt("eventosNegativos", player.eventosNegativos)

        editor.putString("eventoEncadeadoId", player.eventoEncadeadoId)
        editor.putInt("etapaEvento", player.etapaEvento)
        editor.putString("escolhaAnterior", player.escolhaAnterior)

        editor.putInt("dinheiro", player.dinheiro)

        editor.putInt("forca", player.forca)
        editor.putInt("velocidade", player.velocidade)
        editor.putInt("resistencia", player.resistencia)
        editor.putInt("inteligencia", player.inteligencia)
        editor.putInt("carisma", player.carisma)

        editor.putString("especializacaoPortugues", player.especializacaoPortugues?.name)

        editor.putInt("cansacoMental", player.cansacoMental)
        editor.putInt("cansacoMax", player.cansacoMax)

        editor.putInt("progressoInteligencia", player.progressoInteligencia)
        editor.putInt("progressoCarisma", player.progressoCarisma)
        editor.putInt("progressoForca", player.progressoForca)
        editor.putInt("progressoResistencia", player.progressoResistencia)
        editor.putInt("progressoVelocidade", player.progressoVelocidade)

        editor.putInt("progressoInteligenciaMax", player.progressoInteligenciaMax)
        editor.putInt("progressoCarismaMax", player.progressoCarismaMax)
        editor.putInt("progressoForcaMax", player.progressoForcaMax)
        editor.putInt("progressoResistenciaMax", player.progressoResistenciaMax)
        editor.putInt("progressoVelocidadeMax", player.progressoVelocidadeMax)

        editor.putString("equipamentoId", player.equipamentoId)
        editor.putBoolean("temBlessing", player.temBlessing)
        editor.putBoolean("introConcluida", player.introConcluida)

        editor.putBoolean("trabalhouHoje", player.trabalhouHoje)
        editor.putBoolean("pausouHoje", player.pausouHoje)

        editor.putInt("dia", player.dia)
        editor.putInt("hora", player.hora)
        editor.putInt("minuto", player.minuto)

        editor.putInt("traumasAcumulados", player.traumasAcumulados)
        editor.putInt("diasParaRecuperarTrauma", player.diasParaRecuperarTrauma)

        // 🏆 COLEÇÃO E CONQUISTAS
        editor.putString("avatarEquipadoId", player.avatarEquipadoId)
        editor.putStringSet("avataresDesbloqueados", player.avataresDesbloqueados)
        editor.putStringSet("conquistasDesbloqueadas", player.conquistasDesbloqueadas)
        
        // 📊 ESTATÍSTICAS PARA CONQUISTAS
        editor.putInt("mentalStreak", player.mentalStreak)
        editor.putStringSet("zonasExploradas", player.zonasExploradas)
        
        // Save avataresProgressoAds Map
        val progressoStr = player.avataresProgressoAds.entries.joinToString(";") { "${it.key}:${it.value}" }
        editor.putString("avataresProgressoAds", progressoStr)

        // Save historiasAtivas Map
        val historiasStr = player.historiasAtivas.entries.joinToString(";") { "${it.key}:${it.value}" }
        editor.putString("historiasAtivas", historiasStr)

        editor.apply()
    }

    fun load(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (!prefs.contains("nome")) return // No save found

        val loadedVersion = prefs.getInt("saveVersion", 1)

        player.nome = prefs.getString("nome", "") ?: ""
        player.sexo = prefs.getString("sexo", "") ?: ""
        player.profissao = prefs.getString("profissao", "") ?: ""
        player.cidadeNascimento = prefs.getString("cidadeNascimento", "São Paulo") ?: "São Paulo"

        player.nivel = prefs.getInt("nivel", 1)
        player.experienciaAtual = prefs.getInt("experienciaAtual", 0)
        player.experienciaParaProximoNivel = prefs.getInt("experienciaParaProximoNivel", 20)

        player.vida = prefs.getInt("vida", 100)
        player.vidaMax = prefs.getInt("vidaMax", 100)
        player.energia = prefs.getInt("energia", 100)
        player.energiaMax = prefs.getInt("energiaMax", 100)
        player.reputacao = prefs.getInt("reputacao", 0)

        player.ajudouAlguem = prefs.getBoolean("ajudouAlguem", false)
        player.falhouMuito = prefs.getBoolean("falhouMuito", false)
        player.entrouEmConfusao = prefs.getBoolean("entrouEmConfusao", false)
        player.eventosPositivos = prefs.getInt("eventosPositivos", 0)
        player.eventosNegativos = prefs.getInt("eventosNegativos", 0)

        player.eventoEncadeadoId = prefs.getString("eventoEncadeadoId", null)
        player.etapaEvento = prefs.getInt("etapaEvento", 0)
        player.escolhaAnterior = prefs.getString("escolhaAnterior", null)

        player.dinheiro = prefs.getInt("dinheiro", 100)

        player.forca = prefs.getInt("forca", 1)
        player.velocidade = prefs.getInt("velocidade", 1)
        player.resistencia = prefs.getInt("resistencia", 1)
        player.inteligencia = prefs.getInt("inteligencia", 1)
        player.carisma = prefs.getInt("carisma", 1)

        val especStr = prefs.getString("especializacaoPortugues", null)
        player.especializacaoPortugues = if (especStr != null) {
            try { EspecializacaoPortugues.valueOf(especStr) } catch (e: Exception) { null }
        } else null

        player.cansacoMental = prefs.getInt("cansacoMental", 0)
        player.cansacoMax = prefs.getInt("cansacoMax", 50)

        player.progressoInteligencia = prefs.getInt("progressoInteligencia", 0)
        player.progressoCarisma = prefs.getInt("progressoCarisma", 0)
        player.progressoForca = prefs.getInt("progressoForca", 0)
        player.progressoResistencia = prefs.getInt("progressoResistencia", 0)
        player.progressoVelocidade = prefs.getInt("progressoVelocidade", 0)

        player.progressoInteligenciaMax = prefs.getInt("progressoInteligenciaMax", 100)
        player.progressoCarismaMax = prefs.getInt("progressoCarismaMax", 100)
        player.progressoForcaMax = prefs.getInt("progressoForcaMax", 100)
        player.progressoResistenciaMax = prefs.getInt("progressoResistenciaMax", 100)
        player.progressoVelocidadeMax = prefs.getInt("progressoVelocidadeMax", 100)

        player.equipamentoId = prefs.getString("equipamentoId", null)
        player.temBlessing = prefs.getBoolean("temBlessing", false)
        player.introConcluida = prefs.getBoolean("introConcluida", false)

        player.trabalhouHoje = prefs.getBoolean("trabalhouHoje", false)
        player.pausouHoje = prefs.getBoolean("pausouHoje", false)

        player.dia = prefs.getInt("dia", 1)
        player.hora = prefs.getInt("hora", 8)
        player.minuto = prefs.getInt("minuto", 0)

        player.traumasAcumulados = prefs.getInt("traumasAcumulados", 0)
        player.diasParaRecuperarTrauma = prefs.getInt("diasParaRecuperarTrauma", 0)

        // 🏆 COLEÇÃO E CONQUISTAS
        player.avatarEquipadoId = prefs.getString("avatarEquipadoId", null)
        player.avataresDesbloqueados = prefs.getStringSet("avataresDesbloqueados", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        player.conquistasDesbloqueadas = prefs.getStringSet("conquistasDesbloqueadas", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // 📊 ESTATÍSTICAS PARA CONQUISTAS
        player.mentalStreak = prefs.getInt("mentalStreak", 0)
        player.zonasExploradas = prefs.getStringSet("zonasExploradas", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val progressoStr = prefs.getString("avataresProgressoAds", "") ?: ""
        player.avataresProgressoAds.clear()
        if (progressoStr.isNotEmpty()) {
            progressoStr.split(";").forEach {
                val parts = it.split(":")
                if (parts.size == 2) {
                    player.avataresProgressoAds[parts[0]] = parts[1].toIntOrNull() ?: 0
                }
            }
        }

        // Load historiasAtivas Map
        val historiasStr = prefs.getString("historiasAtivas", "") ?: ""
        player.historiasAtivas.clear()
        if (historiasStr.isNotEmpty()) {
            historiasStr.split(";").forEach {
                val parts = it.split(":")
                if (parts.size == 2) {
                    player.historiasAtivas[parts[0]] = parts[1].toIntOrNull() ?: 0
                }
            }
        }

        // MIGRATIONS LOGIC
        if (loadedVersion < 3) {
            // Migração para Versão 3: Balanceamento de Atributos Máximos
            // Recalcula Vida, Energia e Mente baseando-se no Nível e Profissão atuais.
            val nivelAtual = player.nivel
            
            // Reset para base do Nível 1
            player.vidaMax = 100
            player.energiaMax = 100
            player.cansacoMax = 50
            
            // Reaplica os bônus ganhos em cada level up (de 2 até N) reutilizando a lógica oficial
            for (i in 2..nivelAtual) {
                evoluirAtributosMaximos()
            }
            
            // Ajusta valores atuais para não excederem os novos limites recalculados
            player.vida = player.vida.coerceAtMost(player.vidaMax)
            player.energia = player.energia.coerceAtMost(player.energiaMax)
            player.cansacoMental = player.cansacoMental.coerceAtMost(player.cansacoMax)
        }

        if (loadedVersion < 4) {
            // Migração para Versão 4: Inicializa sistema de traumas
            player.traumasAcumulados = 0
            player.diasParaRecuperarTrauma = 0
        }

        if (loadedVersion < 5) {
            // Migração para Versão 5: Inicializa Coleção e Conquistas
            player.avatarEquipadoId = null
            player.avataresDesbloqueados = mutableSetOf()
            player.avataresProgressoAds = mutableMapOf()
            player.conquistasDesbloqueadas = mutableSetOf()
        }

        if (loadedVersion < 6) {
            // Migração para Versão 6: Inicializa estatísticas de conquistas
            player.mentalStreak = 0
            player.zonasExploradas = mutableSetOf()
        }
    }

    /**
     * Desbloqueia uma conquista com segurança, entregando as recompensas apenas uma vez.
     */
    fun desbloquearConquista(context: Context, achievementId: String) {
        if (player.conquistasDesbloqueadas.contains(achievementId)) return

        val conquista = com.typingfrontier.collection.CollectionRepository.getAchievementById(achievementId) ?: return

        player.conquistasDesbloqueadas.add(achievementId)

        // 1. Recompensa em Frons
        if (conquista.recompensaDinheiro > 0) {
            player.dinheiro += conquista.recompensaDinheiro
        }

        // 2. Recompensa em Avatar
        if (conquista.avatarAssociadoId != null) {
            player.avataresDesbloqueados.add(conquista.avatarAssociadoId)
        }

        // Persistência imediata
        save(context)
        
        android.widget.Toast.makeText(context, "🏆 Conquista: ${conquista.nome}", android.widget.Toast.LENGTH_LONG).show()
    }
}
