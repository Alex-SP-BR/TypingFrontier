package com.typingfrontier

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.exploration.*
import com.typingfrontier.utils.CurrencyUtils
import com.typingfrontier.utils.ViewUtils
import android.media.AudioAttributes
import android.media.SoundPool
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import com.google.android.material.card.MaterialCardView
import com.typingfrontier.collection.AchievementManager
import com.typingfrontier.station.StationActivity

// CONFIGURAÇÃO TEMPORÁRIA: Controla o acesso à Estação de Trem de Alta Velocidade
private const val ESTACAO_ALTA_VELOCIDADE_LIBERADA = true

class ExplorationActivity : AppCompatActivity() {

    private lateinit var layoutSelecao: View
    private lateinit var layoutAventura: View
    private lateinit var layoutEscolhasNarrativas: LinearLayout
    private lateinit var layoutAcoesAventura: LinearLayout
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
    private var escolhaAnteriorId: String? = null
    private var recompensaColetada = false

    // ÁUDIO E ANIMAÇÃO RECOMPENSA
    private var soundPool: SoundPool? = null
    private var soundIdA: Int = 0
    private var soundIdB: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exploration)

        vincularViews()

        val regiaoIdExtra = intent.getStringExtra("REGIAO_ID") ?: "sao_paulo"
        if (regiaoIdExtra == "sao_paulo_norte") {
            findViewById<TextView>(R.id.txtTituloSelecao)?.text = "São Paulo Norte — Aventuras Disponíveis"
        }

        configurarRecycler(regiaoIdExtra)
        atualizarHUD()
        prepararSons()

        findViewById<Button>(R.id.btnVoltarMapa).setOnClickListener {
            val regiao = intent.getStringExtra("REGIAO_ID")
            if (regiao == "sao_paulo_norte") {
                if (!ViewUtils.isNetworkAvailable(this)) {
                    Toast.makeText(this, "É necessário estar conectado à internet para acessar a Estação.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val intentStation = Intent(this, StationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intentStation)
            }
            finish()
        }

        val zonaIdExtra = intent.getStringExtra("ZONA_ID")
        if (!zonaIdExtra.isNullOrEmpty()) {
            val zona = ExplorationZoneRepository.getZona(zonaIdExtra)
            if (zona != null) {
                iniciarExploracao(zona)
            }
        }
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
        layoutEscolhasNarrativas = findViewById(R.id.layoutEscolhasNarrativas)
        layoutAcoesAventura = findViewById(R.id.layoutAcoesAventura)
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

    private fun configurarRecycler(regiao: String = "sao_paulo") {
        val rv = findViewById<RecyclerView>(R.id.rvZonas)
        rv.layoutManager = LinearLayoutManager(this)
        val zonasDisponiveis = ExplorationZoneRepository.getZonasPorRegiao(regiao)
        rv.adapter = ExplorationZoneAdapter(zonasDisponiveis) { zona ->
            if (zona.id == "rio_construcao") {
                if (ESTACAO_ALTA_VELOCIDADE_LIBERADA) {
                    if (!ViewUtils.isNetworkAvailable(this)) {
                        Toast.makeText(this, "É necessário estar conectado à internet para acessar a Estação.", Toast.LENGTH_LONG).show()
                        return@ExplorationZoneAdapter
                    }
                    val intent = android.content.Intent(this, com.typingfrontier.station.StationActivity::class.java)
                    startActivity(intent)
                }
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
        recompensaColetada = false
        escolhaAnteriorId = null

        layoutSelecao.visibility = View.GONE
        layoutAventura.visibility = View.VISIBLE
        
        atualizarHUD()
        SoundManager.play(this, "suspense")

        if (PonteEstaiadaStory.isBranchedAdventure(zona.id)) {
            layoutAcoesAventura.visibility = View.GONE
            layoutEscolhasNarrativas.visibility = View.VISIBLE
            carregarNoNarrativo("CENA_1")
            return
        }

        layoutAcoesAventura.visibility = View.VISIBLE
        layoutEscolhasNarrativas.visibility = View.GONE

        txtZonaNome.text = zona.nome
        txtEtapa.text = "Entrada"

        val p = PlayerManager.player
        val temaProfissao = when (zona.id) {
            "parque" -> when(p.profissao) {
                "Policial" -> "Você entra no parque atento ao movimento das famílias e à segurança das trilhas."
                "Médico" -> "O sol forte preocupa você; seus olhos buscam por sinais de exaustão entre os frequentadores."
                "Professor" -> "O burburinho das pessoas chama sua atenção para uma agitação perto do parquinho."
                "Detetive" -> "Em meio ao lazer dos outros, você foca em um detalhe que parece fora do lugar."
                "Engenheiro" -> "O som irregular da fonte central atrai seu olhar técnico imediatamente."
                else -> "Você inicia sua caminhada pelo Parque da Cidade..."
            }
            "centro" -> when(p.profissao) {
                "Policial" -> "Você caminha pelos corredores movimentados, atento a batedores de carteira e tumultos."
                "Médico" -> "A aglomeração e o ar condicionado fraco parecem o cenário ideal para mal-estares súbitos."
                "Professor" -> "Você observa o comportamento dos consumidores e a dinâmica social frenética do comércio."
                "Detetive" -> "Vitrines brilhantes não distraem você; seu foco está em quem parece estar ali sem comprar nada."
                "Engenheiro" -> "A fiação exposta de um quiosque e o barulho de um elevador próximo incomodam seus sentidos."
                else -> "Você entra no Centro Comercial e encara a multidão..."
            }
            "suburbio" -> when(p.profissao) {
                "Policial" -> "Fábricas abandonadas e ruas desertas exigem atenção total a cada sombra."
                "Médico" -> "O cheiro de ferrugem e poeira industrial indica que qualquer ferimento aqui pode ser sério."
                "Professor" -> "Você reflete sobre o declínio econômico da região enquanto observa as ruínas industriais."
                "Detetive" -> "Este é o lugar perfeito para esconder o que não deve ser encontrado. Você começa a busca."
                "Engenheiro" -> "Máquinas pesadas e estruturas de metal corroído são um convite ao seu intelecto."
                else -> "Você caminha pelas ruas silenciosas do Subúrbio Industrial..."
            }
            "beco" -> when(p.profissao) {
                "Policial" -> "Sua mão repousa perto do coldre enquanto você entra na zona onde a lei raramente chega."
                "Médico" -> "Você prepara seu kit de emergência; as condições de higiene aqui são inexistentes."
                "Professor" -> "O silêncio do beco é interrompido por sussurros. Você tenta entender a linguagem das ruas."
                "Detetive" -> "A escuridão é sua aliada. Você procura por rastros que o asfalto tenta esconder."
                "Engenheiro" -> "Você nota a gambiarra nos postes e a precariedade das escadas de incêndio."
                else -> "Você mergulha nas sombras do Beco Escuro..."
            }
            "laboratorio" -> when(p.profissao) {
                "Policial" -> "Sinais de invasão e recipientes quebrados indicam que algo deu muito errado aqui."
                "Médico" -> "Vapores químicos e bio-riscos. Você ajusta sua máscara e entra com cautela médica."
                "Professor" -> "Quadros negros cheios de fórmulas e arquivos espalhados contam uma história de obsessão."
                "Detetive" -> "Segredos corporativos e experimentos proibidos. Você busca a verdade entre os frascos."
                "Engenheiro" -> "Computadores antigos e reatores instáveis. Você quer entender como tudo isso funcionava."
                else -> "Você abre a porta pesada do Laboratório Abandonado..."
            }
            "cassino" -> when(p.profissao) {
                "Policial" -> "Você entra disfarçado, mapeando as saídas e identificando os seguranças armados."
                "Médico" -> "O excesso de fumaça e adrenalina é uma bomba relógio para os apostadores veteranos."
                "Professor" -> "Você observa como o vício e a esperança manipulam o comportamento humano nas mesas."
                "Detetive" -> "Cada blefe e cada olhar nervoso é uma pista. Você procura por quem está trapaceando."
                "Engenheiro" -> "Os mecanismos das roletas e das máquinas de slot parecem ter um padrão para você."
                else -> "Você entra no brilho ofuscante do Cassino Clandestino..."
            }
            "esgotos" -> when(p.profissao) {
                "Policial" -> "A ecoar de passos na água avisa que você não está sozinho nestes túneis."
                "Médico" -> "O risco de infecção é altíssimo. Você se move tentando não tocar nas paredes úmidas."
                "Professor" -> "Você pensa no que a sociedade descarta enquanto caminha pela infraestrutura esquecida."
                "Detetive" -> "O que é jogado fora revela muito sobre a superfície. Você segue o fluxo."
                "Engenheiro" -> "A pressão da água e o estado das vigas de sustentação são sua maior preocupação."
                else -> "Você desce para a escuridão úmida dos Esgotos Profundos..."
            }
            else -> when(p.profissao) {
                "Policial" -> "Você observa o local com cautela tática, procurando por ameaças."
                "Médico" -> "Você analisa o ambiente procurando por suprimentos ou pessoas feridas."
                "Professor" -> "Você tenta ler os sinais sociais e a história do lugar."
                "Detetive" -> "Seus olhos buscam por pistas e inconsistências no cenário."
                "Engenheiro" -> "Você avalia a estrutura e as máquinas ao redor."
                else -> "Você entra silenciosamente no local..."
            }
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
            if (etapaAtual == 1 && xpAcumulado == 0) voltarParaSelecaoDeZonas() else finalizarComSucesso() 
        }
        btnFinalizar.setOnClickListener { voltarParaSelecaoDeZonas() }
    }

    private fun voltarParaSelecaoDeZonas() {
        zonaAtual = null
        etapaAtual = 0
        xpAcumulado = 0
        dinheiroAcumulado = 0
        recompensaColetada = false
        escolhaAnteriorId = null

        layoutAventura.visibility = View.GONE
        layoutSelecao.visibility = View.VISIBLE
        SoundManager.play(this, "aventura")
    }

    private fun carregarNoNarrativo(nodeId: String) {
        val node = PonteEstaiadaStory.getNode(nodeId) ?: return

        txtZonaNome.text = zonaAtual?.nome ?: "Incidente na Ponte Estaiada"
        txtEtapa.text = node.tituloEtapa
        
        val textoNarrativo = if (nodeId == "CENA_3_RECONVERGENCIA") {
            PonteEstaiadaStory.getTextoReconvergencia(escolhaAnteriorId)
        } else {
            node.textoNarrativo
        }
        txtDescricao.text = textoNarrativo
        txtResultado.text = ""

        layoutEscolhasNarrativas.removeAllViews()

        if (node.escolhas.isNotEmpty()) {
            btnFinalizar.visibility = View.GONE

            node.escolhas.forEach { escolha ->
                val cardView = MaterialCardView(this).apply {
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, (12 * resources.displayMetrics.density).toInt())
                    layoutParams = params

                    setCardBackgroundColor(Color.parseColor("#1A222D"))
                    radius = 12f * resources.displayMetrics.density
                    strokeColor = Color.parseColor("#3374C6E0")
                    strokeWidth = (1f * resources.displayMetrics.density).toInt()
                    isClickable = true
                    isFocusable = true
                }

                val cardContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    val p16 = (16 * resources.displayMetrics.density).toInt()
                    setPadding(p16, p16, p16, p16)
                }

                val txtTituloChoice = TextView(this).apply {
                    text = escolha.titulo
                    setTextColor(Color.parseColor("#74C6E0"))
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                }

                val txtSituacao = TextView(this).apply {
                    text = escolha.situacao
                    setTextColor(Color.parseColor("#D0D6DD"))
                    textSize = 14f
                    val pTop = (6 * resources.displayMetrics.density).toInt()
                    setPadding(0, pTop, 0, 0)
                }

                val txtAtributos = TextView(this).apply {
                    text = PonteEstaiadaStory.formatarAtributosEnvolvidos(escolha)
                    setTextColor(Color.parseColor("#81C784"))
                    textSize = 13f
                    setTypeface(null, Typeface.ITALIC)
                    val pTop = (8 * resources.displayMetrics.density).toInt()
                    setPadding(0, pTop, 0, 0)
                }

                val btnEscolher = Button(this).apply {
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (48 * resources.displayMetrics.density).toInt()
                    )
                    params.setMargins(0, (12 * resources.displayMetrics.density).toInt(), 0, 0)
                    layoutParams = params
                    text = "ESCOLHER ESTA AÇÃO"
                    setBackgroundResource(R.drawable.bg_game_button_primary)
                    backgroundTintList = null
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    setOnClickListener {
                        executarEscolhaNarrativa(node, escolha)
                    }
                }

                cardContent.addView(txtTituloChoice)
                cardContent.addView(txtSituacao)
                cardContent.addView(txtAtributos)
                cardContent.addView(btnEscolher)

                cardView.addView(cardContent)
                cardView.setOnClickListener {
                    executarEscolhaNarrativa(node, escolha)
                }

                layoutEscolhasNarrativas.addView(cardView)
            }

            val btnDesistir = Button(this).apply {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (48 * resources.displayMetrics.density).toInt()
                )
                params.setMargins(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)
                layoutParams = params

                if (etapaAtual == 1 || xpAcumulado == 0) {
                    text = "DESISTIR E VOLTAR"
                    setTextColor(Color.parseColor("#D0D6DD"))
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        voltarParaSelecaoDeZonas()
                    }
                } else {
                    text = "DESISTIR E SAIR COM O QUE TENHO 🏃"
                    setTextColor(Color.parseColor("#E0E0E0"))
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        finalizarComSucesso()
                    }
                }
            }
            layoutEscolhasNarrativas.addView(btnDesistir)

        } else {
            if (node.etapa == 5 && etapaAtual == 5) {
                val recompensa5 = PonteEstaiadaStory.gerarRecompensaEtapa(5)
                val xp = recompensa5["xp"] ?: 1200
                val grana = recompensa5["dinheiro"] ?: 1800
                xpAcumulado += xp
                dinheiroAcumulado += grana
                etapaAtual = 6

                val coinIcon = ViewUtils.getCoinDrawable(this)
                val size = (20 * resources.displayMetrics.density).toInt()
                coinIcon.setBounds(0, 0, size, size)

                val baseText = "Progresso Total: +$xpAcumulado XP | "
                val moneyText = CurrencyUtils.formatar(dinheiroAcumulado)
                val fullText = "$baseText $moneyText"
                val spannable = SpannableString(fullText)

                val imageSpan = ImageSpan(coinIcon, ImageSpan.ALIGN_BOTTOM)
                val start = baseText.length
                spannable.setSpan(imageSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                txtResultado.text = spannable
                txtResultado.setTextColor(Color.parseColor("#81C784"))
            }

            layoutEscolhasNarrativas.visibility = View.GONE
            btnFinalizar.visibility = View.VISIBLE
            btnFinalizar.text = "CONCLUIR INVESTIGAÇÃO E COLETAR RECOMPENSAS 🏆"
            btnFinalizar.setOnClickListener {
                finalizarComSucesso()
            }
        }
    }

    private fun executarEscolhaNarrativa(currentNode: NarrativeNode, escolha: NarrativeChoice) {
        val p = PlayerManager.player
        val zona = zonaAtual ?: return

        val result = GameEngine.dispatch(GameAction.Explore(zona.id))
        when (result) {
            is EngineResult.Failure -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                if (xpAcumulado > 0) finalizarComSucesso() else voltarParaSelecaoDeZonas()
                return
            }
            is EngineResult.Success -> {
                atualizarHUD()
            }
        }

        val (sucesso, mensagem) = PonteEstaiadaStory.resolverTeste(p, escolha, currentNode.etapa)

        if (sucesso) {
            val recompensa = PonteEstaiadaStory.gerarRecompensaEtapa(currentNode.etapa)
            val xp = recompensa["xp"] ?: 0
            val grana = recompensa["dinheiro"] ?: 0

            xpAcumulado += xp
            dinheiroAcumulado += grana

            val coinIcon = ViewUtils.getCoinDrawable(this)
            val size = (20 * resources.displayMetrics.density).toInt()
            coinIcon.setBounds(0, 0, size, size)

            val baseText = "Progresso na Ponte: +$xpAcumulado XP | "
            val moneyText = CurrencyUtils.formatar(dinheiroAcumulado)
            val fullText = "$baseText $moneyText"
            val spannable = SpannableString(fullText)

            val imageSpan = ImageSpan(coinIcon, ImageSpan.ALIGN_BOTTOM)
            val start = baseText.length
            spannable.setSpan(imageSpan, start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            txtResultado.text = spannable
            txtResultado.setTextColor(Color.parseColor("#81C784"))

            escolhaAnteriorId = escolha.id
            etapaAtual++
            carregarNoNarrativo(escolha.proximoNodeId)
        } else {
            val msgHospital = ExplorationManager.processarFalhaCritica(p, zonaAtual)
            txtDescricao.text = "❌ INVESTIGAÇÃO INTERROMPIDA!\n\n$mensagem\n\n$msgHospital"
            txtResultado.text = "Você perdeu o progresso acumulado nesta investigação."
            txtResultado.setTextColor(Color.parseColor("#FF8A80"))

            layoutEscolhasNarrativas.visibility = View.GONE
            btnFinalizar.visibility = View.VISIBLE
            btnFinalizar.text = "VOLTAR AO MAPA"
            btnFinalizar.setOnClickListener { voltarParaSelecaoDeZonas() }

            PlayerManager.save(this)
        }
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
                if (xpAcumulado > 0) finalizarComSucesso() else voltarParaSelecaoDeZonas()
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

            // Configuração do ícone da moeda com tratamento de transparência para o Spannable (20dp)
            val coinIcon = ViewUtils.getCoinDrawable(this)
            val size = (20 * resources.displayMetrics.density).toInt()
            coinIcon.setBounds(0, 0, size, size)

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
            val msgHospital = ExplorationManager.processarFalhaCritica(p, zonaAtual)
            txtDescricao.text = "❌ BUSCA ENCERRADA!\n$msgHospital"
            txtResultado.text = "Você perdeu todo o loot acumulado nesta zona."
            txtResultado.setTextColor(android.graphics.Color.parseColor("#FF8A80"))
            
            btnIrMaisFundo.visibility = View.GONE
            btnSairLoot.visibility = View.GONE
            btnFinalizar.visibility = View.VISIBLE
            btnFinalizar.text = "VOLTAR AO MAPA"
            
            PlayerManager.save(this)
        }
    }

    private fun finalizarComSucesso() {
        if (!recompensaColetada) {
            recompensaColetada = true
            val result = GameEngine.dispatch(GameAction.CollectRewards(xpAcumulado, dinheiroAcumulado))
            
            atualizarHUD() 
            
            txtDescricao.text = "🏆 VITÓRIA!\nVocê saiu da zona com vida.\n\nColetou $xpAcumulado XP e ${CurrencyUtils.formatar(dinheiroAcumulado)}."

            // Dispara a animação das 4 moedas se o depósito foi um sucesso e houver ganho de Frons
            if (result is EngineResult.Success && dinheiroAcumulado > 0) {
                dispararAnimacaoMoeda(dinheiroAcumulado)
            }
            
            // 🏆 GATILHO DE CONQUISTA (Apenas se completou as 5 etapas com sucesso)
            if (etapaAtual > 5) {
                zonaAtual?.let { AchievementManager.checkExploration(this, it.id) }
            }
            
            PlayerManager.save(this)
        }

        btnIrMaisFundo.visibility = View.GONE
        btnSairLoot.visibility = View.GONE
        if (layoutEscolhasNarrativas.visibility == View.VISIBLE) {
            layoutEscolhasNarrativas.visibility = View.GONE
        }
        btnFinalizar.visibility = View.VISIBLE
        btnFinalizar.text = "VOLTAR AO MAPA"
        btnFinalizar.setOnClickListener { voltarParaSelecaoDeZonas() }
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

        val moedasConfig = List(4) { R.drawable.fron_coin }

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

        moedasConfig.forEachIndexed { index, resId ->
            container.postDelayed({
                val coinView = ImageView(this@ExplorationActivity).apply {
                    setImageDrawable(ViewUtils.getCoinDrawable(this@ExplorationActivity))
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
        if (zonaAtual?.id == "sp_norte_investigacao") {
            SoundManager.play(this, "aventura")
        }
    }
}
