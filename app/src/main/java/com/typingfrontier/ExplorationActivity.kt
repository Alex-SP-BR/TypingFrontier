package com.typingfrontier

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.exploration.*

class ExplorationActivity : AppCompatActivity() {

    private lateinit var layoutSelecao: View
    private lateinit var layoutAventura: View
    private lateinit var txtZonaNome: TextView
    private lateinit var txtEtapa: TextView
    private lateinit var txtDescricao: TextView
    private lateinit var txtResultado: TextView
    private lateinit var btnIrMaisFundo: Button
    private lateinit var btnSairLoot: Button
    private lateinit var btnFinalizar: Button
    
    private lateinit var progressEnergia: ProgressBar
    private lateinit var progressMente: ProgressBar
    private lateinit var txtDinheiro: TextView
    private lateinit var txtAviso: TextView

    private var zonaAtual: ExplorationZone? = null
    private var etapaAtual = 0
    private var xpAcumulado = 0
    private var dinheiroAcumulado = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exploration)

        vincularViews()
        configurarRecycler()
        atualizarHUD()

        findViewById<Button>(R.id.btnVoltarMapa).setOnClickListener { finish() }
    }

    private fun vincularViews() {
        layoutSelecao = findViewById(R.id.layoutSelecaoZona)
        layoutAventura = findViewById(R.id.layoutAventura)
        txtZonaNome = findViewById(R.id.txtZonaNome)
        txtEtapa = findViewById(R.id.txtEtapaAventura)
        txtDescricao = findViewById(R.id.txtDescricaoEvento)
        txtResultado = findViewById(R.id.txtResultadoEvento)
        btnIrMaisFundo = findViewById(R.id.btnIrMaisFundo)
        btnSairLoot = findViewById(R.id.btnSairComLoot)
        btnFinalizar = findViewById(R.id.btnFinalizar)

        progressEnergia = findViewById(R.id.progressVitalEnergia)
        progressMente = findViewById(R.id.progressVitalMente)
        txtDinheiro = findViewById(R.id.txtVitalDinheiro)
        txtAviso = findViewById(R.id.txtAvisoColapso)
    }

    private fun configurarRecycler() {
        val rv = findViewById<RecyclerView>(R.id.rvZonas)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ExplorationZoneAdapter(ExplorationZoneRepository.zonas) { zona ->
            if (zona.id == "rio_construcao") {
                Toast.makeText(this, "Rio de Janeiro — Em construção. Esta região será disponibilizada em uma futura expansão.", Toast.LENGTH_LONG).show()
                return@ExplorationZoneAdapter
            }

            if (PlayerManager.player.nivel >= zona.nivelMinimo) {
                iniciarExploracao(zona)
            } else {
                Toast.makeText(this, "Nível insuficiente!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarExploracao(zona: ExplorationZone) {
        zonaAtual = zona
        etapaAtual = 1
        xpAcumulado = 0
        dinheiroAcumulado = 0

        layoutSelecao.visibility = View.GONE
        layoutAventura.visibility = View.VISIBLE
        txtZonaNome.text = zona.nome
        txtEtapa.text = "Entrada"
        
        SoundManager.play(this, "suspense")

        val p = PlayerManager.player
        val temaProfissao = when(p.profissao) {
            "Policial" -> "Você observa o local com cautela tática, procurando por ameaças."
            "Médico" -> "Você analisa o ambiente procurando por suprimentos ou pessoas feridas."
            "Professor" -> "Você tenta ler os sinais sociais e a história do lugar."
            "Detetive" -> "Seus olhos buscam por pistas e inconsistências no cenário."
            "Engenheiro" -> "Você avalia a estrutura e as máquinas ao redor."
            else -> "Você entra silenciosamente no local..."
        }
        
        txtDescricao.text = "Você chegou ao ${zona.nome}.\n\n$temaProfissao\n\nO que deseja fazer?"
        txtResultado.text = "Risco Inicial: ${zona.riscoBase}%"
        txtResultado.setTextColor(android.graphics.Color.GRAY)

        btnIrMaisFundo.text = "COMEÇAR BUSCA"
        btnIrMaisFundo.visibility = View.VISIBLE
        btnSairLoot.visibility = View.VISIBLE
        btnSairLoot.text = "DESISTIR E VOLTAR"
        btnFinalizar.visibility = View.GONE

        btnIrMaisFundo.setOnClickListener { 
            btnIrMaisFundo.text = "IR MAIS FUNDO ➡"
            btnSairLoot.text = "FUGIR COM O QUE TENHO 🏃"
            proximaEtapa() 
        }
        btnSairLoot.setOnClickListener { 
            if (etapaAtual == 1 && xpAcumulado == 0) finish() else finalizarComSucesso() 
        }
        btnFinalizar.setOnClickListener { finish() }
    }

    private fun proximaEtapa() {
        val p = PlayerManager.player
        val zona = zonaAtual ?: return

        if (etapaAtual > 5) {
            finalizarComSucesso()
            return
        }

        val result = GameEngine.dispatch(GameAction.Explore(zona.id))
        
        when (result) {
            is EngineResult.Failure -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                if (xpAcumulado > 0) finalizarComSucesso() else finish()
                return
            }
            is EngineResult.Success -> {
                atualizarHUD()
            }
        }

        txtEtapa.text = "Etapa $etapaAtual / 5"
        
        val sucesso = ExplorationManager.calcularSucesso(p, zona, etapaAtual)

        if (sucesso) {
            val recompensa = ExplorationManager.gerarRecompensa(p, zona, etapaAtual)
            val xp = recompensa["xp"] ?: 0
            val grana = recompensa["dinheiro"] ?: 0
            
            xpAcumulado += xp
            dinheiroAcumulado += grana

            val zona = zonaAtual ?: return
            val acaoProfissao = ExplorationManager.gerarDescricaoSucesso(p.profissao, etapaAtual, zona)

            txtDescricao.text = "✅ $acaoProfissao"
            txtResultado.text = "Acumulado: +$xpAcumulado XP | R$ $dinheiroAcumulado"
            txtResultado.setTextColor(android.graphics.Color.GREEN)
            
            if (etapaAtual == 5) {
                btnIrMaisFundo.text = "FINALIZAR EXPLORAÇÃO"
            }
            etapaAtual++
        } else {
            val msgHospital = ExplorationManager.processarFalhaCritica(p)
            txtDescricao.text = "❌ VOCÊ FOI DERROTADO!\n$msgHospital"
            txtResultado.text = "Você perdeu tudo o que coletou nesta zona."
            txtResultado.setTextColor(android.graphics.Color.RED)
            
            btnIrMaisFundo.visibility = View.GONE
            btnSairLoot.visibility = View.GONE
            btnFinalizar.visibility = View.VISIBLE
            
            PlayerManager.save(this)
        }
    }

    private fun finalizarComSucesso() {
        val result = GameEngine.dispatch(GameAction.CollectRewards(xpAcumulado, dinheiroAcumulado))
        
        atualizarHUD() 
        
        txtDinheiro.animate().scaleX(1.4f).scaleY(1.4f).setDuration(200).withEndAction {
            txtDinheiro.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
        }.start()

        txtDescricao.text = "🏆 VITÓRIA!\nVocê saiu da zona com vida.\n\nColetou $xpAcumulado XP e R$ $dinheiroAcumulado."
        
        btnIrMaisFundo.visibility = View.GONE
        btnSairLoot.visibility = View.GONE
        btnFinalizar.visibility = View.VISIBLE
        btnFinalizar.text = "VOLTAR AO MAPA"
        
        PlayerManager.save(this)
    }

    private fun atualizarHUD() {
        val p = PlayerManager.player
        progressEnergia.max = p.energiaMax
        progressEnergia.progress = p.energia
        
        // Cansaço Mental como Barra de Consumo (Invertida)
        val mentalEnergia = (p.cansacoMax - p.cansacoMental).coerceAtLeast(0)
        progressMente.max = p.cansacoMax
        progressMente.progress = mentalEnergia
        
        txtDinheiro.text = "💰 R$ ${p.dinheiro}"

        if (p.energia < p.energiaMax * 0.1 || mentalEnergia < p.cansacoMax * 0.1) {
            txtAviso.visibility = View.VISIBLE
            val anim = android.view.animation.AlphaAnimation(1f, 0.4f).apply {
                duration = 500
                repeatCount = android.view.animation.Animation.INFINITE
                repeatMode = android.view.animation.Animation.REVERSE
            }

            if (p.energia < p.energiaMax * 0.1) {
                progressEnergia.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
                if (progressEnergia.animation == null) progressEnergia.startAnimation(anim)
            } else {
                progressEnergia.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FBC02D"))
                progressEnergia.clearAnimation()
            }

            if (mentalEnergia < p.cansacoMax * 0.1) {
                progressMente.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
                if (progressMente.animation == null) progressMente.startAnimation(anim)
            } else {
                progressMente.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7B1FA2"))
                progressMente.clearAnimation()
            }
        } else {
            txtAviso.visibility = View.GONE
            progressEnergia.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FBC02D"))
            progressMente.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7B1FA2"))
            progressEnergia.clearAnimation()
            progressMente.clearAnimation()
        }
    }
}
