package com.typingfrontier

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.mental.*
import com.typingfrontier.utils.CurrencyUtils

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

    // ÁUDIO RECOMPENSA
    private var soundPool: SoundPool? = null
    private var soundIdA: Int = 0
    private var soundIdB: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mental_training)

        vincularViews()
        configurarCliques()
        
        mostrarDialogoTreino()
        atualizarBarras()

        SoundManager.play(this, "foco")
        prepararSons()
    }

    private fun prepararSons() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()
        
        soundIdA = soundPool?.load(this, R.raw.coin_sound_a, 1) ?: 0
        soundIdB = soundPool?.load(this, R.raw.coin_sound_b, 1) ?: 0
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

        txtVitalDinheiro.setOnClickListener {
            CurrencyUtils.mostrarSaldoExato(this, PlayerManager.player.dinheiro)
        }
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
        MaterialAlertDialogBuilder(this, R.style.Theme_TypingFrontier_MentalDialog)
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
                // Habilita teclado numérico com suporte a decimais e sinais para Matemática
                edtResposta.inputType = android.text.InputType.TYPE_CLASS_NUMBER or 
                                      android.text.InputType.TYPE_NUMBER_FLAG_SIGNED or 
                                      android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
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
        val respostaRaw = edtResposta.text.toString().trim()
        if (respostaRaw.isEmpty()) return

        val atributoAlvo = if (tipoTreinoAtual == TipoTreino.MATEMATICA) "INTELIGENCIA" else "CARISMA"
        
        val isCorrect = if (tipoTreinoAtual == TipoTreino.MATEMATICA) {
            val questao = perguntaMatematicaAtual ?: return
            
            // Normalização: substitui vírgula por ponto para conversão Double
            val respostaNormalizada = respostaRaw.replace(",", ".")
            val respostaDouble = respostaNormalizada.toDoubleOrNull()
            
            if (respostaDouble == null) {
                // Entrada inválida (não numérica)
                txtFeedback.setTextColor(android.graphics.Color.parseColor("#FF8A80"))
                txtFeedback.text = "⚠️ Resposta inválida. Use apenas números."
                return
            }

            // Comparação com tolerância (Epsilon) para evitar erros de ponto flutuante
            Math.abs(respostaDouble - questao.respostaCorreta) < 0.0001
        } else {
            val q = perguntaAtual ?: return
            val baseMatch = respostaRaw.equals(q.respostaCorreta, ignoreCase = true)
            
            // Se não for o sinônimo principal, verifica no mapa de sinônimos aceitáveis
            baseMatch || PortugueseGenerator.validarSinonimo(q, respostaRaw)
        }

        val action = if (isCorrect) GameAction.Train(atributoAlvo, "LEVE") else GameAction.StudyError(atributoAlvo)
        val result = GameEngine.dispatch(action)
        
        when (result) {
            is EngineResult.Success -> {
                if (isCorrect) {
                    txtFeedback.setTextColor(android.graphics.Color.parseColor("#81C784"))
                    txtFeedback.text = "🏆 Correto! ${result.message}"

                    // RECOMPENSA EXTRA POR DIFICULDADE
                    val bonus = calcularBonusDificuldade()
                    if (bonus > 0) {
                        GameEngine.dispatch(GameAction.CollectRewards(0, bonus))

                        // Remove a moeda como compound drawable para preservar o troféu de mérito
                        txtFeedback.setCompoundDrawables(null, null, null, null)

                        txtFeedback.append("\nBônus de Dificuldade: +$bonus Frons!")
                        dispararAnimacaoMoeda(bonus)
                    }
                    
                    // FEEDBACK VISUAL: Bounce na barra de atributo correspondente
                    val viewAlvo = if (tipoTreinoAtual == TipoTreino.MATEMATICA) progressInteligencia else progressCarisma
                    viewAlvo.animate().scaleY(1.5f).scaleX(1.02f).setDuration(150).withEndAction {
                        viewAlvo.animate().scaleY(1f).scaleX(1f).setDuration(100).start()
                    }.start()
                } else {
                    txtFeedback.setTextColor(android.graphics.Color.parseColor("#FF8A80"))
                    
                    if (tipoTreinoAtual == TipoTreino.MATEMATICA) {
                        val questao = perguntaMatematicaAtual
                        val solucao = if (questao != null) MathSolver.solve(questao) else null
                        
                        if (solucao != null) {
                            // Exibe a resolução estruturada do MathSolver
                            val sb = StringBuilder("❌ Resposta incorreta.\n\n")
                            solucao.passos.forEach { sb.append("$it\n") }
                            
                            val respFormatada = if (solucao.resultadoFinal % 1 == 0.0) 
                                solucao.resultadoFinal.toInt().toString() 
                            else 
                                solucao.resultadoFinal.toString().replace(".", ",")
                                
                            sb.append("\nResultado: $respFormatada")
                            txtFeedback.text = sb.toString().trim()
                        } else {
                            // Fallback para categorias sem solver ainda
                            val q = questao?.pergunta ?: ""
                            val respFormatada = if (questao?.respostaCorreta?.rem(1) == 0.0) 
                                questao.respostaCorreta.toInt().toString() 
                            else 
                                questao?.respostaCorreta.toString().replace(".", ",")

                            val perguntaFormatada = if (q.contains("?")) q.replace("?", respFormatada) else respFormatada
                            txtFeedback.text = "❌ Resposta incorreta.\nO correto é:\n$perguntaFormatada"
                        }
                    } else {
                        txtFeedback.text = "❌ Resposta incorreta.\nResposta correta: \"${perguntaAtual?.respostaCorreta}\""
                    }
                }
            }
            is EngineResult.Failure -> {
                txtFeedback.setTextColor(android.graphics.Color.parseColor("#FF8A80"))
                txtFeedback.text = "⚠️ ${result.message}"
                btnResponder.isEnabled = false
            }
        }

        atualizarBarras()
        
        // 🏆 GATILHO DE CONQUISTA: MENTAL
        com.typingfrontier.collection.AchievementManager.checkMental(this, isCorrect, tipoTreinoAtual.name)

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

        val hudViews = com.typingfrontier.utils.HudHelper.HudViews(findViewById(android.R.id.content))
        com.typingfrontier.utils.HudHelper.atualizar(this, hudViews, com.typingfrontier.HudSettingsManager.HudCategory.TRAINING_MENTAL)
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }

    /**
     * Calcula a recompensa adicional baseada no tipo de exercício e no patamar do jogador.
     */
    private fun calcularBonusDificuldade(): Int {
        val p = PlayerManager.player
        val dificuldade: String // "NORMAL", "MEDIO", "DIFICIL", "MUITO_DIFICIL"
        val patamar: Int = if (tipoTreinoAtual == TipoTreino.MATEMATICA) p.inteligencia else p.carisma

        dificuldade = if (tipoTreinoAtual == TipoTreino.MATEMATICA) {
            val tipo = perguntaMatematicaAtual?.tipo ?: return 0
            when (tipo) {
                MathExerciseType.OPERACOES_COMBINADAS,
                MathExerciseType.PORCENTAGEM,
                MathExerciseType.DECIMAIS -> "DIFICIL"

                MathExerciseType.FRACOES,
                MathExerciseType.PROBLEMAS -> "MUITO_DIFICIL"

                MathExerciseType.RADICIACAO,
                MathExerciseType.EQUACAO,
                MathExerciseType.POTENCIACAO,
                MathExerciseType.SEQUENCIAS -> "MEDIO"

                else -> "NORMAL"
            }
        } else {
            val tipo = perguntaAtual?.tipo ?: return 0
            when (tipo) {
                PortugueseExerciseType.OBJETO_DIRETO,
                PortugueseExerciseType.CLASSE_GRAMATICAL -> "DIFICIL"

                PortugueseExerciseType.VOCABULARIO,
                PortugueseExerciseType.INTERPRETACAO -> "MUITO_DIFICIL"

                PortugueseExerciseType.SUJEITO,
                PortugueseExerciseType.VERBO,
                PortugueseExerciseType.ORTOGRAFIA,
                PortugueseExerciseType.ACENTUACAO -> "MEDIO"

                else -> "NORMAL"
            }
        }

        // NORMAL e MEDIO não recebem bônus
        if (dificuldade == "NORMAL" || dificuldade == "MEDIO") return 0

        // Matriz de Recompensa
        return when {
            patamar <= 40 -> if (dificuldade == "DIFICIL") 10 else 20
            patamar <= 80 -> if (dificuldade == "DIFICIL") 25 else 50
            patamar <= 150 -> if (dificuldade == "DIFICIL") 75 else 125
            else -> if (dificuldade == "DIFICIL") 150 else 250
        }
    }

    /**
     * Executa a animação visual da sequência de 4 moedas (1 PNG + 3 MP4) voando ao destino.
     */
    private fun dispararAnimacaoMoeda(bonus: Int) {
        if (bonus <= 0) return

        val container = findViewById<FrameLayout>(android.R.id.content) ?: return
        val density = resources.displayMetrics.density
        val coinSize = (22 * density).toInt()

        // Definição das 4 moedas (Recurso ID, IsVideo)
        val moedasConfig = listOf(
            R.drawable.fron_coin to false,
            R.raw.fron_coin_animation_1 to true,
            R.raw.fron_coin_animation_2 to true,
            R.raw.fron_coin_animation_3 to true
        )

        // Captura coordenadas base uma única vez para a sequência
        val startLoc = IntArray(2)
        txtFeedback.getLocationInWindow(startLoc)

        val endLoc = IntArray(2)
        txtVitalDinheiro.getLocationInWindow(endLoc)

        val rootLoc = IntArray(2)
        container.getLocationInWindow(rootLoc)

        moedasConfig.forEachIndexed { index, (resId, isVideo) ->
            container.postDelayed({
                val coinView = if (isVideo) {
                    VideoView(this@MentalTrainingActivity).apply {
                        setVideoURI(android.net.Uri.parse("android.resource://$packageName/$resId"))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            try { mp.setVolume(0f, 0f) } catch (e: Exception) {}
                        }
                        start()
                    }
                } else {
                    ImageView(this@MentalTrainingActivity).apply {
                        setImageResource(resId)
                    }
                }

                coinView.layoutParams = FrameLayout.LayoutParams(coinSize, coinSize)
                coinView.alpha = 0f
                container.addView(coinView)

                // Espaçamento inicial (leque) para não ficarem sobrepostas
                val spreadX = (index - 1.5f) * 15f * density
                val spreadY = (if (index % 2 == 0) -10f else 10f) * density

                val startX = startLoc[0] - rootLoc[0] + (txtFeedback.width / 2f) - (coinSize / 2f) + spreadX
                val startY = startLoc[1] - rootLoc[1] + (txtFeedback.height / 2f) - (coinSize / 2f) + spreadY

                val endX = endLoc[0] - rootLoc[0] + (txtVitalDinheiro.width / 2f) - (coinSize / 2f)
                val endY = endLoc[1] - rootLoc[1] + (txtVitalDinheiro.height / 2f) - (coinSize / 2f)

                coinView.x = startX
                coinView.y = startY

                // Execução da animação de voo
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

            }, index * 200L) // Intervalo de 200ms entre o surgimento de cada moeda
        }
    }
}
