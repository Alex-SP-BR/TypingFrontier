package com.typingfrontier.station

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.typingfrontier.*
import com.typingfrontier.economy.ProfessionManager
import com.typingfrontier.npc.SharedNpcManager
import com.typingfrontier.social.PresenceManager
import com.typingfrontier.social.SocialProfileRepository
import kotlinx.coroutines.launch

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
    private lateinit var layoutChatInput: View

    // Gerenciamento de Conversas e Estação
    private var currentStationId: String = "sao_paulo"
    private val conversations = mutableMapOf<String, ChatConversation>()
    private var currentConversationId = "public"
    private var isChatExpanded = false
    private var isChatHidden = false

    // Controle de Digitação
    private val typingHandler = Handler(Looper.getMainLooper())
    private var typingRunnable: Runnable? = null
    private var fullTextToType = ""
    private var lastInsets: WindowInsetsCompat? = null

    private fun atualizarTranslacaoChat() {
        val overlay = if (::layoutChat.isInitialized) layoutChat else return
        val input = if (::layoutChatInput.isInitialized) layoutChatInput else return
        val density = resources.displayMetrics.density
        val reduction = (12 * density).toInt()
        val basePadding = (12 * density).toInt()

        val insets = lastInsets
        val imeBottom = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        val navBottom = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0

        if (imeBottom > 0) {
            val offset = (imeBottom - reduction).coerceAtLeast(0)
            overlay.translationY = -offset.toFloat()
            input.translationY = -offset.toFloat()
        } else {
            overlay.translationY = 0f
            input.translationY = 0f
            input.setPadding(input.paddingLeft, input.paddingTop, input.paddingRight, basePadding + navBottom)
        }

        ajustarMargemOverlay()
    }

    private fun ajustarMargemOverlay() {
        if (!::layoutChatInput.isInitialized || !::layoutChat.isInitialized) return
        layoutChatInput.post {
            val height = layoutChatInput.height
            if (height > 0) {
                val params = layoutChat.layoutParams as? FrameLayout.LayoutParams
                if (params != null && params.bottomMargin != height) {
                    params.bottomMargin = height
                    layoutChat.layoutParams = params
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station)
        
        supportActionBar?.hide()

        // Restaura a última estação ativa salva pelo jogador (padrão "sao_paulo")
        val prefs = getSharedPreferences("typing_frontier_station", MODE_PRIVATE)
        currentStationId = prefs.getString("last_station_id", "sao_paulo") ?: "sao_paulo"

        val imgBg = findViewById<ImageView>(R.id.imgBackgroundStation)
        if (currentStationId == "rio_de_janeiro") {
            imgBg.setImageResource(R.drawable.bg_estacao_rio_01)
        } else {
            imgBg.setImageResource(R.drawable.bg_estacao_sp_01)
        }

        gridView = findViewById(R.id.stationGridView)
        gridView.setTarget(imgBg)
        gridView.setInteractionListener(this)
        gridView.setStationId(currentStationId)

        vincularUiChat()
        
        // Inicializa conversas padrão
        val tituloEstacao = if (currentStationId == "rio_de_janeiro") "Estação Rio de Janeiro" else "Estação São Paulo"
        conversations["public"] = ChatConversation("public", tituloEstacao)
        selecionarConversa("public")
        
        recolherChat()
        
        configurarJogador(gridView)
        configurarNpcsParaEstacao(currentStationId)

        // Inicia Presença Multiplayer e Sistema de NPCs após a primeira renderização da tela
        window.decorView.post {
            iniciarPresencaEstacao()
            SharedNpcManager.startNpcSystem(currentStationId)
            observarJogadoresOnline()
            observarNpcsCompartilhados()
        }
    }

    private fun iniciarPresencaEstacao() {
        val player = PlayerManager.player
        
        // Direção inicial mapeada para o sistema de presença
        val dirPresenca = when (playerX_direcao_local) {
            "costas" -> "cima"
            "esquerda" -> "esquerda"
            "direita" -> "direita"
            else -> "baixo"
        }

        // Inicia a infraestrutura de presença
        PresenceManager.startPresence()

        // x=5, y=20 é a posição inicial em StationGridView.kt
        PresenceManager.updatePresenceData(
            stationId = currentStationId,
            gridX = 5,
            gridY = 20,
            direction = dirPresenca,
            gender = player.sexo
        )
    }

    private var playerX_direcao_local = "frente" // Auxiliar para mapeamento de direção

    private fun observarJogadoresOnline() {
        lifecycleScope.launch {
            PresenceManager.onlineUsers.collect { list ->
                val currentUserId = SocialProfileRepository.getCurrentUserId() ?: ""
                val onlineInStation = list.filter { it.stationId == currentStationId }.map { it.user_id }.toSet()
                
                // Limpa interações de usuários que saíram ou desconectaram
                SharedNpcManager.cleanDisconnectedUsers(currentStationId, onlineInStation)

                Log.d("StationActivity", "[PRESENCE_DEBUG] RAW online users received: ${list.size}")
                list.forEach { 
                    Log.d("StationActivity", "[PRESENCE_DEBUG] User: ${it.username} (id=${it.user_id}, station=${it.stationId})")
                }

                // Filtra jogadores que estão na mesma estação (currentStationId)
                val filtered = list.filter { 
                    it.stationId == currentStationId && it.user_id != currentUserId 
                }
                
                Log.d("StationActivity", "[PRESENCE_DEBUG] Filtered remote players (station=$currentStationId, not self): ${filtered.size}")
                gridView.setRemotePlayers(filtered, currentUserId)

                // Exibe balões de fala para mensagens públicas de jogadores remotos no momento do recebimento
                val currentTime = System.currentTimeMillis()
                filtered.forEach { payload ->
                    if (payload.lastMessage.isNotEmpty() && payload.lastMessageTime > 0) {
                        if (currentTime - payload.lastMessageTime <= 5000L) {
                            gridView.addSpeechDialog(
                                senderId = payload.user_id,
                                senderName = payload.username,
                                message = payload.lastMessage,
                                worldX = payload.gridX,
                                worldY = payload.gridY,
                                isPrivate = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observarNpcsCompartilhados() {
        lifecycleScope.launch {
            SharedNpcManager.npcStateFlow.collect { states ->
                Log.d("StationActivity", "[SHARED_NPC_TRACE] StationActivity recebeu StateFlow. Total NPCs: ${states.size}")
                states.values.forEach { sharedState ->
                    Log.d("StationActivity", "[SHARED_NPC_TRACE] StationActivity repassando para GridView: npc=${sharedState.npcId}, pos=(${sharedState.gridX},${sharedState.gridY}), target=(${sharedState.targetX},${sharedState.targetY}), state=${sharedState.state}")
                    gridView.updateNpcFromSharedState(sharedState)
                }
            }
        }
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
        layoutChatInput = findViewById(R.id.layoutChatInput)

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
            if (isChatHidden) {
                selecionarConversa("public")
                mostrarChat()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(layoutChatInput) { _, insets ->
            lastInsets = insets
            atualizarTranslacaoChat()
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
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        
        // Se mudar de uma conversa NPC, encerra a interação deste jogador especificamente
        if (currentConversationId == "antonio" && id != "antonio") {
            SharedNpcManager.removeInteraction("npc_vendedor_antonio", currentUserId)
        }
        
        // Se entrar em uma conversa NPC e o chat não estiver escondido, registra/renova interação
        if (id == "antonio" && !isChatHidden) {
            SharedNpcManager.registerOrRenewInteraction("npc_vendedor_antonio", currentUserId)
        }

        currentConversationId = id
        concluirDigitacaoImediata()
        
        txtChatNpcName.text = conv.title
        val isPrivate = id != "public"
        txtChatMessages.setTextColor(
            if (isPrivate) Color.parseColor("#74C6E0")
            else Color.parseColor("#E8EDF2")
        )
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
            val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
            SharedNpcManager.removeInteraction("npc_vendedor_antonio", currentUserId)
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
            params.height = (30 * density).toInt()
            txtChatExpandHint.text = "[+] expandir"
        }
        scrollChat.layoutParams = params
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun esconderChat() {
        concluirDigitacaoImediata()
        isChatHidden = true
        layoutChat.visibility = View.GONE
        layoutChatInput.visibility = View.VISIBLE
        
        if (currentConversationId == "antonio") {
            val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
            SharedNpcManager.removeInteraction("npc_vendedor_antonio", currentUserId)
        }
        
        edtChatMessage.setText("")
        atualizarTranslacaoChat()
    }

    private fun mostrarChat() {
        isChatHidden = false
        layoutChat.visibility = View.VISIBLE
        layoutChatInput.visibility = View.VISIBLE
        
        ajustarMargemOverlay()
        atualizarTranslacaoChat()
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
        val isPublic = currentConversationId == "public"
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"

        adicionarMensagem("@${p.nome}: $msg", isNpc = false)
        edtChatMessage.setText("")

        // CAPTURA DE POSIÇÃO NO EXATO INSTANTE DO ENVIO (REGRA CRÍTICA)
        val sendX = gridView.getPlayerX()
        val sendY = gridView.getPlayerY()

        // Criar balão de fala no cenário preso à posição (sendX, sendY)
        gridView.addSpeechDialog(
            senderId = "player_local",
            senderName = p.nome.ifEmpty { "Viajante" },
            message = msg,
            worldX = sendX,
            worldY = sendY,
            isPrivate = !isPublic,
            allowedUsers = if (isPublic) emptySet() else setOf(currentUserId, "antonio")
        )

        // Se for mensagem pública, transmite aos outros jogadores via PresenceManager
        if (isPublic) {
            val netDir = when (playerX_direcao_local) {
                "costas" -> "cima"
                "esquerda" -> "esquerda"
                "direita" -> "direita"
                else -> "baixo"
            }
            PresenceManager.updatePresenceData(
                stationId = currentStationId,
                gridX = sendX,
                gridY = sendY,
                direction = netDir,
                gender = p.sexo,
                lastMessage = msg,
                lastMessageTime = System.currentTimeMillis()
            )
        }

        // Simulação / Diálogo de Compra do Antônio e Carlos
        if (currentConversationId == "antonio") {
            processarDialogoAntonio(msg)
        } else if (currentConversationId == "carlos") {
            processarDialogoCarlos(msg)
        }
    }

    private var estadoDialogoAntonio = "INICIO" // "INICIO", "PERGUNTOU_COMPRA", "AGUARDANDO_CONFIRMACAO"
    private var estadoDialogoCarlos = "INICIO"  // "INICIO", "PERGUNTOU_COMPRA", "AGUARDANDO_CONFIRMACAO"

    private fun processarDialogoAntonio(msg: String) {
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.registerOrRenewInteraction("npc_vendedor_antonio", currentUserId)

        val lower = msg.lowercase().trim()
        val player = PlayerManager.player

        val responseText: String

        when (estadoDialogoAntonio) {
            "INICIO" -> {
                if (lower == "oi" || lower == "olá" || lower == "ola" || lower.contains("comprar") || lower.contains("passagem")) {
                    estadoDialogoAntonio = "PERGUNTOU_COMPRA"
                    responseText = "Antônio: Olá! Você deseja comprar uma passagem para o Rio de Janeiro?"
                } else {
                    responseText = "Antônio: Olá! Bem-vindo à estação. Digite 'Oi' para saber mais sobre passagens."
                }
            }
            "PERGUNTOU_COMPRA" -> {
                if (lower.contains("não") || lower.contains("nao") || lower.contains("passando") || lower.contains("cancelar")) {
                    estadoDialogoAntonio = "INICIO"
                    responseText = "Antônio: Tudo bem. Se precisar de uma passagem, é só falar comigo."
                } else if (lower.contains("sim") || lower.contains("comprar") || lower.contains("quero")) {
                    if (player.dinheiro < 1) {
                        estadoDialogoAntonio = "INICIO"
                        responseText = "Antônio: A passagem para o Rio de Janeiro custa 1 Fron. Você não possui Frons suficientes para comprar esta passagem."
                    } else {
                        estadoDialogoAntonio = "AGUARDANDO_CONFIRMACAO"
                        responseText = "Antônio: A passagem para o Rio de Janeiro custa 1 Fron. Você tem ${player.dinheiro} Frons. Deseja comprar a passagem?"
                    }
                } else {
                    responseText = "Antônio: Você deseja comprar uma passagem para o Rio de Janeiro? Responda 'Sim' para continuar ou 'Não' para cancelar."
                }
            }
            "AGUARDANDO_CONFIRMACAO" -> {
                if (lower.contains("sim") || lower.contains("comprar") || lower.contains("quero") || lower.contains("passagem")) {
                    estadoDialogoAntonio = "INICIO"
                    if (player.dinheiro < 1) {
                        responseText = "Antônio: Você não possui Frons suficientes para comprar esta passagem."
                    } else {
                        player.dinheiro -= 1
                        val qtdAtual = player.mochila["ticket_rio"] ?: 0
                        player.mochila["ticket_rio"] = qtdAtual + 1
                        PlayerManager.save(this)
                        responseText = "Antônio: Passagem para o Rio de Janeiro comprada com sucesso!"
                    }
                } else {
                    estadoDialogoAntonio = "INICIO"
                    responseText = "Antônio: Tudo bem. Se precisar de uma passagem, é só falar comigo."
                }
            }
            else -> {
                estadoDialogoAntonio = "INICIO"
                responseText = "Antônio: Olá! Bem-vindo à estação."
            }
        }

        adicionarMensagem(responseText, isNpc = true)

        val npcPos = gridView.getNpcPosition("NPC Antônio")
        if (npcPos != null) {
            val cleanMsg = responseText.removePrefix("Antônio: ")
            gridView.addSpeechDialog(
                senderId = "npc_antonio",
                senderName = "NPC Antônio",
                message = cleanMsg,
                worldX = npcPos.x,
                worldY = npcPos.y,
                isPrivate = true,
                allowedUsers = setOf(currentUserId, "antonio")
            )
        }
    }

    private fun processarDialogoCarlos(msg: String) {
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.registerOrRenewInteraction("npc_vendedor_carlos", currentUserId)

        val lower = msg.lowercase().trim()
        val player = PlayerManager.player

        val responseText: String

        when (estadoDialogoCarlos) {
            "INICIO" -> {
                if (lower == "oi" || lower == "olá" || lower == "ola" || lower.contains("comprar") || lower.contains("passagem")) {
                    estadoDialogoCarlos = "PERGUNTOU_COMPRA"
                    responseText = "Carlos: Olá! Você deseja comprar uma passagem para São Paulo?"
                } else {
                    responseText = "Carlos: Olá! Bem-vindo à estação. Digite 'Oi' para saber mais sobre passagens."
                }
            }
            "PERGUNTOU_COMPRA" -> {
                if (lower.contains("não") || lower.contains("nao") || lower.contains("passando") || lower.contains("cancelar")) {
                    estadoDialogoCarlos = "INICIO"
                    responseText = "Carlos: Tudo bem. Se precisar de uma passagem, é só falar comigo."
                } else if (lower.contains("sim") || lower.contains("comprar") || lower.contains("quero")) {
                    if (player.dinheiro < 1) {
                        estadoDialogoCarlos = "INICIO"
                        responseText = "Carlos: A passagem para São Paulo custa 1 Fron. Você não possui Frons suficientes para comprar esta passagem."
                    } else {
                        estadoDialogoCarlos = "AGUARDANDO_CONFIRMACAO"
                        responseText = "Carlos: A passagem para São Paulo custa 1 Fron. Você tem ${player.dinheiro} Frons. Deseja comprar a passagem?"
                    }
                } else {
                    responseText = "Carlos: Você deseja comprar uma passagem para São Paulo? Responda 'Sim' para continuar ou 'Não' para cancelar."
                }
            }
            "AGUARDANDO_CONFIRMACAO" -> {
                if (lower.contains("sim") || lower.contains("comprar") || lower.contains("quero") || lower.contains("passagem")) {
                    estadoDialogoCarlos = "INICIO"
                    if (player.dinheiro < 1) {
                        responseText = "Carlos: Você não possui Frons suficientes para comprar esta passagem."
                    } else {
                        player.dinheiro -= 1
                        val qtdAtual = player.mochila["ticket_sao_paulo"] ?: 0
                        player.mochila["ticket_sao_paulo"] = qtdAtual + 1
                        PlayerManager.save(this)
                        responseText = "Carlos: Passagem para São Paulo comprada com sucesso!"
                    }
                } else {
                    estadoDialogoCarlos = "INICIO"
                    responseText = "Carlos: Tudo bem. Se precisar de uma passagem, é só falar comigo."
                }
            }
            else -> {
                estadoDialogoCarlos = "INICIO"
                responseText = "Carlos: Olá! Bem-vindo à estação."
            }
        }

        adicionarMensagem(responseText, isNpc = true)

        val npcPos = gridView.getNpcPosition("NPC Carlos")
        if (npcPos != null) {
            val cleanMsg = responseText.removePrefix("Carlos: ")
            gridView.addSpeechDialog(
                senderId = "npc_carlos",
                senderName = "NPC Carlos",
                message = cleanMsg,
                worldX = npcPos.x,
                worldY = npcPos.y,
                isPrivate = true,
                allowedUsers = setOf(currentUserId, "carlos")
            )
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

    private fun configurarNpcsParaEstacao(stationId: String) {
        gridView.clearNpcs()
        if (stationId == "sao_paulo") {
            val npcSprites = mapOf(
                "cima" to R.drawable.npc_estacao_homem_01_cima,
                "baixo" to R.drawable.npc_estacao_homem_01_baixo,
                "esquerda" to R.drawable.npc_estacao_homem_01_esquerda,
                "direita" to R.drawable.npc_estacao_homem_01_direita
            )
            gridView.setNpcData(
                name = "NPC Antônio",
                x = 5,
                y = 12,
                direction = "baixo",
                sprites = npcSprites,
                isCirculating = true
            )
        } else if (stationId == "rio_de_janeiro") {
            val carlosSprites = mapOf(
                "cima" to R.drawable.npc_carlos_costas,
                "baixo" to R.drawable.npc_carlos_frente,
                "esquerda" to R.drawable.npc_carlos_esquerda,
                "direita" to R.drawable.npc_carlos_direita
            )
            gridView.setNpcData(
                name = "NPC Carlos",
                x = 5,
                y = 12,
                direction = "baixo",
                sprites = carlosSprites,
                isCirculating = true
            )
        }
    }

    override fun onArmoireTapped() {
        abrirInterfaceArmario()
    }

    override fun onNpcTapped(npcName: String) {
        if (npcName == "NPC Antônio" || npcName == "Antônio") {
            abrirChatAntonio()
        } else if (npcName == "NPC Carlos" || npcName == "Carlos") {
            abrirChatCarlos()
        }
    }

    override fun onTrainTapped() {
        if (currentStationId == "sao_paulo") {
            processarEmbarqueTrainSP()
        } else {
            processarEmbarqueTrainRio()
        }
    }

    private fun processarEmbarqueTrainSP() {
        val player = PlayerManager.player
        val qtdTicket = player.mochila["ticket_rio"] ?: 0

        if (qtdTicket <= 0) {
            AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
                .setTitle("🚆 Trem para o Rio de Janeiro")
                .setMessage("Para embarcar no trem para o Rio de Janeiro, você precisa ter uma passagem na sua mochila.\n\nFale com o NPC Antônio para comprar sua passagem por 1 Fron.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
            .setTitle("🚆 Embarque para o Rio de Janeiro")
            .setMessage("Você possui uma passagem para o Rio de Janeiro na sua mochila.\nDeseja embarcar no trem agora?")
            .setPositiveButton("Embarcar") { _, _ ->
                executarViagemSPparaRio()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executarViagemSPparaRio() {
        val player = PlayerManager.player
        val qtdTicket = player.mochila["ticket_rio"] ?: 0
        if (qtdTicket <= 0) return

        // Consome 1 passagem da mochila
        if (qtdTicket > 1) {
            player.mochila["ticket_rio"] = qtdTicket - 1
        } else {
            player.mochila.remove("ticket_rio")
        }
        PlayerManager.save(this)

        // Exibe tela de transição São Paulo -> Rio
        exibirTransicaoEIrParaEstacao(
            transicaoResId = R.drawable.bg_transicao_sp_rio,
            novaEstacaoId = "rio_de_janeiro",
            bgNovaEstacaoResId = R.drawable.bg_estacao_rio_01,
            tituloNovaEstacao = "Estação Rio de Janeiro"
        )
    }

    private fun processarEmbarqueTrainRio() {
        val player = PlayerManager.player
        val qtdTicket = player.mochila["ticket_sao_paulo"] ?: 0

        if (qtdTicket <= 0) {
            AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
                .setTitle("🚆 Trem para São Paulo")
                .setMessage("Para embarcar no trem para São Paulo, você precisa ter uma passagem na sua mochila.\n\nFale com o NPC Carlos para comprar sua passagem por 1 Fron.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
            .setTitle("🚆 Embarque para São Paulo")
            .setMessage("Você possui uma passagem para São Paulo na sua mochila.\nDeseja embarcar no trem agora?")
            .setPositiveButton("Embarcar") { _, _ ->
                executarViagemRioparaSP()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executarViagemRioparaSP() {
        val player = PlayerManager.player
        val qtdTicket = player.mochila["ticket_sao_paulo"] ?: 0
        if (qtdTicket <= 0) return

        // Consome 1 passagem da mochila
        if (qtdTicket > 1) {
            player.mochila["ticket_sao_paulo"] = qtdTicket - 1
        } else {
            player.mochila.remove("ticket_sao_paulo")
        }
        PlayerManager.save(this)

        // Exibe tela de transição Rio -> São Paulo
        exibirTransicaoEIrParaEstacao(
            transicaoResId = R.drawable.bg_transicao_rio_sp,
            novaEstacaoId = "sao_paulo",
            bgNovaEstacaoResId = R.drawable.bg_estacao_sp_01,
            tituloNovaEstacao = "Estação São Paulo"
        )
    }

    private fun exibirTransicaoEIrParaEstacao(
        transicaoResId: Int,
        novaEstacaoId: String,
        bgNovaEstacaoResId: Int,
        tituloNovaEstacao: String
    ) {
        val rootLayout = findViewById<FrameLayout>(android.R.id.content)
        val overlayView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(transicaoResId)
            elevation = 100f
        }
        rootLayout.addView(overlayView)

        Handler(Looper.getMainLooper()).postDelayed({
            // Atualiza background da estação
            val imgBg = findViewById<ImageView>(R.id.imgBackgroundStation)
            imgBg.setImageResource(bgNovaEstacaoResId)

            currentStationId = novaEstacaoId
            gridView.setStationId(currentStationId)

            // Persiste a nova estação em disco para restauração ao fechar/reabrir a StationActivity
            getSharedPreferences("typing_frontier_station", MODE_PRIVATE)
                .edit()
                .putString("last_station_id", novaEstacaoId)
                .apply()

            // Configura os NPCs para a nova estação e ativa o NpcSystem
            configurarNpcsParaEstacao(novaEstacaoId)
            SharedNpcManager.startNpcSystem(novaEstacaoId)

            // Atualiza conversa pública
            conversations["public"] = ChatConversation("public", tituloNovaEstacao)
            selecionarConversa("public")

            // Atualiza presença
            val dirPresenca = when (playerX_direcao_local) {
                "costas" -> "cima"
                "esquerda" -> "esquerda"
                "direita" -> "direita"
                else -> "baixo"
            }
            PresenceManager.updatePresenceData(
                stationId = novaEstacaoId,
                gridX = gridView.getPlayerX(),
                gridY = gridView.getPlayerY(),
                direction = dirPresenca,
                gender = PlayerManager.player.sexo
            )

            // Remove o overlay de transição com um suave fade out
            overlayView.animate()
                .alpha(0f)
                .setDuration(500L)
                .withEndAction {
                    rootLayout.removeView(overlayView)
                    Toast.makeText(this, "🚆 Bem-vindo à $tituloNovaEstacao!", Toast.LENGTH_SHORT).show()
                }
                .start()

        }, 2500L)
    }

    private var isExitPromptVisible = false
    private var lastCheckedExitX = -1
    private var lastCheckedExitY = -1

    override fun onPlayerPositionChanged(x: Int, y: Int, direction: String) {
        Log.d("StationActivity", "[STAIR_DEBUG] playerPosition x=$x, y=$y, direction=$direction")
        Log.d("StationActivity", "[STAIR_CELL_TRACE] EXIT_CHECK position=($x,$y)")
        // Converte a direção interna do grid para o padrão da rede
        val netDir = when (direction) {
            "costas" -> "cima"
            "esquerda" -> "esquerda"
            "direita" -> "direita"
            else -> "baixo"
        }
        
        PresenceManager.updatePresenceData(
            stationId = currentStationId,
            gridX = x,
            gridY = y,
            direction = netDir,
            gender = PlayerManager.player.sexo
        )

        // Verifica saída da Estação São Paulo pela escada inferior (x = 1, y = 19 ou x = 2, y = 19)
        if (currentStationId == "sao_paulo" && ((x == 1 && y == 19) || (x == 2 && y == 19))) {
            Log.d("StationActivity", "[STAIR_CELL_TRACE] EXIT_CHECK_MATCH position=($x,$y)")
            if (!isExitPromptVisible && (lastCheckedExitX != x || lastCheckedExitY != y)) {
                Log.d("StationActivity", "[STAIR_DEBUG] EXIT_DIALOG_TRIGGERED")
                isExitPromptVisible = true
                lastCheckedExitX = x
                lastCheckedExitY = y
                mostrarDialogoSaida()
            }
        } else {
            Log.d("StationActivity", "[STAIR_CELL_TRACE] EXIT_CHECK_NO_MATCH position=($x,$y)")
            if (!((x == 1 && y == 19) || (x == 2 && y == 19))) {
                lastCheckedExitX = -1
                lastCheckedExitY = -1
            }
        }
    }

    private fun mostrarDialogoSaida() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Estação São Paulo")
            .setMessage("Você quer voltar para as Aventuras em São Paulo?")
            .setPositiveButton("Voltar para as Aventuras") { _, _ ->
                isExitPromptVisible = false
                finish()
            }
            .setNegativeButton("Continuar na estação") { dialogInterface, _ ->
                isExitPromptVisible = false
                dialogInterface.dismiss()
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }

    private fun abrirChatAntonio() {
        if (!conversations.containsKey("antonio")) {
            conversations["antonio"] = ChatConversation("antonio", "Antônio", isNpc = true)
        }
        
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.registerOrRenewInteraction("npc_vendedor_antonio", currentUserId)

        selecionarConversa("antonio")
        
        if (conversations["antonio"]?.history?.isEmpty() == true) {
            estadoDialogoAntonio = "PERGUNTOU_COMPRA"
            val greetMsg = "Antônio: Olá! Você deseja comprar uma passagem para o Rio de Janeiro?"
            adicionarMensagem(greetMsg, isNpc = true)

            val npcPos = gridView.getNpcPosition("NPC Antônio")
            if (npcPos != null) {
                gridView.addSpeechDialog(
                    senderId = "npc_antonio",
                    senderName = "NPC Antônio",
                    message = "Olá! Você deseja comprar uma passagem para o Rio de Janeiro?",
                    worldX = npcPos.x,
                    worldY = npcPos.y,
                    isPrivate = true,
                    allowedUsers = setOf(currentUserId, "antonio")
                )
            }
        }
    }

    private fun abrirChatCarlos() {
        if (!conversations.containsKey("carlos")) {
            conversations["carlos"] = ChatConversation("carlos", "Carlos", isNpc = true)
        }
        
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.registerOrRenewInteraction("npc_vendedor_carlos", currentUserId)

        selecionarConversa("carlos")
        
        if (conversations["carlos"]?.history?.isEmpty() == true) {
            estadoDialogoCarlos = "PERGUNTOU_COMPRA"
            val greetMsg = "Carlos: Olá! Você deseja comprar uma passagem para São Paulo?"
            adicionarMensagem(greetMsg, isNpc = true)

            val npcPos = gridView.getNpcPosition("NPC Carlos")
            if (npcPos != null) {
                gridView.addSpeechDialog(
                    senderId = "npc_carlos",
                    senderName = "NPC Carlos",
                    message = "Olá! Você deseja comprar uma passagem para São Paulo?",
                    worldX = npcPos.x,
                    worldY = npcPos.y,
                    isPrivate = true,
                    allowedUsers = setOf(currentUserId, "carlos")
                )
            }
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

    override fun onDestroy() {
        super.onDestroy()
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.removeInteraction("npc_vendedor_antonio", currentUserId)
        SharedNpcManager.stopNpcSystem()

        // Limpa os dados da estação ao sair
        PresenceManager.updatePresenceData(
            stationId = null,
            gridX = -1,
            gridY = -1,
            direction = "baixo",
            gender = ""
        )
    }
}
