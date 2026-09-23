package com.typingfrontier.station

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.typingfrontier.*
import com.typingfrontier.economy.ProfessionManager

class StationActivity : AppCompatActivity(), StationGridView.InteractionListener {
    
    // Modelo de Conversa
    private data class ChatConversation(
        val id: String,
        val title: String,
        var history: String = "",
        val isNpc: Boolean = false
    )

    private var armoireDialog: AlertDialog? = null
    private lateinit var gridView: StationGridView
    
    // UI Chat
    private lateinit var layoutChat: View
    private lateinit var layoutChatTabs: LinearLayout
    private lateinit var layoutChatHeader: View
    private lateinit var txtChatNpcName: TextView
    private lateinit var txtChatExpandHint: TextView
    private lateinit var txtChatMessages: TextView
    private lateinit var edtChatMessage: EditText
    private lateinit var btnChatSend: ImageButton
    private lateinit var btnChatClose: ImageButton
    private lateinit var scrollChat: ScrollView

    // Gerenciamento de Conversas
    private val conversations = mutableMapOf<String, ChatConversation>()
    private var currentConversationId = "public"
    private var isChatExpanded = false
    private var isChatHidden = false

    // Controle de Digitação
    private val typingHandler = Handler(Looper.getMainLooper())
    private var typingRunnable: Runnable? = null
    private var fullTextToType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station)
        
        supportActionBar?.hide()

        val imgBg = findViewById<ImageView>(R.id.imgBackgroundStation)
        gridView = findViewById(R.id.stationGridView)

        gridView.setTarget(imgBg)
        gridView.setInteractionListener(this)

        vincularUiChat()
        
        // Inicializa conversas padrão
        conversations["public"] = ChatConversation("public", "Estação São Paulo")
        selecionarConversa("public")
        
        recolherChat()
        
        configurarJogador(gridView)
        configurarNpcTeste(gridView)
    }

    private fun vincularUiChat() {
        layoutChat = findViewById(R.id.layoutChatOverlay)
        layoutChatTabs = findViewById(R.id.layoutChatTabs)
        layoutChatHeader = findViewById(R.id.layoutChatHeader)
        txtChatNpcName = findViewById(R.id.txtChatNpcName)
        txtChatExpandHint = findViewById(R.id.txtChatExpandHint)
        txtChatMessages = findViewById(R.id.txtChatMessages)
        edtChatMessage = findViewById(R.id.edtChatMessage)
        btnChatSend = findViewById(R.id.btnChatSend)
        btnChatClose = findViewById(R.id.btnChatClose)
        scrollChat = findViewById(R.id.scrollChat)

        btnChatClose.setOnClickListener {
            esconderChat()
        }

        layoutChatHeader.setOnClickListener {
            alternarExpansaoChat()
        }

        btnChatSend.setOnClickListener {
            processarEnvio()
        }

        edtChatMessage.setOnClickListener {
            if (isChatHidden) mostrarChat()
        }

        ViewCompat.setOnApplyWindowInsetsListener(layoutChat) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val density = resources.displayMetrics.density
            val reduction = (12 * density).toInt() // Revertido para o ajuste anterior (12dp) que não sobrepõe o campo
            val offset = (imeInsets.bottom - reduction).coerceAtLeast(0)
            v.translationY = -offset.toFloat()
            insets
        }

        edtChatMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                processarEnvio()
                true
            } else {
                false
            }
        }
    }

    private fun selecionarConversa(id: String) {
        val conv = conversations[id] ?: return
        
        // Se mudar de uma conversa NPC, retoma a circulação dele (simplificado para Antônio)
        if (currentConversationId == "antonio" && id != "antonio") {
            gridView.setNpcInteracting("Antônio", false)
        }
        
        // Se entrar em uma conversa NPC e o chat não estiver escondido, pausa ele
        if (id == "antonio" && !isChatHidden) {
            gridView.setNpcInteracting("Antônio", true)
        }

        currentConversationId = id
        concluirDigitacaoImediata()
        
        txtChatNpcName.text = conv.title
        txtChatMessages.text = conv.history
        
        atualizarInterfaceAbas()
        
        if (isChatHidden) mostrarChat()
        
        scrollChat.post {
            scrollChat.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun fecharConversa(id: String) {
        if (id == "public") return // Não fecha a pública
        
        if (id == "antonio") {
            gridView.setNpcInteracting("Antônio", false)
        }
        
        conversations.remove(id)
        
        if (currentConversationId == id) {
            selecionarConversa("public")
        } else {
            atualizarInterfaceAbas()
        }
    }

    private fun atualizarInterfaceAbas() {
        layoutChatTabs.removeAllViews()
        val density = resources.displayMetrics.density
        
        conversations.values.forEach { conv ->
            val tabContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                val isSelected = conv.id == currentConversationId
                setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
                setBackgroundColor(if (isSelected) android.graphics.Color.parseColor("#3374C6E0") else android.graphics.Color.TRANSPARENT)
                setOnClickListener { selecionarConversa(conv.id) }
            }

            val txtTitle = TextView(this).apply {
                text = if (conv.id == "public") "🌍 ${conv.title}" else "👤 ${conv.title}"
                textSize = 12f
                val isSelected = conv.id == currentConversationId
                setTextColor(if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#889099"))
            }
            tabContainer.addView(txtTitle)

            if (conv.id != "public") {
                val txtClose = TextView(this).apply {
                    text = " ×"
                    textSize = 14f
                    val isSelected = conv.id == currentConversationId
                    setTextColor(if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#889099"))
                    setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
                    
                    // Ripple effect para o botão fechar
                    val typedValue = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true)
                    setBackgroundResource(typedValue.resourceId)
                    
                    setOnClickListener {
                        fecharConversa(conv.id)
                    }
                }
                tabContainer.addView(txtClose)
            }
            
            layoutChatTabs.addView(tabContainer)
            
            // Espaçador
            val spacer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams((4 * density).toInt(), 1)
            }
            layoutChatTabs.addView(spacer)
        }
    }

    private fun alternarExpansaoChat() {
        if (isChatHidden) {
            mostrarChat()
            return
        }
        isChatExpanded = !isChatExpanded
        atualizarAlturaChat()
    }

    private fun atualizarAlturaChat() {
        val params = scrollChat.layoutParams
        val density = resources.displayMetrics.density
        if (isChatExpanded) {
            params.height = (165 * density).toInt()
            txtChatExpandHint.text = "[-] recolher"
        } else {
            params.height = (55 * density).toInt()
            txtChatExpandHint.text = "[+] expandir"
        }
        scrollChat.layoutParams = params
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun esconderChat() {
        concluirDigitacaoImediata()
        isChatHidden = true
        layoutChatHeader.visibility = View.GONE
        scrollChat.visibility = View.GONE
        layoutChatTabs.visibility = View.GONE
        
        // Retoma circulação do Antônio se estiver em conversa com ele
        if (currentConversationId == "antonio") {
            gridView.setNpcInteracting("Antônio", false)
        }
        
        edtChatMessage.setText("")
    }

    private fun mostrarChat() {
        isChatHidden = false
        layoutChatHeader.visibility = View.VISIBLE
        scrollChat.visibility = View.VISIBLE
        layoutChatTabs.visibility = View.VISIBLE
        
        // Ao reabrir pelo campo compacto, seleciona a aba pública por padrão
        selecionarConversa("public")
        
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun recolherChat() {
        isChatExpanded = false
        atualizarAlturaChat()
    }

    private fun processarEnvio() {
        val msg = edtChatMessage.text.toString().trim()
        if (msg.isEmpty()) return

        val p = PlayerManager.player
        adicionarMensagem("@${p.nome}: $msg", isNpc = false)
        edtChatMessage.setText("")

        // Simulação específica para aba do Antônio
        if (currentConversationId == "antonio") {
            if (msg.equals("Oi", ignoreCase = true)) {
                adicionarMensagem("Antônio: Olá! Você deseja comprar uma passagem para o Rio de Janeiro?", isNpc = true)
            } else if (msg.equals("Sim", ignoreCase = true)) {
                adicionarMensagem("Antônio: A passagem custa 1 Fron. (Lógica de compra será integrada futuramente)", isNpc = true)
            } else {
                adicionarMensagem("Antônio: Desculpe, não entendi. (Digite 'Oi' para começar ou 'Sim' para simular compra)", isNpc = true)
            }
        }
    }

    private fun adicionarMensagem(texto: String, isNpc: Boolean) {
        val conv = conversations[currentConversationId] ?: return
        
        concluirDigitacaoImediata()

        val prefixo = if (conv.history.isEmpty()) "" else "\n"
        
        if (!isNpc) {
            conv.history += "$prefixo$texto"
            if (!isChatHidden) {
                txtChatMessages.text = conv.history
                scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
            }
        } else {
            iniciarEfeitoDigitacao(conv, prefixo, texto)
        }
    }

    private fun concluirDigitacaoImediata() {
        typingRunnable?.let {
            typingHandler.removeCallbacks(it)
            val conv = conversations[currentConversationId]
            if (conv != null) {
                conv.history = fullTextToType
                if (!isChatHidden) txtChatMessages.text = conv.history
            }
            typingRunnable = null
            scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun iniciarEfeitoDigitacao(conv: ChatConversation, prefixo: String, novaMensagem: String) {
        val baseHistory = conv.history + prefixo
        fullTextToType = baseHistory + novaMensagem
        var charIndex = 0
        
        typingRunnable = object : Runnable {
            override fun run() {
                if (charIndex <= novaMensagem.length) {
                    val parcial = baseHistory + novaMensagem.substring(0, charIndex)
                    conv.history = parcial
                    if (!isChatHidden && currentConversationId == conv.id) {
                        txtChatMessages.text = parcial
                        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
                    }
                    charIndex++
                    typingHandler.postDelayed(this, 30)
                } else {
                    typingRunnable = null
                }
            }
        }
        typingHandler.post(typingRunnable!!)
    }

    private fun configurarNpcTeste(gridView: StationGridView) {
        val npcSprites = mapOf(
            "cima" to R.drawable.npc_estacao_homem_01_cima,
            "baixo" to R.drawable.npc_estacao_homem_01_baixo,
            "esquerda" to R.drawable.npc_estacao_homem_01_esquerda,
            "direita" to R.drawable.npc_estacao_homem_01_direita
        )
        
        gridView.setNpcData(
            name = "Antônio",
            x = 5,
            y = 12,
            direction = "baixo",
            sprites = npcSprites,
            isCirculating = true
        )
    }

    override fun onArmoireTapped() {
        abrirInterfaceArmario()
    }

    override fun onNpcTapped(npcName: String) {
        if (npcName == "Antônio") {
            abrirChatAntonio()
        }
    }

    private fun abrirChatAntonio() {
        if (!conversations.containsKey("antonio")) {
            conversations["antonio"] = ChatConversation("antonio", "Antônio", isNpc = true)
        }
        
        selecionarConversa("antonio")
        gridView.setNpcInteracting("Antônio", true)
        
        if (conversations["antonio"]?.history?.isEmpty() == true) {
            adicionarMensagem("Antônio: Olá! Bem-vindo à estação.", isNpc = true)
        }
    }

    private fun abrirInterfaceArmario() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_armario, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        
        armoireDialog = builder.create()
        armoireDialog?.show()

        dialogView.findViewById<Button>(R.id.btnFecharArmario).setOnClickListener {
            armoireDialog?.dismiss()
        }

        atualizarListasArmario(dialogView)
    }

    private fun atualizarListasArmario(view: View) {
        val player = PlayerManager.player
        val layoutMochila = view.findViewById<LinearLayout>(R.id.layoutMochilaTransfer)
        val layoutArmario = view.findViewById<LinearLayout>(R.id.layoutArmarioTransfer)
        val txtCapacidade = view.findViewById<TextView>(R.id.txtCapacidadeArmario)

        layoutMochila.removeAllViews()
        layoutArmario.removeAllViews()

        val totalArmario = player.armario.values.sum()
        txtCapacidade.text = "Capacidade: $totalArmario/${player.capacidadeArmario}"

        player.mochila.forEach { (id, qtd) ->
            if (qtd > 0) {
                val itemView = criarItemTransfer(id, qtd, true, view)
                layoutMochila.addView(itemView)
            }
        }

        player.armario.forEach { (id, qtd) ->
            if (qtd > 0) {
                val itemView = criarItemTransfer(id, qtd, false, view)
                layoutArmario.addView(itemView)
            }
        }
    }

    private fun criarItemTransfer(id: String, qtd: Int, isMochila: Boolean, dialogView: View): View {
        val itemView = LayoutInflater.from(this).inflate(R.layout.item_transfer, null)
        val equip = ProfessionManager.getEquipment(id)
        
        itemView.findViewById<TextView>(R.id.txtNomeTransfer).text = equip?.nome ?: id
        itemView.findViewById<TextView>(R.id.txtQtdTransfer).text = "Qtd: $qtd"
        
        val img = itemView.findViewById<ImageView>(R.id.imgItemTransfer)
        if (equip?.imagemRes != null) {
            img.setImageResource(equip.imagemRes)
        }

        val btn = itemView.findViewById<Button>(R.id.btnActionTransfer)
        btn.text = if (isMochila) "GUARDAR" else "RETIRAR"
        
        btn.setOnClickListener {
            val action = if (isMochila) GameAction.DepositItem(id) else GameAction.WithdrawItem(id)
            val result = GameEngine.dispatch(action)
            
            if (result is EngineResult.Success) {
                atualizarListasArmario(dialogView)
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            } else if (result is EngineResult.Failure) {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        return itemView
    }

    private fun configurarJogador(gridView: StationGridView) {
        val player = com.typingfrontier.PlayerManager.player
        val sexoChar = if (player.sexo == "Feminino") "f" else "m"
        
        val baseNome = when (player.profissao) {
            "Médico" -> if (sexoChar == "f") "medica" else "medico"
            "Engenheiro" -> if (sexoChar == "f") "engenheira" else "engenheiro"
            "Professor" -> if (sexoChar == "f") "professora" else "professor"
            "Detetive" -> "detetive"
            "Policial" -> "policial"
            else -> "homem"
        }
        
        val sprites = mutableMapOf<String, Int>()
        val directions = listOf("frente", "costas", "esquerda", "direita")
        
        for (dir in directions) {
            val resName = "${baseNome}_${sexoChar}_$dir"
            val resId = resources.getIdentifier(resName, "drawable", packageName)
            if (resId != 0) sprites[dir] = resId
        }
        
        val nomeExibicao = if (player.nome.isNotEmpty()) player.nome else "Viajante"
        gridView.setPlayerData(sprites, nomeExibicao)
    }
}
