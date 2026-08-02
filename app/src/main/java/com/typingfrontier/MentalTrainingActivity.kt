package com.typingfrontier

import android.app.AlertDialog
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.mental.*

class MentalTrainingActivity : AppCompatActivity() {

    private lateinit var txtAssuntoAtual: TextView
    private lateinit var txtQuestao: TextView
    private lateinit var txtFeedback: TextView
    private lateinit var edtResposta: EditText
    private lateinit var btnResponder: Button
    
    private lateinit var progressInteligencia: ProgressBar
    private lateinit var progressCarisma: ProgressBar
    private lateinit var txtProgressoInteligencia: TextView
    private lateinit var txtProgressoCarisma: TextView

    // VITAIS
    private lateinit var progressVitalEnergia: ProgressBar
    private lateinit var progressVitalMente: ProgressBar
    private lateinit var txtVitalEnergia: TextView
    private lateinit var txtVitalMente: TextView
    private lateinit var txtAvisoColapso: TextView
    private lateinit var txtVitalDinheiro: TextView

    private var perguntaAtual: PortugueseQuestion? = null
    private var perguntaMatematicaAtual: MathQuestion? = null
    private var tipoTreinoAtual: TipoTreino = TipoTreino.MATEMATICA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mental_training)

        vincularViews()
        configurarCliques()
        
        mostrarDialogoTreino()
        atualizarBarras()

        SoundManager.play(this, "foco")
    }

    private fun vincularViews() {
        txtAssuntoAtual = findViewById(R.id.txtAssuntoAtual)
        txtQuestao = findViewById(R.id.txtQuestao)
        txtFeedback = findViewById(R.id.txtFeedback)
        edtResposta = findViewById(R.id.edtResposta)
        btnResponder = findViewById(R.id.btnResponder)
        
        progressInteligencia = findViewById(R.id.progressInteligencia)
        progressCarisma = findViewById(R.id.progressCarisma)
        txtProgressoInteligencia = findViewById(R.id.txtProgressoInteligencia)
        txtProgressoCarisma = findViewById(R.id.txtProgressoCarisma)

        progressVitalEnergia = findViewById(R.id.progressVitalEnergia)
        progressVitalMente = findViewById(R.id.progressVitalMente)
        txtVitalEnergia = findViewById(R.id.txtVitalEnergia)
        txtVitalMente = findViewById(R.id.txtVitalMente)
        txtAvisoColapso = findViewById(R.id.txtAvisoColapso)
        txtVitalDinheiro = findViewById(R.id.txtVitalDinheiro)
    }

    private fun configurarCliques() {
        btnResponder.setOnClickListener { verificarResposta() }
        
        findViewById<Button>(R.id.btnTrocarAssunto).setOnClickListener {
            mostrarDialogoTreino()
        }

        findViewById<Button>(R.id.btnSair).setOnClickListener { finish() }

        edtResposta.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                verificarResposta()
                true
            } else false
        }
    }

    private fun mostrarDialogoTreino() {
        val opcoes = arrayOf("Matemática (Inteligência)", "Português (Carisma)")
        AlertDialog.Builder(this)
            .setTitle("O que vamos treinar agora?")
            .setCancelable(false)
            .setItems(opcoes) { _, which ->
                tipoTreinoAtual = if (which == 0) TipoTreino.MATEMATICA else TipoTreino.PORTUGUES
                txtAssuntoAtual.text = if (which == 0) "Treinando: Matemática" else "Treinando: Português"
                iniciarNovaQuestao()
            }
            .show()
    }

    private fun iniciarNovaQuestao() {
        val p = PlayerManager.player
        
        btnResponder.isEnabled = true
        edtResposta.setText("")
        
        when (tipoTreinoAtual) {
            TipoTreino.MATEMATICA -> {
                perguntaAtual = null
                perguntaMatematicaAtual = MathGenerator.gerar(p.inteligencia)
                txtQuestao.text = perguntaMatematicaAtual?.pergunta
                // Habilita teclado numérico apenas para Matemática (Inteligência)
                edtResposta.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            }
            TipoTreino.PORTUGUES -> {
                perguntaMatematicaAtual = null
                perguntaAtual = PortugueseGenerator.gerar(p.carisma)
                txtQuestao.text = perguntaAtual?.pergunta
                // Habilita teclado de texto para Português (Carisma)
                edtResposta.inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
        }
        
        edtResposta.requestFocus()
    }

    private fun verificarResposta() {
        val resposta = edtResposta.text.toString().trim()
        if (resposta.isEmpty()) return

        val atributoAlvo = if (tipoTreinoAtual == TipoTreino.MATEMATICA) "INTELIGENCIA" else "CARISMA"
        
        val isCorrect = if (tipoTreinoAtual == TipoTreino.MATEMATICA) {
            resposta == perguntaMatematicaAtual?.respostaCorreta.toString()
        } else {
            val q = perguntaAtual ?: return
            val baseMatch = resposta.equals(q.respostaCorreta, ignoreCase = true)
            
            // Se não for o sinônimo principal, verifica no mapa de sinônimos aceitáveis
            baseMatch || PortugueseGenerator.validarSinonimo(q, resposta)
        }

        val action = if (isCorrect) GameAction.Train(atributoAlvo, "LEVE") else GameAction.StudyError(atributoAlvo)
        val result = GameEngine.dispatch(action)
        
        when (result) {
            is EngineResult.Success -> {
                if (isCorrect) {
                    txtFeedback.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                    txtFeedback.text = "✅ Correto! ${result.message}"
                    
                    // FEEDBACK VISUAL: Bounce na barra de atributo correspondente
                    val viewAlvo = if (tipoTreinoAtual == TipoTreino.MATEMATICA) progressInteligencia else progressCarisma
                    viewAlvo.animate().scaleY(1.5f).scaleX(1.02f).setDuration(150).withEndAction {
                        viewAlvo.animate().scaleY(1f).scaleX(1f).setDuration(100).start()
                    }.start()
                } else {
                    txtFeedback.setTextColor(android.graphics.Color.parseColor("#C62828"))
                    val correta = if (tipoTreinoAtual == TipoTreino.MATEMATICA) {
                        val q = perguntaMatematicaAtual?.pergunta ?: ""
                        val formatada = if (q.contains("?")) q.replace("?", perguntaMatematicaAtual?.respostaCorreta.toString()) else "${perguntaMatematicaAtual?.respostaCorreta}"
                        "O correto é:\n$formatada"
                    } else {
                        "Resposta correta: \"${perguntaAtual?.respostaCorreta}\""
                    }
                    txtFeedback.text = "❌ Resposta incorreta.\n$correta"
                }
            }
            is EngineResult.Failure -> {
                txtFeedback.setTextColor(android.graphics.Color.parseColor("#C62828"))
                txtFeedback.text = "⚠️ ${result.message}"
                btnResponder.isEnabled = false
            }
        }

        atualizarBarras()
        PlayerManager.save(this)
        if (btnResponder.isEnabled) iniciarNovaQuestao()
    }

    private fun atualizarBarras() {
        val p = PlayerManager.player
        progressInteligencia.max = p.progressoInteligenciaMax
        progressInteligencia.progress = p.progressoInteligencia
        
        progressCarisma.max = p.progressoCarismaMax
        progressCarisma.progress = p.progressoCarisma
        
        txtProgressoInteligencia.text = "Lv.${p.inteligencia} ${p.progressoInteligencia}/${p.progressoInteligenciaMax}"
        txtProgressoCarisma.text = "Lv.${p.carisma} ${p.progressoCarisma}/${p.progressoCarismaMax}"

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
}
