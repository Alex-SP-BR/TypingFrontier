package com.typingfrontier

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TrainingActivity : AppCompatActivity() {

    private lateinit var txtLogTreino: TextView
    private lateinit var txtStatusForca: TextView
    private lateinit var txtStatusResistencia: TextView
    private lateinit var txtStatusVelocidade: TextView
    
    private lateinit var progressForca: ProgressBar
    private lateinit var progressResistencia: ProgressBar
    private lateinit var progressVelocidade: ProgressBar

    // VITAIS
    private lateinit var progressVitalEnergia: ProgressBar
    private lateinit var progressVitalMente: ProgressBar
    private lateinit var txtVitalEnergia: TextView
    private lateinit var txtVitalMente: TextView
    private lateinit var txtAvisoColapso: TextView
    private lateinit var txtVitalDinheiro: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        vincularViews()
        configurarBotoes()
        atualizarPainel()
        
        SoundManager.play(this, "acao")
    }

    private fun vincularViews() {
        txtLogTreino = findViewById(R.id.txtLogTreino)
        txtStatusForca = findViewById(R.id.txtStatusForca)
        txtStatusResistencia = findViewById(R.id.txtStatusResistencia)
        txtStatusVelocidade = findViewById(R.id.txtStatusVelocidade)
        
        progressForca = findViewById(R.id.progressForca)
        progressResistencia = findViewById(R.id.progressResistencia)
        progressVelocidade = findViewById(R.id.progressVelocidade)

        progressVitalEnergia = findViewById(R.id.progressVitalEnergia)
        progressVitalMente = findViewById(R.id.progressVitalMente)
        txtVitalEnergia = findViewById(R.id.txtVitalEnergia)
        txtVitalMente = findViewById(R.id.txtVitalMente)
        txtAvisoColapso = findViewById(R.id.txtAvisoColapso)
        txtVitalDinheiro = findViewById(R.id.txtVitalDinheiro)
    }

    private fun configurarBotoes() {
        // Academia (Força)
        findViewById<Button>(R.id.btnForcaLeve).setOnClickListener { treinar("FORCA", "LEVE") }
        findViewById<Button>(R.id.btnForcaMedio).setOnClickListener { treinar("FORCA", "MEDIO") }
        findViewById<Button>(R.id.btnForcaPesado).setOnClickListener { treinar("FORCA", "PESADO") }

        // Parque (Resistência)
        findViewById<Button>(R.id.btnResLeve).setOnClickListener { treinar("RESISTENCIA", "LEVE") }
        findViewById<Button>(R.id.btnResMedio).setOnClickListener { treinar("RESISTENCIA", "MEDIO") }
        findViewById<Button>(R.id.btnResPesado).setOnClickListener { treinar("RESISTENCIA", "PESADO") }

        // Pista (Velocidade)
        findViewById<Button>(R.id.btnVelLeve).setOnClickListener { treinar("VELOCIDADE", "LEVE") }
        findViewById<Button>(R.id.btnVelMedio).setOnClickListener { treinar("VELOCIDADE", "MEDIO") }
        findViewById<Button>(R.id.btnVelPesado).setOnClickListener { treinar("VELOCIDADE", "PESADO") }

        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }
    }

    private fun atualizarPainel() {
        val p = PlayerManager.player
        
        txtStatusForca.text = "ACADEMIA (FORÇA: Lv.${p.forca} - ${p.progressoForca}/${p.progressoForcaMax})"
        progressForca.max = p.progressoForcaMax
        progressForca.progress = p.progressoForca
        
        txtStatusResistencia.text = "PARQUE (RESISTÊNCIA: Lv.${p.resistencia} - ${p.progressoResistencia}/${p.progressoResistenciaMax})"
        progressResistencia.max = p.progressoResistenciaMax
        progressResistencia.progress = p.progressoResistencia
        
        txtStatusVelocidade.text = "PISTA (VELOCIDADE: Lv.${p.velocidade} - ${p.progressoVelocidade}/${p.progressoVelocidadeMax})"
        progressVelocidade.max = p.progressoVelocidadeMax
        progressVelocidade.progress = p.progressoVelocidade

        // Atualiza Vitais
        txtVitalDinheiro.text = "💰 R$ ${p.dinheiro}"

        txtVitalEnergia.text = "⚡ ${p.energia}/${p.energiaMax}"
        progressVitalEnergia.max = p.energiaMax
        progressVitalEnergia.progress = p.energia

        // Cansaço Mental como Barra de Consumo (Invertida)
        val mentalEnergia = (p.cansacoMax - p.cansacoMental).coerceAtLeast(0)
        txtVitalMente.text = "🧠 $mentalEnergia/${p.cansacoMax}"
        progressVitalMente.max = p.cansacoMax
        progressVitalMente.progress = mentalEnergia

        if (p.energia < p.energiaMax * 0.1 || mentalEnergia < p.cansacoMax * 0.1) {
            txtAvisoColapso.visibility = android.view.View.VISIBLE
            progressVitalEnergia.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
            progressVitalMente.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
            
            // Pulsa as barras se estiver em risco
            if (progressVitalEnergia.animation == null) {
                val anim = android.view.animation.AlphaAnimation(1f, 0.4f).apply {
                    duration = 500
                    repeatCount = android.view.animation.Animation.INFINITE
                    repeatMode = android.view.animation.Animation.REVERSE
                }
                progressVitalEnergia.startAnimation(anim)
                progressVitalMente.startAnimation(anim)
            }
        } else {
            txtAvisoColapso.visibility = android.view.View.GONE
            progressVitalEnergia.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FBC02D"))
            progressVitalMente.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7B1FA2"))
            progressVitalEnergia.clearAnimation()
            progressVitalMente.clearAnimation()
        }
    }

    private fun treinar(atributo: String, intensidade: String) {
        val result = GameEngine.dispatch(GameAction.Train(atributo, intensidade))
        
        when (result) {
            is EngineResult.Success -> {
                txtLogTreino.text = "${result.message}\n${result.extra ?: ""}"
                
                // FEEDBACK VISUAL: Bounce na barra correspondente
                val viewAlvo = when(atributo) {
                    "FORCA" -> progressForca
                    "RESISTENCIA" -> progressResistencia
                    "VELOCIDADE" -> progressVelocidade
                    else -> null
                }
                
                viewAlvo?.let {
                    it.animate().scaleY(1.8f).setDuration(150).withEndAction {
                        it.animate().scaleY(1f).setDuration(100).start()
                    }.start()
                }
            }
            is EngineResult.Failure -> {
                txtLogTreino.text = "⚠️ ${result.message}"
            }
        }
        
        atualizarPainel()
        PlayerManager.save(this)
    }
}
