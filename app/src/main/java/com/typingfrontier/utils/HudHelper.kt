package com.typingfrontier.utils

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.typingfrontier.HudSettingsManager
import com.typingfrontier.HudSettingsManager.HudCategory
import com.typingfrontier.PlayerManager
import com.typingfrontier.R
import com.typingfrontier.TimeManager
import com.typingfrontier.collection.CollectionRepository
import com.typingfrontier.utils.CurrencyUtils

object HudHelper {

    class HudViews(val root: View) {
        val hudLayoutHeader: View? = root.findViewById(R.id.hudLayoutHeader)
        val imgPersonagem: ImageView? = root.findViewById(R.id.hudAvatar)
        val txtNomePlayer: TextView? = root.findViewById(R.id.hudNome)
        val txtTempo: TextView? = root.findViewById(R.id.hudTempo)
        val txtDinheiro: TextView? = root.findViewById(R.id.txtVitalDinheiro)
        
        val hudDivider: View? = root.findViewById(R.id.hudDivider)
        
        val hudLayoutXP: View? = root.findViewById(R.id.hudLayoutXP)
        val lblNivel: TextView? = root.findViewById(R.id.hudNivel)
        val progressXP: ProgressBar? = root.findViewById(R.id.hudProgressXP)
        val btnHelpNivel: View? = root.findViewById(R.id.hudHelpNivel)
        
        val hudLayoutVida: View? = root.findViewById(R.id.hudLayoutVida)
        val lblVida: TextView? = root.findViewById(R.id.hudVida)
        val progressVida: ProgressBar? = root.findViewById(R.id.hudProgressVida)
        val btnHelpVida: View? = root.findViewById(R.id.hudHelpVida)
        
        val hudLayoutEnergia: View? = root.findViewById(R.id.hudLayoutEnergia)
        val lblEnergia: TextView? = root.findViewById(R.id.txtVitalEnergia)
        val progressEnergia: ProgressBar? = root.findViewById(R.id.progressVitalEnergia)
        val btnHelpEnergia: View? = root.findViewById(R.id.hudHelpEnergia)
        
        val hudLayoutMente: View? = root.findViewById(R.id.hudLayoutMente)
        val lblMente: TextView? = root.findViewById(R.id.txtVitalMente)
        val progressMente: ProgressBar? = root.findViewById(R.id.progressVitalMente)
        val btnHelpMente: View? = root.findViewById(R.id.hudHelpMente)
        
        val txtAvisoColapso: View? = root.findViewById(R.id.txtAvisoColapso)
    }

    fun atualizar(activity: Activity, views: HudViews, category: HudCategory? = null) {
        val p = PlayerManager.player

        // APLICA PERSONALIZAÇÃO SE HOUVER CATEGORIA (Exceto na GameActivity que não passa categoria)
        if (category != null) {
            configurarExibicao(
                views = views,
                showXP = HudSettingsManager.isXpVisible(category),
                showVida = HudSettingsManager.isHealthVisible(category),
                showEnergia = HudSettingsManager.isPhysicalEnergyVisible(category),
                showMente = HudSettingsManager.isMentalEnergyVisible(category)
            )
        } else {
            // Garante que tudo esteja visível se não houver categoria (comportamento padrão)
            configurarExibicao(views)
        }

        // 🏆 SISTEMA DE AVATARES
        val avatarValido = CollectionRepository.isAvatarValidoParaPlayer(p.avatarEquipadoId, p)
        val avatarEquipado = if (avatarValido) CollectionRepository.getAvatarById(p.avatarEquipadoId) else null

        views.imgPersonagem?.let { img ->
            if (avatarEquipado != null) {
                img.setImageResource(avatarEquipado.imagemRes)
            } else {
                img.setImageResource(
                    if (p.sexo == "Masculino") R.drawable.homem
                    else R.drawable.mulher
                )
            }
            img.setOnClickListener {
                ampliarAvatar(activity)
            }
        }

        views.txtNomePlayer?.text = p.nome
        views.txtTempo?.text = TimeManager.tempoFormatado()
        
        // Configuração do ícone da moeda com tamanho controlado (20dp)
        val coinIcon = androidx.core.content.ContextCompat.getDrawable(activity, R.drawable.fron_coin)
        val size = (20 * activity.resources.displayMetrics.density).toInt()
        coinIcon?.setBounds(0, 0, size, size)
        views.txtDinheiro?.setCompoundDrawables(coinIcon, null, null, null)

        val dinheiroFormatado = CurrencyUtils.formatar(p.dinheiro)
        views.txtDinheiro?.text = dinheiroFormatado
        views.txtDinheiro?.setOnClickListener {
            CurrencyUtils.mostrarSaldoExato(activity, p.dinheiro)
        }

        // XP
        views.lblNivel?.text = "⭐ Nível ${p.nivel}: ${p.experienciaAtual}/${p.experienciaParaProximoNivel} XP"
        views.progressXP?.max = p.experienciaParaProximoNivel
        views.progressXP?.progress = p.experienciaAtual

        // Vida
        views.lblVida?.text = "❤️ Vida: ${p.vida}/${p.vidaMax}"
        views.progressVida?.max = p.vidaMax
        views.progressVida?.progress = p.vida
        
        if (p.vida < 20) {
            startPulseAnimation(views.progressVida, 400)
        } else {
            views.progressVida?.clearAnimation()
        }

        // Energia
        views.lblEnergia?.let {
            it.text = "⚡ Energia: ${p.energia}/${p.energiaMax}"
        }
        views.progressEnergia?.max = p.energiaMax
        views.progressEnergia?.progress = p.energia

        // Mente
        val mentalEnergia = (p.cansacoMax - p.cansacoMental).coerceAtLeast(0)
        views.lblMente?.text = "🧠 Energia Mental: $mentalEnergia/${p.cansacoMax}"
        views.progressMente?.max = p.cansacoMax
        views.progressMente?.progress = mentalEnergia

        // Padronização de Cores e Alertas Críticos (10%)
        val emRisco = p.energia < p.energiaMax * 0.1 || mentalEnergia < p.cansacoMax * 0.1
        views.txtAvisoColapso?.visibility = if (emRisco) View.VISIBLE else View.GONE

        // Energia
        if (p.energia < p.energiaMax * 0.1) {
            views.progressEnergia?.progressTintList = ColorStateList.valueOf(Color.RED)
            startPulseAnimation(views.progressEnergia, 500)
        } else {
            views.progressEnergia?.progressTintList = ColorStateList.valueOf(Color.parseColor("#FBC02D"))
            views.progressEnergia?.clearAnimation()
        }

        // Mente
        if (mentalEnergia < p.cansacoMax * 0.1) {
            views.progressMente?.progressTintList = ColorStateList.valueOf(Color.RED)
            startPulseAnimation(views.progressMente, 500)
        } else {
            views.progressMente?.progressTintList = ColorStateList.valueOf(Color.parseColor("#7B1FA2"))
            views.progressMente?.clearAnimation()
        }
        
        // Help Buttons
        views.btnHelpNivel?.setOnClickListener {
            showHelp(activity, "⭐ Nível", "Representa seu progresso geral. Ganhe XP em atividades e aventuras para subir de nível e aumentar o limite de seus atributos.")
        }
        views.btnHelpVida?.setOnClickListener {
            showHelp(activity, "❤️ Vida", "Representa sua condição física. Se chegar a zero, determinadas consequências podem ocorrer durante o jogo.")
        }
        views.btnHelpEnergia?.setOnClickListener {
            showHelp(activity, "⚡ Energia", "Representa sua disposição física para realizar atividades durante o dia.")
        }
        views.btnHelpMente?.setOnClickListener {
            showHelp(activity, "🧠 Energia Mental", "Representa sua capacidade mental para estudar e realizar atividades intelectuais.")
        }
    }

