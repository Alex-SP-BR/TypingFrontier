package com.typingfrontier

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.exploration.*
import com.typingfrontier.utils.CurrencyUtils
import android.media.AudioAttributes
import android.media.SoundPool
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan

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

    // ÁUDIO E ANIMAÇÃO RECOMPENSA
    private var soundPool: SoundPool? = null
    private var soundIdA: Int = 0
    private var soundIdB: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exploration)

        vincularViews()
        configurarRecycler()
        atualizarHUD()
        prepararSons()

        findViewById<Button>(R.id.btnVoltarMapa).setOnClickListener { finish() }
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

        txtDinheiro.setOnClickListener {
            CurrencyUtils.mostrarSaldoExato(this, PlayerManager.player.dinheiro)
        }
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
        
        atualizarHUD()

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
        txtResultado.setTextColor(android.graphics.Color.parseColor("#889099"))

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

            // Configuração do ícone da moeda para o Spannable (20dp)
            val coinIcon = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.fron_coin)
            val size = (20 * resources.displayMetrics.density).toInt()
            coinIcon?.setBounds(0, 0, size, size)

            val baseText = "Acumulado: +$xpAcumulado XP | "
            val moneyText = CurrencyUtils.formatar(dinheiroAcumulado)
            val fullText = "$baseText $moneyText"
            val spannable = SpannableString(fullText)
            
            val imageSpan = ImageSpan(coinIcon!!, ImageSpan.ALIGN_BOTTOM)
            val start = baseText.length
            spannable.setSpan(imageSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            txtDescricao.text = "✅ $acaoProfissao"
            txtResultado.text = spannable
            txtResultado.setCompoundDrawables(null, null, null, null) // Limpa o ícone antigo se houver
            txtResultado.setTextColor(android.graphics.Color.parseColor("#81C784"))
            
            if (etapaAtual == 5) {
                btnIrMaisFundo.text = "FINALIZAR EXPLORAÇÃO"
            }
            etapaAtual++
        } else {
            val msgHospital = ExplorationManager.processarFalhaCritica(p)
            txtDescricao.text = "❌ VOCÊ FOI DERROTADO!\n$msgHospital"
            txtResultado.text = "Você perdeu tudo o que coletou nesta zona."
            txtResultado.setTextColor(android.graphics.Color.parseColor("#FF8A80"))
            
            btnIrMaisFundo.visibility = View.GONE
            btnSairLoot.visibility = View.GONE
            btnFinalizar.visibility = View.VISIBLE
            
            PlayerManager.save(this)
        }
    }

    private fun finalizarComSucesso() {
        val result = GameEngine.dispatch(GameAction.CollectRewards(xpAcumulado, dinheiroAcumulado))
        
        atualizarHUD() 
        
        txtDescricao.text = "🏆 VITÓRIA!\nVocê saiu da zona com vida.\n\nColetou $xpAcumulado XP e ${CurrencyUtils.formatar(dinheiroAcumulado)}."

        // Dispara a animação das 4 moedas se o depósito foi um sucesso e houver ganho de Frons
        if (result is EngineResult.Success && dinheiroAcumulado > 0) {
            dispararAnimacaoMoeda(dinheiroAcumulado)
        }
        
        btnIrMaisFundo.visibility = View.GONE
        btnSairLoot.visibility = View.GONE
        btnFinalizar.visibility = View.VISIBLE
        btnFinalizar.text = "VOLTAR AO MAPA"
        
        // 🏆 GATILHO DE CONQUISTA (Apenas se completou as 5 etapas com sucesso)
        if (etapaAtual > 5) {
            zonaAtual?.let { com.typingfrontier.collection.AchievementManager.checkExploration(this, it.id) }
        }
        
        PlayerManager.save(this)
    }

    private fun atualizarHUD() {
        val isAventura = layoutAventura.visibility == View.VISIBLE
        val hudRootId = if (isAventura) R.id.includeHudAdventure else R.id.includeHudSelection
        val hudRoot = findViewById<View>(hudRootId) ?: findViewById(android.R.id.content)

        val views = com.typingfrontier.utils.HudHelper.HudViews(hudRoot)
        val category = if (isAventura)
            com.typingfrontier.HudSettingsManager.HudCategory.ADVENTURE 
        else 
            com.typingfrontier.HudSettingsManager.HudCategory.EXPLORE
        com.typingfrontier.utils.HudHelper.atualizar(this, views, category)
    }

    /**
     * Executa a animação visual da sequência de 4 moedas (1 PNG + 3 MP4) voando ao destino.
     * Adaptado da MentalTrainingActivity para o contexto de Exploração.
     */
    private fun dispararAnimacaoMoeda(bonus: Int) {
        if (bonus <= 0) return

        val container = findViewById<FrameLayout>(android.R.id.content) ?: return
        val density = resources.displayMetrics.density
        val coinSize = (22 * density).toInt()

        val moedasConfig = listOf(
            R.drawable.fron_coin to false,
            R.raw.fron_coin_animation_1 to true,
            R.raw.fron_coin_animation_2 to true,
            R.raw.fron_coin_animation_3 to true
        )

        // Origem: txtDescricao (onde está o texto de Vitória/Sucesso)
        val startLoc = IntArray(2)
        txtDescricao.getLocationInWindow(startLoc)

        // Destino: Resolve o txtVitalDinheiro do HUD ativo (Adventure ou Selection)
        val isAventura = layoutAventura.visibility == View.VISIBLE
        val hudRootId = if (isAventura) R.id.includeHudAdventure else R.id.includeHudSelection
        val hudRoot = findViewById<View>(hudRootId)
        val targetView = hudRoot?.findViewById<TextView>(R.id.txtVitalDinheiro) ?: txtDinheiro

        val endLoc = IntArray(2)
        targetView.getLocationInWindow(endLoc)

        val rootLoc = IntArray(2)
        container.getLocationInWindow(rootLoc)

        moedasConfig.forEachIndexed { index, (resId, isVideo) ->
            container.postDelayed({
                val coinView = if (isVideo) {
                    VideoView(this@ExplorationActivity).apply {
                        setVideoURI(android.net.Uri.parse("android.resource://$packageName/$resId"))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            try { mp.setVolume(0f, 0f) } catch (e: Exception) {}
                        }
                        start()
                    }
                } else {
                    ImageView(this@ExplorationActivity).apply {
                        setImageResource(resId)
                    }
                }

                coinView.layoutParams = FrameLayout.LayoutParams(coinSize, coinSize)
                coinView.alpha = 0f
                container.addView(coinView)

                val spreadX = (index - 1.5f) * 20f * density
                val spreadY = (if (index % 2 == 0) -15f else 15f) * density

                val startX = startLoc[0] - rootLoc[0] + (txtDescricao.width / 2f) - (coinSize / 2f) + spreadX
                val startY = startLoc[1] - rootLoc[1] + (txtDescricao.height / 2f) - (coinSize / 2f) + spreadY

                val endX = endLoc[0] - rootLoc[0] + (targetView.width / 2f) - (coinSize / 2f)
                val endY = endLoc[1] - rootLoc[1] + (targetView.height / 2f) - (coinSize / 2f)

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
                        val soundToPlay = if (index % 2 == 0) soundIdA else soundIdB
                        soundPool?.play(soundToPlay, 0.5f, 0.5f, 1, 0, 1f)

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

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
        soundPool = null
    }
}
