package com.typingfrontier

import com.typingfrontier.exploration.EventoExploracao
import com.typingfrontier.economy.ProfessionManager
import com.typingfrontier.shop.ShopActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.databinding.ActivityGameBinding

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarTelaPrincipal()
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

        binding.btnComer.setOnClickListener {
            val result = GameEngine.dispatch(GameAction.Eat)
            
            when (result) {
                is EngineResult.Success -> {
                    binding.txtDescricao.text = result.message
                }
                is EngineResult.Failure -> {
                    binding.txtDescricao.text = result.message
                }
            }

            atualizarHUD()
            PlayerManager.save(this)
        }

        binding.btnTrabalhar.setOnClickListener {
            val result = GameEngine.dispatch(GameAction.Work)
            
            when (result) {
                is EngineResult.Success -> {
                    binding.txtDescricao.text = result.message
                    result.extra?.let {
                        binding.txtDescricao.append("\n\n$it")
                    }
                    // FEEDBACK VISUAL: Dinheiro pulando
                    binding.txtDinheiro.animate().scaleX(1.4f).scaleY(1.4f).setDuration(200).withEndAction {
                        binding.txtDinheiro.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                    }.start()
                }
                is EngineResult.Failure -> {
                    binding.txtDescricao.text = result.message
                }
            }

            atualizarHUD()
            PlayerManager.save(this)
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

        binding.btnLoja.setOnClickListener {
            startActivity(Intent(this, ShopActivity::class.java))
        }

        binding.btnDormir.setOnClickListener {
            val result = GameEngine.dispatch(GameAction.Sleep)
            when (result) {
                is EngineResult.Success -> {
                    binding.txtDescricao.text = result.message
                    result.extra?.let { binding.txtDescricao.append("\n\n$it") }
                }
                is EngineResult.Failure -> {
                    binding.txtDescricao.text = result.message
                }
            }
            atualizarHUD()
            PlayerManager.save(this)
        }

        binding.btnDescansar.setOnClickListener {
            val result = GameEngine.dispatch(GameAction.Rest)
            when (result) {
                is EngineResult.Success -> {
                    binding.txtDescricao.text = result.message
                    result.extra?.let { binding.txtDescricao.append("\n\n$it") }
                }
                is EngineResult.Failure -> {
                    binding.txtDescricao.text = result.message
                }
            }
            atualizarHUD()
            PlayerManager.save(this)
        }

        binding.btnSair.setOnClickListener {
            finishAffinity()
        }
    }

    // ------------------------------------------------
    // EXPLORAÇÃO
    // ------------------------------------------------
    private fun abrirExploracao() {
        startActivity(Intent(this, ExplorationActivity::class.java))
    }

    // ------------------------------------------------
    // HUD
    // ------------------------------------------------
    private fun atualizarHUD() {

        val p = PlayerManager.player
        
        // Removido o load() daqui para evitar que o save antigo sobrescreva as mudanças da Engine em tempo real.

        binding.imgPersonagem.setImageResource(
            if (p.sexo == "Masculino") R.drawable.homem
            else R.drawable.mulher
        )

        binding.txtNomePlayer.text = p.nome
        binding.txtTempo.text = TimeManager.tempoFormatado()
        binding.txtDinheiro.text = "💰 R$ ${p.dinheiro}"

        // 🩹 AVISO DE TRAUMAS (Apenas informativo no load da tela ou repouso)
        if (p.traumasAcumulados > 0) {
            val totalDias = (p.traumasAcumulados - 1) * 2 + p.diasParaRecuperarTrauma
            val msgTrauma = "🩹 Seu corpo está se recuperando. Recomendado descansar mais $totalDias ${if (totalDias == 1) "dia" else "dias"}."
            // Se a descrição estiver vazia ou com a pergunta padrão, mostra o aviso
            if (binding.txtDescricao.text == "O que vamos fazer hoje?") {
                binding.txtDescricao.text = msgTrauma
            }
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
}