    private fun startPulseAnimation(view: View?, dur: Long) {
        if (view != null && view.animation == null) {
            val anim = AlphaAnimation(1f, 0.4f).apply {
                duration = dur
                repeatCount = Animation.INFINITE
                repeatMode = Animation.REVERSE
            }
            view.startAnimation(anim)
        }
    }

    private fun showHelp(activity: Activity, title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Entendi", null)
            .show()
    }

    fun ampliarAvatar(activity: Activity) {
        val p = PlayerManager.player
        val avatarValido = CollectionRepository.isAvatarValidoParaPlayer(p.avatarEquipadoId, p)
        val avatarEquipado = if (avatarValido) CollectionRepository.getAvatarById(p.avatarEquipadoId) else null

        if (avatarEquipado != null) {
            ViewUtils.showZoomDialog(
                activity,
                avatarEquipado.imagemRes,
                avatarEquipado.nome,
                "Nível ${avatarEquipado.nivelRequisito}"
            )
        } else {
            val resId = if (p.sexo == "Masculino") R.drawable.homem else R.drawable.mulher
            ViewUtils.showZoomDialog(
                activity,
                resId,
                "Avatar Original",
                "Sempre disponível"
            )
        }
    }

    fun configurarExibicao(
        views: HudViews,
        showAvatar: Boolean = true,
        showXP: Boolean = true,
        showVida: Boolean = true,
        showEnergia: Boolean = true,
        showMente: Boolean = true,
        showDinheiro: Boolean = true,
        showTempo: Boolean = true
    ) {
        views.imgPersonagem?.visibility = if (showAvatar) View.VISIBLE else View.GONE
        views.txtNomePlayer?.visibility = if (showAvatar) View.VISIBLE else View.GONE
        views.hudLayoutHeader?.visibility = if (showAvatar || showDinheiro || showTempo) View.VISIBLE else View.GONE
        
        views.hudLayoutXP?.visibility = if (showXP) View.VISIBLE else View.GONE
        views.hudLayoutVida?.visibility = if (showVida) View.VISIBLE else View.GONE
        views.hudLayoutEnergia?.visibility = if (showEnergia) View.VISIBLE else View.GONE
        views.hudLayoutMente?.visibility = if (showMente) View.VISIBLE else View.GONE
        views.txtDinheiro?.visibility = if (showDinheiro) View.VISIBLE else View.GONE
        views.txtTempo?.visibility = if (showTempo) View.VISIBLE else View.GONE
        
        val showBars = showXP || showVida || showEnergia || showMente
        views.hudDivider?.visibility = if (showBars) View.VISIBLE else View.GONE
    }
}
