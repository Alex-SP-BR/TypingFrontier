package com.typingfrontier

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.economy.ProfessionManager

class StatusActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        val player = PlayerManager.player

        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }

        findViewById<TextView>(R.id.txtIdentidade).text =
            "Nome: ${player.nome}\nProfissão: ${player.profissao}\nCidade: ${player.cidadeNascimento}"

        findViewById<TextView>(R.id.txtNivel).text = "Nível ${player.nivel}"

        findViewById<ProgressBar>(R.id.progressNivel).apply {
            max = player.experienciaParaProximoNivel
            progress = player.experienciaAtual
        }
        findViewById<TextView>(R.id.txtXpPercentual).text = 
            "Experiência: ${player.experienciaAtual} / ${player.experienciaParaProximoNivel}"

        // ATRIBUTOS E BARRAS
        setupAtributo(R.id.txtForca, R.id.progressForca, "Força", player.forca, player.progressoForca, player.progressoForcaMax)
        setupAtributo(R.id.txtVelocidade, R.id.progressVelocidade, "Velocidade", player.velocidade, player.progressoVelocidade, player.progressoVelocidadeMax)
        setupAtributo(R.id.txtInteligencia, R.id.progressInteligencia, "Inteligência", player.inteligencia, player.progressoInteligencia, player.progressoInteligenciaMax)
        setupAtributo(R.id.txtResistencia, R.id.progressResistencia, "Resistência", player.resistencia, player.progressoResistencia, player.progressoResistenciaMax)
        setupAtributo(R.id.txtCarisma, R.id.progressCarisma, "Carisma", player.carisma, player.progressoCarisma, player.progressoCarismaMax)

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

        // EQUIPAMENTO
        val equip = ProfessionManager.getEquipment(player.equipamentoId)
        findViewById<TextView>(R.id.txtEquipamentosDetalhe).text = if (equip != null) {
            "${equip.nome}\n${equip.descricao}\nBônus: +${equip.bonus} em ${equip.atributoAlvo}"
        } else {
            "Nenhum equipamento equipado."
        }
        
        if (player.temBlessing) {
            findViewById<TextView>(R.id.txtEquipamentosDetalhe).append("\n\n🕊️ Benção de Proteção Ativa")
        }
    }

    private fun showHelp(titulo: String, mensagem: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensagem)
            .setPositiveButton("Entendi", null)
            .show()
    }

    private fun setupAtributo(textId: Int, progressId: Int, nome: String, nivel: Int, progresso: Int, maxProg: Int) {
        findViewById<TextView>(textId).text = "$nome: Lv. $nivel ($progresso/$maxProg)"
        findViewById<ProgressBar>(progressId).apply {
            max = maxProg
            progress = progresso
        }
    }
}
