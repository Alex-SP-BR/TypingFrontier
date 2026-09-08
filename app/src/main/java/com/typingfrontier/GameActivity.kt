package com.typingfrontier

import com.typingfrontier.exploration.EventoExploracao
import com.typingfrontier.economy.ProfessionManager
import com.typingfrontier.shop.ShopActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.databinding.ActivityGameBinding
import com.typingfrontier.utils.CurrencyUtils
import com.typingfrontier.collection.CentralActivity
import com.typingfrontier.collection.CollectionRepository
import com.typingfrontier.utils.ViewUtils

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    
    // ESTADO DA FILA DE ANÚNCIOS (HORA EXTRA)
    private var overtimeAdsNeeded = 0
    private var overtimeAdsCompleted = 0
    private var isOvertimeSessionInProgress = false
    
    // PRIORIDADE DA LOUSA
    private var prioridadeAtual = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarTelaPrincipal()
        
        // Inicialização da lousa
        prioridadeAtual = 5
        binding.txtDescricao.text = "O que vamos fazer hoje?"

        atualizarHUD()
    }

    override fun onResume() {
        super.onResume()
        atualizarHUD()
        SoundManager.play(this, "aventura")
    }

    // ------------------------------------------------
    // MENU PRINCIPAL
    // ------------------------------------------------
    private fun configurarTelaPrincipal() {

        binding.btnExplore.setOnClickListener {
            abrirExploracao()
        }

        binding.btnHelpExplore.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🌍 Explorar")
                .setMessage("Aventure-se na cidade para ganhar Experiência e Frons. Cada avanço consome 5 de Energia, 7 de Energia Mental e 1h15 do dia. Cuidado: falhas críticas podem levar à hospitalização!")
                .setPositiveButton("Entendi", null)
                .show()
        }

        binding.btnHelpWork.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("💼 Trabalhar")
                .setMessage("Sua principal fonte de renda. Consome 35 de Energia e 11 de Energia Mental. O salário aumenta conforme seu nível e atributos mentais. Disponível 1x ao dia.")
                .setPositiveButton("Entendi", null)
                .show()
        }

        binding.btnHelpTrainPhysical.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🏋️ Físico")
                .setMessage("Melhore Força, Resistência ou Velocidade. Consome Energia, Energia Mental e tempo (30m a 1h). Risco de falha se estiver com a Energia Mental exausta!")
                .setPositiveButton("Entendi", null)
                .show()
        }

        binding.btnHelpTrainMental.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🧠 Mental")
                .setMessage("Estude Português ou Matemática para desenvolver Inteligência e Carisma. Não consome tempo, mas estudar de madrugada consome muito mais Energia e Energia Mental.")
                .setPositiveButton("Entendi", null)
                .show()
        }

        binding.btnHelpEat.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🥪 Comer")
                .setMessage("Recupere 20 pontos de Energia por meio de uma refeição. Custa Frons conforme sua profissão. Lanchonetes fecham às 22h.")
                .setPositiveButton("Entendi", null)
                .show()
        }

        binding.btnHelpRest.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🧘 Pausa")
                .setMessage("Recupere 20 pontos de Energia Mental instantaneamente. Não gasta dinheiro nem tempo. Disponível apenas uma vez por dia.")
                .setPositiveButton("Entendi", null)
                .show()
        }

        binding.btnHelpSleep.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("😴 Dormir")
                .setMessage("Encerre o dia e restaure seus status. O aluguel é cobrado automaticamente. Se não tiver dinheiro, você dormirá na rua com penalidades de Vida.")
                .setPositiveButton("Entendi", null)
                .show()
        }

        binding.btnHelpVida.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("❤️ Vida")
                .setMessage("Representa sua condição física. Se chegar a zero, determinadas consequências podem ocorrer durante o jogo.")
                .setPositiveButton("Entendi", null)
                .show()
        }

        binding.btnHelpNivel.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⭐ Nível")
                .setMessage("Representa seu progresso geral. Ganhe XP em atividades e aventuras para subir de nível e aumentar o limite de seus atributos.")
                .setPositiveButton("Entendi", null)
                .show()
        }

        binding.btnHelpEnergia.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚡ Energia")
                .setMessage("Representa sua disposição física para realizar atividades durante o dia.")
                .setPositiveButton("Entendi", null)
                .show()
        }

        binding.btnHelpMente.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🧠 Energia Mental")
                .setMessage("Representa sua capacidade mental para estudar e realizar atividades intelectuais.")
                .setPositiveButton("Entendi", null)
                .show()
        }

        binding.btnComer.setOnClickListener {
            val result = GameEngine.dispatch(GameAction.Eat)
            
            when (result) {
                is EngineResult.Success -> {
                    exibirMensagem(result.message, result.extra, prioridade = 4)
                }
                is EngineResult.Failure -> {
                    exibirMensagem(result.message, prioridade = 3)
                }
            }

            atualizarHUD()
            PlayerManager.save(this)
        }

        binding.btnTrabalhar.setOnClickListener {
            val p = PlayerManager.player
            if (!p.trabalhouHoje) {
                // TRABALHO NORMAL
                val result = GameEngine.dispatch(GameAction.Work)
                when (result) {
                    is EngineResult.Success -> {
                        exibirMensagem(result.message, result.extra, prioridade = 4)
                        binding.txtDinheiro.animate().scaleX(1.4f).scaleY(1.4f).setDuration(200).withEndAction {
                            binding.txtDinheiro.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                        }.start()
                    }
                    is EngineResult.Failure -> {
                        exibirMensagem(result.message, prioridade = 3)
                    }
                }
                atualizarHUD()
                PlayerManager.save(this)
            } else {
                // HORA EXTRA
                iniciarPreparacaoHoraExtra()
            }
        }

        binding.btnTreinoFisico.setOnClickListener {
            startActivity(Intent(this, TrainingActivity::class.java))
        }

        binding.btnTreinoMental.setOnClickListener {
            startActivity(Intent(this, MentalTrainingActivity::class.java))
        }

        binding.btnStatus.setOnClickListener {
            startActivity(Intent(this, StatusActivity::class.java))
        }

        binding.btnSettingsInGame.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnCentral.setOnClickListener {
            startActivity(Intent(this, CentralActivity::class.java))
        }

        binding.imgPersonagem.setOnClickListener {
            ampliarAvatarAtual()
        }

        binding.btnLoja.setOnClickListener {
            startActivity(Intent(this, ShopActivity::class.java))
        }

        binding.btnDormir.setOnClickListener {
            val result = GameEngine.dispatch(GameAction.Sleep)
            when (result) {
                is EngineResult.Success -> {
                    // Após dormir, tudo reseta, então a prioridade também volta ao normal.
                    prioridadeAtual = 5
                    exibirMensagem(result.message, result.extra, prioridade = 4)
                }
                is EngineResult.Failure -> {
                    exibirMensagem(result.message, prioridade = 3)
                }
            }
            atualizarHUD()
            PlayerManager.save(this)
        }

        binding.btnDescansar.setOnClickListener {
            val result = GameEngine.dispatch(GameAction.Rest)
            when (result) {
                is EngineResult.Success -> {
                    exibirMensagem(result.message, result.extra, prioridade = 4)
                }
                is EngineResult.Failure -> {
                    exibirMensagem(result.message, prioridade = 3)
                }
            }
            atualizarHUD()
            PlayerManager.save(this)
        }

        binding.btnSair.setOnClickListener {
            finishAffinity()
        }

        binding.txtDinheiro.setOnClickListener {
            CurrencyUtils.mostrarSaldoExato(this, PlayerManager.player.dinheiro)
        }
    }

    // ------------------------------------------------
    // EXPLORAÇÃO
    // ------------------------------------------------
    private fun abrirExploracao() {
        startActivity(Intent(this, ExplorationActivity::class.java))
    }

    // ------------------------------------------------
    // FILA DE ANÚNCIOS (HORA EXTRA)
    // ------------------------------------------------

    private fun iniciarPreparacaoHoraExtra() {
        val p = PlayerManager.player
        val duracaoMinutos = GameEngine.getMaiorDuracaoOvertimeDisponivel()
        if (duracaoMinutos == null) {
            exibirMensagem("🕒 Sem tempo suficiente para Hora Extra hoje (limite 22:00).", prioridade = 3)
            return
        }

        val adsNecessarios = GameEngine.getAnunciosNecessariosOvertime(duracaoMinutos)
        val horarioTermino = GameEngine.calcularHorarioTerminoOvertime(duracaoMinutos)
        
        // Simulação de custos para o diálogo
        val numeroHE = p.horasExtrasFeitasHoje + 1
        val percentual = Math.min(numeroHE * 0.05, 0.5)
        val custoEnergia = (5 + (p.energiaMax * percentual)).toInt()
        val gastoMente = (2.5 + (p.cansacoMax * percentual)).toInt()
        
        // Recompensa estimada
        val salarioNormal = ProfessionManager.calcularSalario(p)
        val duracaoHoras = duracaoMinutos / 60.0
        val fatorEficiencia = Math.max(0.5, 1.0 - (p.horasExtrasFeitasHoje * 0.1))
        val recompensa = ((salarioNormal / 8.0) * duracaoHoras * 2.0 * fatorEficiencia).toInt()

        val msg = "⏱️ Hora Extra #$numeroHE\n\n" +
                "⏳ Duração: ${formatarDuracao(duracaoMinutos)}\n" +
                "🏁 Término: ${horarioTermino.first}:${horarioTermino.second.toString().padStart(2, '0')}\n" +
                "💰 Ganho: ${CurrencyUtils.formatar(recompensa)}\n\n" +
                "⚡ Energia: -$custoEnergia\n" +
                "🧠 Cansaço Mental: +$gastoMente\n" +
                "📺 Requisito: $adsNecessarios ${if (adsNecessarios == 1) "anúncio" else "anúncios"}\n\n" +
                "⚠️ Esta atividade não concede XP."

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirmar Jornada")
            .setMessage(msg)
            .setPositiveButton("Assistir e Iniciar") { _, _ ->
                if (p.energia < custoEnergia || (p.cansacoMax - p.cansacoMental) < gastoMente) {
                    Toast.makeText(this, "Recursos insuficientes!", Toast.LENGTH_SHORT).show()
                } else {
                    executarFilaAdsOvertime(adsNecessarios)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun formatarDuracao(totalMinutos: Int): String {
        val h = totalMinutos / 60
        val m = totalMinutos % 60
        return if (h > 0) {
            if (m > 0) "${h}h ${m}min" else "${h}h"
        } else {
            "${m}min"
        }
    }

    private fun executarFilaAdsOvertime(totalAds: Int) {
        overtimeAdsNeeded = totalAds
        overtimeAdsCompleted = 0
        isOvertimeSessionInProgress = true
        solicitarProximoAdOvertime()
    }

    private fun solicitarProximoAdOvertime() {
        if (!isOvertimeSessionInProgress) return

        com.typingfrontier.utils.AdManager.showRewardedAd(
            this,
            onRewardEarned = {
                runOnUiThread {
                    overtimeAdsCompleted++
                }
            },
            onAdClosed = {
                runOnUiThread {
                    if (overtimeAdsCompleted < overtimeAdsNeeded) {
                        mostrarDialogoProgressoAds()
                    } else {
                        concluirHoraExtra()
                    }
                }
            },
            onAdFailed = { erro ->
                runOnUiThread {
                    Toast.makeText(this, "Anúncio indisponível: $erro", Toast.LENGTH_SHORT).show()
                    mostrarDialogoProgressoAds()
                }
            }
        )
    }

    private fun mostrarDialogoProgressoAds() {
        val faltam = overtimeAdsNeeded - overtimeAdsCompleted
        val msg = "Progresso: $overtimeAdsCompleted de $overtimeAdsNeeded concluídos.\n\n" +
                "Faltam $faltam ${if (faltam == 1) "anúncio" else "anúncios"} para liberar a Hora Extra.\n" +
                "Deseja continuar?"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⏱️ Fila de Anúncios")
            .setMessage(msg)
            .setPositiveButton("Ver Próximo") { _, _ -> solicitarProximoAdOvertime() }
            .setNegativeButton("Desistir") { _, _ -> isOvertimeSessionInProgress = false }
            .setCancelable(false)
            .show()
    }

    private fun concluirHoraExtra() {
        isOvertimeSessionInProgress = false
        val result = GameEngine.dispatch(GameAction.Overtime)
        
        when (result) {
            is EngineResult.Success -> {
                exibirMensagem(result.message, prioridade = 4)
                binding.txtDinheiro.animate().scaleX(1.4f).scaleY(1.4f).setDuration(200).withEndAction {
                    binding.txtDinheiro.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                }.start()
            }
            is EngineResult.Failure -> {
                exibirMensagem(result.message, prioridade = 3)
            }
        }
        atualizarHUD()
        PlayerManager.save(this)
    }

    private fun ampliarAvatarAtual() {
        val p = PlayerManager.player
        val avatarValido = CollectionRepository.isAvatarValidoParaPlayer(p.avatarEquipadoId, p)
        val avatarEquipado = if (avatarValido) CollectionRepository.getAvatarById(p.avatarEquipadoId) else null

        if (avatarEquipado != null) {
            ViewUtils.showZoomDialog(
                this,
                avatarEquipado.imagemRes,
                avatarEquipado.nome,
                "Nível ${avatarEquipado.nivelRequisito}"
            )
        } else {
            val resId = if (p.sexo == "Masculino") R.drawable.homem else R.drawable.mulher
            ViewUtils.showZoomDialog(
                this,
                resId,
                "Avatar Original",
                "Sempre disponível"
            )
        }
    }

    // ------------------------------------------------
    // HUD
    // ------------------------------------------------
    private fun atualizarHUD() {

        val p = PlayerManager.player
        
        // Removido o load() daqui para evitar que o save antigo sobrescreva as mudanças da Engine em tempo real.

        // 🏆 SISTEMA DE AVATARES
        val avatarValido = CollectionRepository.isAvatarValidoParaPlayer(p.avatarEquipadoId, p)
        val avatarEquipado = if (avatarValido) CollectionRepository.getAvatarById(p.avatarEquipadoId) else null

        if (avatarEquipado != null) {
            binding.imgPersonagem.setImageResource(avatarEquipado.imagemRes)
        } else {
            binding.imgPersonagem.setImageResource(
                if (p.sexo == "Masculino") R.drawable.homem
                else R.drawable.mulher
            )
        }

        binding.txtNomePlayer.text = p.nome
        binding.txtTempo.text = TimeManager.tempoFormatado()
        binding.txtDinheiro.text = "💰 ${CurrencyUtils.formatar(p.dinheiro)}"

        // 🔘 BOTÃO TRABALHAR DINÂMICO
        if (p.trabalhouHoje) {
            binding.btnTrabalhar.text = "⏱️ Hora Extra"
        } else {
            binding.btnTrabalhar.text = "💼 Trabalhar"
        }

        // 🩹 AVISO DE TRAUMAS (Apenas informativo no load da tela ou repouso)
        if (p.traumasAcumulados > 0) {
            val totalDias = (p.traumasAcumulados - 1) * 2 + p.diasParaRecuperarTrauma
            val msgTrauma = "🩹 Seu corpo está se recuperando. Recomendado descansar mais $totalDias ${if (totalDias == 1) "dia" else "dias"}."
            exibirMensagem(msgTrauma, prioridade = 2)
        }

        binding.lblNivel.text = "⭐ Nível ${p.nivel}: ${p.experienciaAtual}/${p.experienciaParaProximoNivel} XP"

        binding.progressXP.max = p.experienciaParaProximoNivel
        binding.progressXP.progress = p.experienciaAtual

        binding.lblVida.text = "❤️ Vida: ${p.vida}/${p.vidaMax}"
        binding.progressVida.max = p.vidaMax
        binding.progressVida.progress = p.vida

        // Pulsa a barra de Vida se estiver baixa (< 20%)
        if (p.vida < 20) {
            if (binding.progressVida.animation == null) {
                val anim = android.view.animation.AlphaAnimation(1f, 0.3f).apply {
                    duration = 400 // Pulso um pouco mais rápido para a vida
                    repeatCount = android.view.animation.Animation.INFINITE
                    repeatMode = android.view.animation.Animation.REVERSE
                }
                binding.progressVida.startAnimation(anim)
            }
        } else {
            binding.progressVida.clearAnimation()
        }

        binding.lblEnergia.text = "⚡ Energia: ${p.energia}/${p.energiaMax}"
        binding.progressEnergia.max = p.energiaMax
        binding.progressEnergia.progress = p.energia

        val mentalEnergia = (p.cansacoMax - p.cansacoMental).coerceAtLeast(0)
        binding.lblMente.text = "🧠 Energia Mental: $mentalEnergia/${p.cansacoMax}"
        binding.progressMente.max = p.cansacoMax
        binding.progressMente.progress = mentalEnergia

        // Padronização de Cores e Alertas Críticos (10%)
        val anim = android.view.animation.AlphaAnimation(1f, 0.4f).apply {
            duration = 500
            repeatCount = android.view.animation.Animation.INFINITE
            repeatMode = android.view.animation.Animation.REVERSE
        }

        // Energia
        if (p.energia < p.energiaMax * 0.1) {
            binding.progressEnergia.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
            if (binding.progressEnergia.animation == null) binding.progressEnergia.startAnimation(anim)
        } else {
            binding.progressEnergia.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FBC02D"))
            binding.progressEnergia.clearAnimation()
        }

        // Energia Mental
        if (mentalEnergia < p.cansacoMax * 0.1) {
            binding.progressMente.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
            if (binding.progressMente.animation == null) binding.progressMente.startAnimation(anim)
        } else {
            binding.progressMente.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7B1FA2"))
            binding.progressMente.clearAnimation()
        }
    }

    /**
     * Gerencia a lousa de mensagens com sistema de prioridades.
     */
    private fun exibirMensagem(mensagem: String, extra: String? = null, prioridade: Int) {
        var finalPrioridade = prioridade
        
        // Detecção automática de eventos graves nos extras
        if (extra != null) {
            if (extra.contains("VOCÊ DESMAIOU") || extra.contains("COLAPSO CORPORAL") || extra.contains("ESTADO CRÍTICO")) {
                finalPrioridade = 1
            }
        }

        // Se a nova mensagem for mais ou igualmente importante que a atual, substitui.
        if (finalPrioridade <= prioridadeAtual) {
            prioridadeAtual = finalPrioridade
            
            val textoFinal = if (extra != null) "$mensagem\n\n$extra" else mensagem
            binding.txtDescricao.text = textoFinal
        }
    }
}
