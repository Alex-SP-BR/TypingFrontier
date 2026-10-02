package com.typingfrontier

import com.typingfrontier.exploration.EventoExploracao
import com.typingfrontier.economy.ProfessionManager
import com.typingfrontier.shop.ShopActivity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.databinding.ActivityGameBinding
import com.typingfrontier.utils.CurrencyUtils
import com.typingfrontier.collection.CentralActivity
import com.typingfrontier.collection.CollectionRepository
import com.typingfrontier.utils.ViewUtils
import java.util.Locale
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.lifecycle.lifecycleScope
import com.typingfrontier.social.ModerationRepository
import com.typingfrontier.social.PresenceManager
import com.typingfrontier.social.SocialProfileRepository
import com.typingfrontier.station.VipManager
import kotlinx.coroutines.launch
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import com.typingfrontier.social.PrivateMessage
import com.typingfrontier.social.PrivateMessageRepository

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    
    private class PrivateChatSession(
        val targetUserId: String,
        val targetUsername: String,
        var panel: View? = null,
        var historyLayout: LinearLayout? = null,
        var scrollView: ScrollView? = null,
        var minimizedBar: TextView? = null,
        var isMinimized: Boolean = false,
        var unreadCount: Int = 0,
        var lastReceivedMessage: PrivateMessage? = null
    )

    private val activeChatSessions = mutableMapOf<String, PrivateChatSession>()
    private val activeDisplayedMessageIds = mutableSetOf<String>()

    private fun adicionarMensagemAoHistorico(
        context: Context,
        historyLayout: LinearLayout,
        scrollView: ScrollView,
        senderLabel: String,
        messageText: String,
        msg: PrivateMessage?,
        targetUsername: String
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 4)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tv = TextView(context).apply {
            text = "$senderLabel: $messageText"
            setTextColor(Color.parseColor("#E8EDF2"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(tv)

        if (msg != null && msg.id != null) {
            val btnReportMsg = TextView(context).apply {
                text = "▷"
                textSize = 13f
                setTextColor(Color.parseColor("#889099"))
                setPadding(12, 4, 4, 4)
                setOnClickListener {
                    mostrarDialogDenunciaMensagemPrivada(msg, targetUsername)
                }
            }
            row.addView(btnReportMsg)
        }

        historyLayout.addView(row)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun mostrarDialogDenunciaMensagemPrivada(msg: PrivateMessage, targetUsername: String) {
        if (msg.id == null) {
            Toast.makeText(this, "ID da mensagem inválido para denúncia.", Toast.LENGTH_SHORT).show()
            return
        }

        val reasons = arrayOf("Spam / Propaganda", "Ofensas / Assédio", "Conteúdo Inadequado", "Outros")
        val builder = AlertDialog.Builder(this, R.style.Theme_TypingFrontier_AdminDialog)
            .setTitle("Denunciar Mensagem Privada")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val txtPreview = TextView(this).apply {
            text = "Mensagem de @$targetUsername:\n\"${msg.message}\""
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(txtPreview)

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@GameActivity, android.R.layout.simple_spinner_dropdown_item, reasons)
        }
        layout.addView(spinner)

        val edtDesc = EditText(this).apply {
            hint = "Descrição opcional (máx 200)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        layout.addView(edtDesc)

        builder.setView(layout)
        builder.setPositiveButton("Enviar Denúncia") { _, _ ->
            val reason = reasons[spinner.selectedItemPosition]
            val desc = edtDesc.text.toString().trim()
            lifecycleScope.launch {
                try {
                    ModerationRepository.createReport("private_message", msg.id, reason, desc.ifEmpty { null })
                    Toast.makeText(this@GameActivity, "Denúncia enviada com sucesso.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@GameActivity, "Erro ao enviar denúncia: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }
    
    // ESTADO DA FILA DE ANÚNCIOS (HORA EXTRA)
    private var overtimeAdsNeeded = 0
    private var overtimeAdsCompleted = 0
    private var isOvertimeSessionInProgress = false
    
    // PRIORIDADE DA LOUSA
    private var prioridadeAtual = 5

    companion object {
        var startTimeNanos: Long = 0L
        private fun logPerf(marker: String, desc: String) {
            val base = if (startTimeNanos > 0) startTimeNanos else SystemClock.elapsedRealtimeNanos()
            val elapsed = (SystemClock.elapsedRealtimeNanos() - base) / 1_000_000.0
            Log.d("TF_PERF_START", "TF_PERF_START $marker: $desc (+${String.format(Locale.US, "%.1f", elapsed)} ms)")
        }
    }

    private var soundPool: android.media.SoundPool? = null
    private var soundIdA: Int = 0
    private var soundIdB: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        logPerf("T2", "Entry in GameActivity.onCreate()")
        super.onCreate(savedInstanceState)
        
        // Ajuste da Barra de Status para o tema escuro
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        logPerf("T3", "Completion of setContentView()")

        configurarTelaPrincipal()
        logPerf("T4", "Completion of configurarTelaPrincipal()")
        
        // Inicialização da lousa
        prioridadeAtual = 5
        binding.txtDescricao.text = "O que vamos fazer hoje?"

        lifecycleScope.launch {
            PrivateMessageRepository.incomingMessages.collect { msg ->
                val currentUserId = SocialProfileRepository.getCurrentUserId() ?: return@collect
                if (msg.senderId == currentUserId) return@collect

                if (msg.id != null && activeDisplayedMessageIds.contains(msg.id)) {
                    return@collect
                }
                msg.id?.let { activeDisplayedMessageIds.add(it) }

                val senderName = PresenceManager.onlineUsers.value.find { it.user_id == msg.senderId }?.username
                    ?: VipManager.getVipList(this@GameActivity).find { it.userId == msg.senderId }?.username
                    ?: "Viajante"

                val existingSession = activeChatSessions[msg.senderId]
                if (existingSession != null) {
                    existingSession.lastReceivedMessage = msg
                    if (existingSession.historyLayout != null && existingSession.scrollView != null) {
                        adicionarMensagemAoHistorico(this@GameActivity, existingSession.historyLayout!!, existingSession.scrollView!!, senderName, msg.message, msg, existingSession.targetUsername)
                    }
                    if (existingSession.isMinimized) {
                        existingSession.unreadCount++
                        val unreadSuffix = if (existingSession.unreadCount > 0) " (${existingSession.unreadCount})" else ""
                        existingSession.minimizedBar?.text = "$senderName$unreadSuffix"
                    }
                } else {
                    abrirPainelChatPrivado(msg.senderId, senderName, initialMessage = msg, startMinimized = true)
                }
            }
        }

        // Preparação de sons diferida para após a primeira renderização da UI
        binding.root.post {
            logPerf("T5", "First frame scheduled / post-render UI cycle")
            logPerf("T8", "Start of prepararSons()")
            prepararSons()
        }
    }

    override fun onResume() {
        logPerf("T6", "Entry in GameActivity.onResume()")
        super.onResume()
        atualizarHUD()
        logPerf("T7", "Completion of onResume()")
        binding.root.post {
            logPerf("T9", "Start of SoundManager.play()")
            SoundManager.play(this, "aventura")
        }
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

        binding.lblVida.setOnClickListener {
            val p = PlayerManager.player
            if (p.traumasAcumulados > 0) {
                if (p.estoqueMedicamento > 0) {
                    androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
                        .setTitle("💊 Medicamento")
                        .setMessage("Deseja usar 1 Medicamento?\n\n" +
                                "✅ Trata 1 Trauma\n" +
                                "❤️ Recupera HP\n" +
                                "✨ Restaura 50% do progresso perdido (XP e Atributos)\n\n" +
                                "Estoque: ${p.estoqueMedicamento}")
                        .setPositiveButton("USAR") { _, _ ->
                            val result = GameEngine.dispatch(GameAction.UseMedicine)
                            when (result) {
                                is EngineResult.Success -> {
                                    exibirMensagem(result.message, result.extra, prioridade = 4)
                                }
                                is EngineResult.Failure -> {
                                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                            atualizarHUD()
                            PlayerManager.save(this)
                        }
                        .setNegativeButton("CANCELAR", null)
                        .show()
                } else {
                    Toast.makeText(this, "Você não possui Medicamento. Compre na Loja!", Toast.LENGTH_LONG).show()
                }
            }
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
                        dispararAnimacaoMoeda(1)
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

        binding.btnVip.setOnClickListener {
            mostrarDialogoVip()
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

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_overtime, null)
        dialogView.findViewById<TextView>(R.id.txtOvertimeHeader).text = "⏱️ Hora Extra #$numeroHE"
        dialogView.findViewById<TextView>(R.id.txtOvertimeDuration).text = formatarDuracao(duracaoMinutos)
        dialogView.findViewById<TextView>(R.id.txtOvertimeEnergy).text = "-$custoEnergia"
        dialogView.findViewById<TextView>(R.id.txtOvertimeMental).text = "+$gastoMente"
        dialogView.findViewById<TextView>(R.id.txtOvertimeReward).text = CurrencyUtils.formatar(recompensa)
        dialogView.findViewById<TextView>(R.id.txtOvertimeEndTime).text = "🏁 Término: ${horarioTermino.first}:${horarioTermino.second.toString().padStart(2, '0')}"
        dialogView.findViewById<TextView>(R.id.txtOvertimeAdsCount).text = "📺 Requisito: $adsNecessarios ${if (adsNecessarios == 1) "anúncio" else "anúncios"}"

        val dialog = AlertDialog.Builder(this, R.style.Theme_TypingFrontier_MentalDialog)
            .setTitle("Confirmar Jornada")
            .setView(dialogView)
            .setPositiveButton("Assistir e Iniciar") { _, _ ->
                if (p.energia < custoEnergia || (p.cansacoMax - p.cansacoMental) < gastoMente) {
                    Toast.makeText(this, "Recursos insuficientes!", Toast.LENGTH_SHORT).show()
                } else {
                    executarFilaAdsOvertime(adsNecessarios)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()

        // Ajuste de opacidade para tornar o diálogo mais sólido
        dialog.window?.findViewById<android.view.View>(androidx.appcompat.R.id.parentPanel)?.backgroundTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E1E1E"))

        val density = resources.displayMetrics.density
        
        // Customização dos botões
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setBackgroundResource(R.drawable.bg_game_button_primary)
            backgroundTintList = null
            setTextColor(android.graphics.Color.WHITE)
            setPadding((16 * density).toInt(), 0, (16 * density).toInt(), 0)
            val params = layoutParams as android.widget.LinearLayout.LayoutParams
            params.setMargins((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            layoutParams = params
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setBackgroundResource(R.drawable.bg_game_button_secondary)
            backgroundTintList = null
            setTextColor(android.graphics.Color.WHITE)
            setPadding((16 * density).toInt(), 0, (16 * density).toInt(), 0)
            val params = layoutParams as android.widget.LinearLayout.LayoutParams
            params.setMargins((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            layoutParams = params
        }
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

        val dialog = AlertDialog.Builder(this, R.style.Theme_TypingFrontier_MentalDialog)
            .setTitle("⏱️ Fila de Anúncios")
            .setMessage(msg)
            .setPositiveButton("Ver Próximo") { _, _ -> solicitarProximoAdOvertime() }
            .setNegativeButton("Desistir") { _, _ -> isOvertimeSessionInProgress = false }
            .setCancelable(false)
            .show()

        // Ajuste de opacidade para tornar o diálogo mais sólido
        dialog.window?.findViewById<android.view.View>(androidx.appcompat.R.id.parentPanel)?.backgroundTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1E1E1E"))

        val density = resources.displayMetrics.density
        
        // Customização dos botões
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setBackgroundResource(R.drawable.bg_game_button_primary)
            backgroundTintList = null
            setTextColor(android.graphics.Color.WHITE)
            setPadding((16 * density).toInt(), 0, (16 * density).toInt(), 0)
            val params = layoutParams as android.widget.LinearLayout.LayoutParams
            params.setMargins((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            layoutParams = params
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setBackgroundResource(R.drawable.bg_game_button_secondary)
            backgroundTintList = null
            setTextColor(android.graphics.Color.WHITE)
            setPadding((16 * density).toInt(), 0, (16 * density).toInt(), 0)
            val params = layoutParams as android.widget.LinearLayout.LayoutParams
            params.setMargins((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            layoutParams = params
        }
    }

    private fun concluirHoraExtra() {
        isOvertimeSessionInProgress = false
        val result = GameEngine.dispatch(GameAction.Overtime)
        
        when (result) {
            is EngineResult.Success -> {
                exibirMensagem(result.message, prioridade = 4)
                dispararAnimacaoMoeda(1)
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
        logPerf("HUD0", "Entry in atualizarHUD()")

        val p = PlayerManager.player
        logPerf("HUD1", "Player retrieved")

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
        logPerf("HUD2", "Avatar configured")

        binding.txtNomePlayer.text = p.nome
        binding.txtTempo.text = TimeManager.tempoFormatado()
        logPerf("HUD3", "Name and time configured")
        
        // Configuração do ícone da moeda com tratamento de transparência e tamanho controlado (20dp)
        logPerf("HUD4_BEFORE_COIN", "Before ViewUtils.getCoinDrawable()")
        val coinIcon = ViewUtils.getCoinDrawable(this)
        logPerf("HUD4_AFTER_COIN", "After ViewUtils.getCoinDrawable()")

        val size = (20 * resources.displayMetrics.density).toInt()
        coinIcon.setBounds(0, 0, size, size)
        binding.txtDinheiro.setCompoundDrawables(coinIcon, null, null, null)
        binding.txtDinheiro.text = CurrencyUtils.formatar(p.dinheiro)
        logPerf("HUD5", "Money configured")

        // 🔘 BOTÃO TRABALHAR DINÂMICO
        if (p.trabalhouHoje) {
            binding.btnTrabalhar.text = "⏱️ Hora Extra"
        } else {
            binding.btnTrabalhar.text = "💼 Trabalhar"
        }

        // 🩹 AVISO DE TRAUMAS (Apenas informativo se a lousa estiver livre)
        if (p.traumasAcumulados > 0 && prioridadeAtual >= 5) {
            val totalDias = (p.traumasAcumulados - 1) * 2 + p.diasParaRecuperarTrauma
            val msgTrauma = "🩹 Seu corpo está se recuperando. Recomendado descansar mais $totalDias ${if (totalDias == 1) "dia" else "dias"}."
            exibirMensagem(msgTrauma, prioridade = 2)
        }
        logPerf("HUD6", "Button work & traumas configured")

        binding.lblNivel.text = "⭐ Nível ${p.nivel}: ${p.experienciaAtual}/${p.experienciaParaProximoNivel} XP"

        binding.progressXP.max = p.experienciaParaProximoNivel
        binding.progressXP.progress = p.experienciaAtual
        logPerf("HUD7", "XP configured")

        // HUD - VIDA, TRAUMA E BÊNÇÃO (Barra mantida exclusivamente vida/vidaMax)
        val vidaTextBuilder = StringBuilder("❤️ Vida: ${p.vida}/${p.vidaMax}")
        if (p.traumasAcumulados > 0) {
            vidaTextBuilder.append(" 🩹 ${p.traumasAcumulados}")
        }
        if (p.estoqueMedicamento > 0) {
            vidaTextBuilder.append(" 💊 ${p.estoqueMedicamento}")
        }
        if (p.estoqueBencao > 0) {
            vidaTextBuilder.append(" 🛡️ ${p.estoqueBencao}")
        }
        binding.lblVida.text = vidaTextBuilder.toString()

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
        logPerf("HUD8", "Life configured")

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
        logPerf("HUD9", "Exit atualizarHUD()")
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

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }

    private fun prepararSons() {
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_GAME)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = android.media.SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        soundIdA = soundPool?.load(this, R.raw.coin_sound_a, 1) ?: 0
        soundIdB = soundPool?.load(this, R.raw.coin_sound_b, 1) ?: 0
    }

    /**
     * Executa a animação visual das moedas de Frons voando em direção ao contador.
     * Reutiliza o padrão aprovado de 4 moedas (1 estática + 3 animadas).
     */
    private fun dispararAnimacaoMoeda(bonus: Int) {
        if (bonus <= 0) return

        val container = findViewById<android.widget.FrameLayout>(android.R.id.content) ?: return
        val density = resources.displayMetrics.density
        val coinSize = (22 * density).toInt()

        // Definição das 4 moedas (Todas agora usam a versão transparente processada)
        val moedasConfig = List(4) { R.drawable.fron_coin }

        // Origem: txtDescricao (região onde o feedback de pagamento/sucesso aparece)
        val startLoc = IntArray(2)
        binding.txtDescricao.getLocationInWindow(startLoc)

        // Destino: txtDinheiro no HUD
        val endLoc = IntArray(2)
        binding.txtDinheiro.getLocationInWindow(endLoc)

        val rootLoc = IntArray(2)
        container.getLocationInWindow(rootLoc)

        moedasConfig.forEachIndexed { index, resId ->
            container.postDelayed({
                val coinView = android.widget.ImageView(this@GameActivity).apply {
                    setImageDrawable(ViewUtils.getCoinDrawable(this@GameActivity))
                }

                coinView.layoutParams = android.widget.FrameLayout.LayoutParams(coinSize, coinSize)
                coinView.alpha = 0f
                container.addView(coinView)

                // Espaçamento inicial (spread) para não sobrepor todas no início
                val spreadX = (index - 1.5f) * 20f * density
                val spreadY = (if (index % 2 == 0) -15f else 15f) * density

                val startX = startLoc[0] - rootLoc[0] + (binding.txtDescricao.width / 2f) - (coinSize / 2f) + spreadX
                val startY = startLoc[1] - rootLoc[1] + (binding.txtDescricao.height / 2f) - (coinSize / 2f) + spreadY

                val endX = endLoc[0] - rootLoc[0] + (binding.txtDinheiro.width / 2f) - (coinSize / 2f)
                val endY = endLoc[1] - rootLoc[1] + (binding.txtDinheiro.height / 2f) - (coinSize / 2f)

                coinView.x = startX
                coinView.y = startY

                coinView.animate()
                    .translationX(endX)
                    .translationY(endY)
                    .alpha(1f)
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(900)
                    .withEndAction {
                        // Toca o som no momento da chegada (Impacto)
                        val soundToPlay = if (index % 2 == 0) soundIdA else soundIdB
                        soundPool?.play(soundToPlay, 0.5f, 0.5f, 1, 0, 1f)

                        // Feedback de "depósito" no contador
                        coinView.animate()
                            .alpha(0f)
                            .scaleX(0.5f)
                            .scaleY(0.5f)
                            .setDuration(200)
                            .withEndAction {
                                container.removeView(coinView)
                            }
                    }
                    .start()
            }, index * 200L)
        }
    }

    private fun mostrarDialogoVip() {
        val context = this
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (100 * resources.displayMetrics.density).toInt()
            ).apply { topMargin = 16.0f.toInt() }
        }

        val listLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(listLayout)

        var dialog: AlertDialog? = null
        var refreshList: () -> Unit = {}

        refreshList = fun() {
            listLayout.removeAllViews()
            val vipList = VipManager.getVipList(context)
            val onlineSet = PresenceManager.onlineUsers.value.map { it.user_id }.toSet()

            val sortedVips = vipList.sortedByDescending { onlineSet.contains(it.userId) }

            dialog?.setTitle("⭐ Lista VIP (${vipList.size}/50)")

            if (sortedVips.isEmpty()) {
                val emptyTv = TextView(context).apply {
                    text = "Sua lista VIP está vazia."
                    setTextColor(Color.parseColor("#889099"))
                    setPadding(0, 24, 0, 24)
                    gravity = Gravity.CENTER
                }
                listLayout.addView(emptyTv)
                return
            }

            sortedVips.forEach { vip ->
                val isOnline = onlineSet.contains(vip.userId)
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        abrirPainelChatPrivado(vip.userId, vip.username)
                        dialog?.dismiss()
                    }
                }

                val indicator = TextView(context).apply {
                    text = "●"
                    textSize = 16f
                    setTextColor(if (isOnline) Color.parseColor("#4CAF50") else Color.parseColor("#889099"))
                    setPadding(0, 0, 16, 0)
                }
                row.addView(indicator)

                val nameTv = TextView(context).apply {
                    text = vip.username
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(nameTv)

                val btnRemove = Button(context).apply {
                    text = "Remover"
                    textSize = 12f
                    setTextColor(Color.parseColor("#FF5252"))
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        VipManager.removeVip(context, vip.userId)
                        refreshList()
                    }
                }
                row.addView(btnRemove)

                listLayout.addView(row)
            }
        }

        val btnAdd = Button(context).apply {
            text = "+ Adicionar VIP"
            setBackgroundColor(Color.parseColor("#286680"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                mostrarDialogoAdicionarVip {
                    refreshList()
                }
            }
        }
        container.addView(btnAdd)
        container.addView(scrollView)

        refreshList()

        dialog = AlertDialog.Builder(context, R.style.Theme_TypingFrontier_ShopDialog)
            .setTitle("⭐ Lista VIP (${VipManager.getVipList(this).size}/50)")
            .setView(container)
            .setPositiveButton("Fechar", null)
            .create()

        dialog.show()

        val presenceJob = lifecycleScope.launch {
            PresenceManager.onlineUsers.collect {
                refreshList()
            }
        }
        dialog.setOnDismissListener {
            presenceJob.cancel()
        }
    }

    private fun mostrarDialogoAdicionarVip(onAdded: () -> Unit) {
        val input = EditText(this).apply {
            hint = "Digite o username..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#889099"))
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
            .setTitle("Adicionar VIP")
            .setView(input)
            .setPositiveButton("Adicionar") { _, _ ->
                val usernameInput = input.text.toString().trim().removePrefix("@")
                if (usernameInput.isEmpty()) return@setPositiveButton

                val currentUserId = SocialProfileRepository.getCurrentUserId() ?: ""

                lifecycleScope.launch {
                    try {
                        val profile = ModerationRepository.getProfileByUsername(usernameInput)
                        if (profile == null) {
                            Toast.makeText(this@GameActivity, "Jogador não encontrado.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        if (profile.id == currentUserId) {
                            Toast.makeText(this@GameActivity, "Você não pode adicionar a si mesmo.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val added = VipManager.addVip(this@GameActivity, profile.id, profile.username)
                        if (!added) {
                            Toast.makeText(this@GameActivity, "Lista cheia (máx 50) ou jogador já adicionado.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@GameActivity, "${profile.username} adicionado aos VIPs!", Toast.LENGTH_SHORT).show()
                            onAdded()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@GameActivity, "Sem conexão com a internet ou erro ao buscar jogador: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirPainelChatPrivado(
        targetUserId: String,
        targetUsername: String,
        initialMessage: PrivateMessage? = null,
        startMinimized: Boolean = false
    ) {
        val existingSession = activeChatSessions[targetUserId]
        if (existingSession != null) {
            if (existingSession.isMinimized && !startMinimized) {
                existingSession.minimizedBar?.let {
                    (it.parent as? ViewGroup)?.removeView(it)
                    existingSession.minimizedBar = null
                }
                existingSession.isMinimized = false
                existingSession.unreadCount = 0
                existingSession.panel?.visibility = View.VISIBLE
            } else if (existingSession.isMinimized && startMinimized) {
                if (initialMessage != null) {
                    if (existingSession.historyLayout != null && existingSession.scrollView != null) {
                        adicionarMensagemAoHistorico(this, existingSession.historyLayout!!, existingSession.scrollView!!, targetUsername, initialMessage.message, initialMessage, targetUsername)
                    }
                    existingSession.unreadCount++
                    val unreadSuffix = if (existingSession.unreadCount > 0) " (${existingSession.unreadCount})" else ""
                    existingSession.minimizedBar?.text = "$targetUsername$unreadSuffix"
                }
            }
            return
        }

        val context = this
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: return

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E6121212"))
            setPadding(16, 16, 16, 16)
            elevation = 16f
            layoutParams = FrameLayout.LayoutParams(
                (320 * resources.displayMetrics.density).toInt(),
                (210 * resources.displayMetrics.density).toInt()
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        val session = PrivateChatSession(
            targetUserId = targetUserId,
            targetUsername = targetUsername,
            panel = panel,
            unreadCount = if (startMinimized && initialMessage != null) 1 else 0
        )

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 8)
            setBackgroundColor(Color.parseColor("#286680"))
        }

        val onlineSet = PresenceManager.onlineUsers.value.map { it.user_id }.toSet()
        val isOnline = onlineSet.contains(targetUserId)

        val indicator = TextView(context).apply {
            text = "●"
            textSize = 14f
            setTextColor(if (isOnline) Color.parseColor("#4CAF50") else Color.parseColor("#889099"))
            setPadding(0, 0, 8, 0)
        }
        header.addView(indicator)

        val titleTv = TextView(context).apply {
            text = targetUsername
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(titleTv)

        val btnHelpChat = Button(context).apply {
            text = "?"
            textSize = 12f
            setTextColor(Color.CYAN)
            setBackgroundColor(Color.TRANSPARENT)
            minWidth = 0
            minHeight = 0
            setPadding(12, 0, 12, 0)
            setOnClickListener {
                AlertDialog.Builder(context, R.style.Theme_TypingFrontier_AdminDialog)
                    .setTitle("Ajuda - Denunciar Mensagem")
                    .setMessage("Para denunciar uma mensagem específica, toque no símbolo ▷ localizado ao lado daquela mensagem.")
                    .setPositiveButton("Entendi", null)
                    .show()
            }
        }
        header.addView(btnHelpChat)

        val btnMinimize = Button(context).apply {
            text = "_"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            minWidth = 0
            minHeight = 0
            setPadding(12, 0, 12, 0)
            setOnClickListener {
                toggleMinimizeChat(session)
            }
        }
        header.addView(btnMinimize)

        val btnClose = Button(context).apply {
            text = "X"
            textSize = 12f
            setTextColor(Color.parseColor("#FF5252"))
            setBackgroundColor(Color.TRANSPARENT)
            minWidth = 0
            minHeight = 0
            setPadding(12, 0, 12, 0)
            setOnClickListener {
                panel.let { (it.parent as? ViewGroup)?.removeView(it) }
                session.minimizedBar?.let { (it.parent as? ViewGroup)?.removeView(it) }
                activeChatSessions.remove(targetUserId)
            }
        }
        header.addView(btnClose)
        panel.addView(header)

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply { topMargin = 8.0f.toInt(); bottomMargin = 8.0f.toInt() }
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(8, 8, 8, 8)
        }

        val historyLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        scrollView.addView(historyLayout)
        panel.addView(scrollView)

        session.historyLayout = historyLayout
        session.scrollView = scrollView

        if (initialMessage != null) {
            adicionarMensagemAoHistorico(context, historyLayout, scrollView, targetUsername, initialMessage.message, initialMessage, targetUsername)
        }

        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val input = EditText(context).apply {
            hint = "Digite sua mensagem..."
            setTextColor(Color.BLACK)
            setHintTextColor(Color.parseColor("#889099"))
            setBackgroundColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        footer.addView(input)

        val btnSend = Button(context).apply {
            text = "Enviar"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#286680"))
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    input.setText("")
                    lifecycleScope.launch {
                        try {
                            val sentMsg = PrivateMessageRepository.sendMessage(currentUserId, targetUserId, text)
                            sentMsg.id?.let { activeDisplayedMessageIds.add(it) }
                            adicionarMensagemAoHistorico(context, historyLayout, scrollView, "Você", sentMsg.message, sentMsg, targetUsername)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao enviar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        footer.addView(btnSend)
        panel.addView(footer)

        var dX = 0f
        var dY = 0f
        header.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (isTouchInside(btnMinimize, event.rawX, event.rawY) || isTouchInside(btnClose, event.rawX, event.rawY)) {
                    return@setOnTouchListener false
                }
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = panel.x - event.rawX
                    dY = panel.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    var newX = event.rawX + dX
                    var newY = event.rawY + dY
                    val parent = panel.parent as? View ?: return@setOnTouchListener false
                    newX = newX.coerceIn(0f, (parent.width - panel.width).toFloat().coerceAtLeast(0f))
                    newY = newY.coerceIn(0f, (parent.height - panel.height).toFloat().coerceAtLeast(0f))
                    panel.x = newX
                    panel.y = newY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    header.performClick()
                    true
                }
                else -> false
            }
        }

        activeChatSessions[targetUserId] = session
        binding.root.addView(panel)

        if (startMinimized) {
            toggleMinimizeChat(session)
        }
    }

    private fun isTouchInside(view: View, rawX: Float, rawY: Float): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        return rawX >= x.toFloat() && rawX <= (x + view.width).toFloat() && rawY >= y.toFloat() && rawY <= (y + view.height).toFloat()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun toggleMinimizeChat(session: PrivateChatSession) {
        val panel = session.panel ?: return
        if (!session.isMinimized) {
            session.isMinimized = true
            panel.visibility = View.GONE
            val context = this
            val bar = TextView(context).apply {
                val unreadSuffix = if (session.unreadCount > 0) " (${session.unreadCount})" else ""
                text = "${session.targetUsername}$unreadSuffix"
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setBackgroundColor(Color.parseColor("#286680"))
                setPadding(24, 12, 24, 12)
                elevation = 16f
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    val activeBarIndex = activeChatSessions.values.count { it.minimizedBar != null }
                    val baseMarginDp = (24 * resources.displayMetrics.density).toInt()
                    val offsetYDp = (activeBarIndex * 48 * resources.displayMetrics.density).toInt()
                    setMargins(baseMarginDp, baseMarginDp, baseMarginDp, baseMarginDp + offsetYDp)
                }

                var dX = 0f
                var dY = 0f
                var isDragging = false
                val touchSlop = 10f
                var startX = 0f
                var startY = 0f

                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            dX = v.x - event.rawX
                            dY = v.y - event.rawY
                            startX = event.rawX
                            startY = event.rawY
                            isDragging = false
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = Math.abs(event.rawX - startX)
                            val deltaY = Math.abs(event.rawY - startY)
                            if (deltaX > touchSlop || deltaY > touchSlop) {
                                isDragging = true
                            }
                            if (isDragging) {
                                var newX = event.rawX + dX
                                var newY = event.rawY + dY
                                val parent = v.parent as? View ?: return@setOnTouchListener false
                                newX = newX.coerceIn(0f, (parent.width - v.width).toFloat().coerceAtLeast(0f))
                                newY = newY.coerceIn(0f, (parent.height - v.height).toFloat().coerceAtLeast(0f))
                                v.x = newX
                                v.y = newY
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!isDragging) {
                                v.performClick()
                            }
                            true
                        }
                        else -> false
                    }
                }

                setOnClickListener {
                    (parent as? ViewGroup)?.removeView(this)
                    session.minimizedBar = null
                    session.isMinimized = false
                    session.panel?.visibility = View.VISIBLE
                    session.unreadCount = 0
                }
            }
            session.minimizedBar = bar
            binding.root.addView(bar)
        }
    }
}
