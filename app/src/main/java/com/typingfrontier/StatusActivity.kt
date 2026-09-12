package com.typingfrontier

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.economy.ProfessionManager
import com.typingfrontier.collection.CollectionRepository

class StatusActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        val player = PlayerManager.player

        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }

        findViewById<TextView>(R.id.txtIdentidade).text =
            "Nome: ${player.nome}\nProfissão: ${player.profissao}\nCidade: ${player.cidadeNascimento}"

        val imgAvatar = findViewById<ImageView>(R.id.imgStatusAvatar)
        val avatarValido = CollectionRepository.isAvatarValidoParaPlayer(player.avatarEquipadoId, player)
        val avatarEquipado = if (avatarValido) CollectionRepository.getAvatarById(player.avatarEquipadoId) else null

        if (avatarEquipado != null) {
            imgAvatar.setImageResource(avatarEquipado.imagemRes)
        } else {
            imgAvatar.setImageResource(
                if (player.sexo == "Masculino") R.drawable.homem
                else R.drawable.mulher
            )
        }

        imgAvatar.setOnClickListener {
            com.typingfrontier.utils.HudHelper.ampliarAvatar(this)
        }

        findViewById<TextView>(R.id.txtNivel).text = "Nível ${player.nivel}"

        findViewById<ProgressBar>(R.id.progressNivel).apply {
            max = player.experienciaParaProximoNivel
            progress = player.experienciaAtual
        }
        findViewById<TextView>(R.id.txtXpPercentual).text = 
            "Experiência: ${player.experienciaAtual} / ${player.experienciaParaProximoNivel}"

        // ATRIBUTOS E BARRAS
        setupAtributo(R.id.txtForca, R.id.progressForca, "Força", player.forca, player.forcaEfetiva, player.progressoForca, player.progressoForcaMax)
        setupAtributo(R.id.txtVelocidade, R.id.progressVelocidade, "Velocidade", player.velocidade, player.velocidadeEfetiva, player.progressoVelocidade, player.progressoVelocidadeMax)
        setupAtributo(R.id.txtInteligencia, R.id.progressInteligencia, "Inteligência", player.inteligencia, player.inteligenciaEfetiva, player.progressoInteligencia, player.progressoInteligenciaMax)
        setupAtributo(R.id.txtResistencia, R.id.progressResistencia, "Resistência", player.resistencia, player.resistenciaEfetiva, player.progressoResistencia, player.progressoResistenciaMax)
        setupAtributo(R.id.txtCarisma, R.id.progressCarisma, "Carisma", player.carisma, player.carismaEfetiva, player.progressoCarisma, player.progressoCarismaMax)

        // HELP BUTTONS
        findViewById<TextView>(R.id.btnHelpNivel).setOnClickListener {
            showHelp("⭐ Nível", "Representa seu desenvolvimento geral. Subir de nível aumenta seus limites de atributos e desbloqueia novos conteúdos.")
        }
        findViewById<TextView>(R.id.btnHelpForca).setOnClickListener {
            showHelp("💪 Força", "Poder físico para atividades pesadas e combates. Influencia o sucesso em desafios de força bruta na exploração.")
        }
        findViewById<TextView>(R.id.btnHelpVelocidade).setOnClickListener {
            showHelp("⚡ Velocidade", "Agilidade e rapidez. Reduz o tempo de certas ações e ajuda a evitar perigos na exploração.")
        }
        findViewById<TextView>(R.id.btnHelpInteligencia).setOnClickListener {
            showHelp("🧠 Inteligência", "Capacidade mental e conhecimento. Melhora o desempenho em estudos e profissões intelectuais.")
        }
        findViewById<TextView>(R.id.btnHelpResistencia).setOnClickListener {
            showHelp("🛡️ Resistência", "Vigor e saúde. Reduz o consumo de energia e protege contra o cansaço excessivo.")
        }
        findViewById<TextView>(R.id.btnHelpCarisma).setOnClickListener {
            showHelp("🗣️ Carisma", "Liderança e influência social. Melhora preços na loja e a relação com NPCs na cidade.")
        }

        atualizarEquipamentos()
        atualizarMochila()
    }

    private fun atualizarMochila() {
        val player = PlayerManager.player
        val layout = findViewById<android.widget.LinearLayout>(R.id.layoutMochila)
        layout.removeAllViews()

        if (player.mochila.isEmpty()) {
            val txtVazia = TextView(this).apply {
                text = "Mochila vazia"
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(android.graphics.Color.parseColor("#889099"))
                setPadding(12, 0, 12, 12)
            }
            layout.addView(txtVazia)
            return
        }

        // Ordenar mochila por nome do equipamento
        val itensOrdenados = player.mochila.keys.sortedBy { id ->
            ProfessionManager.getEquipment(id)?.nome ?: ""
        }

        itensOrdenados.forEach { itemId ->
            val qtd = player.mochila[itemId] ?: 0
            if (qtd <= 0) return@forEach

            val equip = ProfessionManager.getEquipment(itemId)
            
            val itemLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(12, 8, 12, 8)
                isClickable = true
                isFocusable = true
                val typedValue = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                setBackgroundResource(typedValue.resourceId)
                setOnClickListener { confirmarEquipar(itemId) }
            }

            val txtNome = TextView(this).apply {
                val nome = equip?.nome ?: "Item Desconhecido ($itemId)"
                text = if (qtd > 1) "$nome ×$qtd" else nome
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
                setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            }

            itemLayout.addView(txtNome)

            if (equip != null) {
                val txtBonus = TextView(this).apply {
                    text = "+${equip.bonus} ${equip.atributoAlvo}"
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextColor(android.graphics.Color.parseColor("#74C6E0"))
                }
                itemLayout.addView(txtBonus)
            }

            layout.addView(itemLayout)

            // Divider
            val divider = android.view.View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (1 * resources.displayMetrics.density).toInt())
                setBackgroundColor(android.graphics.Color.parseColor("#33FFFFFF"))
            }
            layout.addView(divider)
        }
    }

    private fun atualizarEquipamentos() {
        val player = PlayerManager.player
        val layout = findViewById<android.widget.LinearLayout>(R.id.layoutSlots)
        layout.removeAllViews()

        val slots = listOf("CABEÇA", "PESCOÇO", "CORPO", "MÃO", "ACESSÓRIO", "PÉS")

        slots.forEach { slotName ->
            val itemId = player.slotsEquipados[slotName]
            val equip = if (itemId != null) ProfessionManager.getEquipment(itemId) else null

            val slotView = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(12, 12, 12, 12)
                isClickable = equip != null
                isFocusable = equip != null
                if (equip != null) {
                    val typedValue = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                    setBackgroundResource(typedValue.resourceId)
                    setOnClickListener { confirmarDesequipar(slotName, equip.nome) }
                }
            }

            val txtSlot = TextView(this).apply {
                text = slotName
                textSize = 10f
                setTextColor(android.graphics.Color.parseColor("#889099"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val txtItem = TextView(this).apply {
                text = if (equip != null) equip.nome else "—"
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                setTextColor(if (equip != null) android.graphics.Color.parseColor("#FFFFFF") else android.graphics.Color.parseColor("#889099"))
            }

            slotView.addView(txtSlot)
            slotView.addView(txtItem)

            if (equip != null) {
                val txtBonus = TextView(this).apply {
                    text = "+${equip.bonus} ${equip.atributoAlvo}"
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTextColor(android.graphics.Color.parseColor("#74C6E0"))
                }
                slotView.addView(txtBonus)
            }

            layout.addView(slotView)

            // Divider
            val divider = android.view.View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (1 * resources.displayMetrics.density).toInt())
                setBackgroundColor(android.graphics.Color.parseColor("#33FFFFFF"))
            }
            layout.addView(divider)
        }

        // Blessing
        val hasBlessing = player.temBlessing
        findViewById<android.view.View>(R.id.dividerBlessing).visibility = if (hasBlessing) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<TextView>(R.id.txtBlessingStatus).visibility = if (hasBlessing) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun confirmarDesequipar(slot: String, nomeItem: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Desequipar")
            .setMessage("Deseja desequipar $nomeItem e enviá-lo para a mochila?")
            .setPositiveButton("Sim") { _, _ ->
                val result = GameEngine.dispatch(GameAction.UnequipItem(slot))
                if (result is EngineResult.Success) {
                    atualizarEquipamentos()
                    atualizarMochila()
                    PlayerManager.save(this)
                    android.widget.Toast.makeText(this, result.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    private fun confirmarEquipar(itemId: String) {
        val player = PlayerManager.player
        val equip = ProfessionManager.getEquipment(itemId) ?: return
        val targetSlot = equip.slot ?: return

        val currentInSlotId = player.slotsEquipados[targetSlot]
        val currentEquip = if (currentInSlotId != null) ProfessionManager.getEquipment(currentInSlotId) else null

        val message = StringBuilder()
        message.append("Deseja equipar ${equip.nome}?\n\n")
        message.append("Este item ocupará o slot: $targetSlot\n")
        
        if (currentEquip != null) {
            message.append("\nO item atual (${currentEquip.nome}) voltará para a mochila.")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Equipar Equipamento")
            .setMessage(message.toString())
            .setPositiveButton("Equipar") { _, _ ->
                val result = GameEngine.dispatch(GameAction.EquipItem(itemId))
                if (result is EngineResult.Success) {
                    atualizarEquipamentos()
                    atualizarMochila()
                    PlayerManager.save(this)
                    android.widget.Toast.makeText(this, result.message, android.widget.Toast.LENGTH_SHORT).show()
                } else if (result is EngineResult.Failure) {
                    android.widget.Toast.makeText(this, result.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showHelp(titulo: String, mensagem: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensagem)
            .setPositiveButton("Entendi", null)
            .show()
    }

    private fun setupAtributo(textId: Int, progressId: Int, nome: String, valorBase: Int, valorEfetivo: Int, progresso: Int, maxProg: Int) {
        val bonus = valorEfetivo - valorBase
        val bonusTexto = if (bonus > 0) " (+$bonus)" else ""
        
        findViewById<TextView>(textId).text = "$nome: $valorEfetivo$bonusTexto ($progresso/$maxProg)"
        findViewById<ProgressBar>(progressId).apply {
            max = maxProg
            progress = progresso
        }
    }
}
