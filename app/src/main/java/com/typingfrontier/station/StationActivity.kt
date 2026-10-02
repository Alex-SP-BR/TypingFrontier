package com.typingfrontier.station

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
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
import com.typingfrontier.ExplorationActivity
import com.typingfrontier.economy.ProfessionManager
import com.typingfrontier.npc.SharedNpcManager
import com.typingfrontier.social.ModerationRepository
import com.typingfrontier.social.PresenceManager
import com.typingfrontier.social.PrivateMessage
import com.typingfrontier.social.PrivateMessageRepository
import com.typingfrontier.social.SocialProfileRepository
import com.typingfrontier.social.SupabaseManager
import com.typingfrontier.utils.CurrencyUtils
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StationActivity : AppCompatActivity(), StationGridView.InteractionListener {
    
    // Modelo de Conversa
    private data class ChatConversation(
        val id: String,
        val title: String,
        var history: String = "",
        val isNpc: Boolean = false,
        var lastActivityTime: Long = System.currentTimeMillis(),
        val privateMessages: MutableList<PrivateMessage> = mutableListOf()
    )

    @Serializable
    data class PublicMessage(
        val id: String? = null,
        @SerialName("sender_id") val senderId: String,
        @SerialName("station_id") val stationId: String,
        val message: String,
        @SerialName("created_at") val createdAt: String = ""
    )

    private lateinit var publicChatLayout: LinearLayout
    private val activeDisplayedPublicMessageIds = ConcurrentHashMap.newKeySet<String>()
    private var publicMessagesChannel: RealtimeChannel? = null
    private var publicMessagesJob: Job? = null

    private fun iniciarRealtimeMensagensPublicas(stationId: String) {
        publicMessagesJob?.cancel()
        val oldChannel = publicMessagesChannel
        publicMessagesChannel = null

        val channelId = "station-public-chat:$stationId"

        publicMessagesJob = lifecycleScope.launch {
            try {
                if (oldChannel != null) {
                    try {
                        SupabaseManager.client.realtime.removeChannel(oldChannel)
                    } catch (_: Exception) {}
                }

                val newChannel = SupabaseManager.client.realtime.channel(channelId)
                publicMessagesChannel = newChannel

                val changeFlow = newChannel.postgresChangeFlow<PostgresAction>("public") {
                    table = "public_messages"
                    filter("station_id", FilterOperator.EQ, stationId)
                }

                launch {
                    changeFlow.collect { action ->
                        if (action is PostgresAction.Insert) {
                            try {
                                val pubMsg = action.decodeRecord<PublicMessage>()
                                if (pubMsg.id != null && activeDisplayedPublicMessageIds.add(pubMsg.id)) {
                                    val senderName = PresenceManager.onlineUsers.value.find { it.user_id == pubMsg.senderId }?.username
                                        ?: "Viajante"
                                    if (currentConversationId == "public" && !isChatHidden) {
                                        adicionarMensagemPublicaNaUI(pubMsg, "@$senderName")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("StationActivity", "Erro ao decodificar mensagem pública Realtime: ${e.message}")
                            }
                        }
                    }
                }

                newChannel.subscribe(blockUntilSubscribed = true)
            } catch (e: Exception) {
                Log.e("StationActivity", "Erro ao iniciar Realtime público: ${e.message}")
                publicMessagesChannel = null
            }
        }
    }

    private fun carregarHistoricoMensagensPublicas(stationId: String) {
        lifecycleScope.launch {
            try {
                val messages = SupabaseManager.client.postgrest["public_messages"]
                    .select {
                        filter { eq("station_id", stationId) }
                        order("created_at", Order.ASCENDING)
                        limit(50)
                    }.decodeList<PublicMessage>()

                publicChatLayout.removeAllViews()
                activeDisplayedPublicMessageIds.clear()

                messages.forEach { pubMsg ->
                    pubMsg.id?.let { activeDisplayedPublicMessageIds.add(it) }
                    val senderName = PresenceManager.onlineUsers.value.find { it.user_id == pubMsg.senderId }?.username
                        ?: "Viajante"
                    adicionarMensagemPublicaNaUI(pubMsg, "@$senderName")
                }
            } catch (e: Exception) {
                Log.e("StationActivity", "Erro ao carregar histórico público: ${e.message}")
            }
        }
    }

    private fun adicionarMensagemPublicaNaUI(pubMsg: PublicMessage, senderLabel: String) {
        val context = this
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 4)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tv = TextView(context).apply {
            text = "$senderLabel: ${pubMsg.message}"
            setTextColor(Color.parseColor("#E8EDF2"))
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(tv)

        if (pubMsg.id != null) {
            val btnReportMsg = TextView(context).apply {
                text = "▷"
                textSize = 13f
                setTextColor(Color.parseColor("#889099"))
                setPadding(12, 4, 4, 4)
                setOnClickListener {
                    mostrarDialogDenunciaMensagemPublicaEstacao(pubMsg, senderLabel)
                }
            }
            row.addView(btnReportMsg)
        }

        publicChatLayout.addView(row)
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun mostrarDialogDenunciaMensagemPublicaEstacao(pubMsg: PublicMessage, senderName: String) {
        if (pubMsg.id == null) {
            Toast.makeText(this, "ID da mensagem inválido para denúncia.", Toast.LENGTH_SHORT).show()
            return
        }

        val reasons = arrayOf("Spam / Propaganda", "Ofensas / Assédio", "Conteúdo Inadequado", "Outros")
        val builder = AlertDialog.Builder(this, R.style.Theme_TypingFrontier_AdminDialog)
            .setTitle("Denunciar Mensagem Pública")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val txtPreview = TextView(this).apply {
            text = "Mensagem de $senderName:\n\"${pubMsg.message}\""
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(txtPreview)

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@StationActivity, android.R.layout.simple_spinner_dropdown_item, reasons)
        }
        layout.addView(spinner)

        val edtDesc = EditText(this).apply {
            hint = "Descrição opcional (máx 200)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        layout.addView(edtDesc)

        builder.setView(layout)
        builder.setPositiveButton("Enviar Denúncia") { _, _ ->
            val reason = reasons[spinner.selectedItemPosition]
            val desc = edtDesc.text.toString().trim()
            lifecycleScope.launch {
                try {
                    ModerationRepository.createReport("public_message", pubMsg.id, reason, desc.ifEmpty { null })
                    Toast.makeText(this@StationActivity, "Denúncia enviada com sucesso.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@StationActivity, "Erro ao enviar denúncia: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun renderizarMensagensPrivadasDaConversa(conv: ChatConversation) {
        privateChatLayout.removeAllViews()
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: ""

        conv.privateMessages.forEach { msg ->
            val senderLabel = if (msg.senderId == currentUserId) {
                "@${PlayerManager.player.nome.ifEmpty { "Viajante" }}"
            } else {
                "@${conv.title}"
            }
            adicionarMensagemPrivadaNaUI(msg, senderLabel)
        }
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun adicionarMensagemPrivadaNaUI(msg: PrivateMessage, senderLabel: String) {
        val context = this
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: ""

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 4)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tv = TextView(context).apply {
            text = "$senderLabel: ${msg.message}"
            setTextColor(Color.parseColor("#74C6E0"))
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(tv)

        if (msg.id != null && msg.senderId != currentUserId) {
            val btnReportMsg = TextView(context).apply {
                text = "▷"
                textSize = 13f
                setTextColor(Color.parseColor("#889099"))
                setPadding(12, 4, 4, 4)
                setOnClickListener {
                    mostrarDialogDenunciaMensagemPrivadaEstacao(msg, senderLabel.removePrefix("@"))
                }
            }
            row.addView(btnReportMsg)
        }

        privateChatLayout.addView(row)
        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun mostrarDialogDenunciaMensagemPrivadaEstacao(msg: PrivateMessage, targetUsername: String) {
        if (msg.id == null) {
            Toast.makeText(this, "ID da mensagem inválido para denúncia.", Toast.LENGTH_SHORT).show()
            return
        }

        val reasons = arrayOf("Spam / Propaganda", "Ofensas / Assédio", "Conteúdo Inadequado", "Outros")
        val builder = AlertDialog.Builder(this, R.style.Theme_TypingFrontier_AdminDialog)
            .setTitle("Denunciar Mensagem Privada")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val txtPreview = TextView(this).apply {
            text = "Mensagem de @$targetUsername:\n\"${msg.message}\""
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(txtPreview)

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@StationActivity, android.R.layout.simple_spinner_dropdown_item, reasons)
        }
        layout.addView(spinner)

        val edtDesc = EditText(this).apply {
            hint = "Descrição opcional (máx 200)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        layout.addView(edtDesc)

        builder.setView(layout)
        builder.setPositiveButton("Enviar Denúncia") { _, _ ->
            val reason = reasons[spinner.selectedItemPosition]
            val desc = edtDesc.text.toString().trim()
            lifecycleScope.launch {
                try {
                    ModerationRepository.createReport("private_message", msg.id, reason, desc.ifEmpty { null })
                    Toast.makeText(this@StationActivity, "Denúncia enviada com sucesso.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@StationActivity, "Erro ao enviar denúncia: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

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
    private lateinit var privateChatLayout: LinearLayout

    // Gerenciamento de Conversas e Estação
    private var currentStationId: String = "sao_paulo"
    private val conversations = mutableMapOf<String, ChatConversation>()
    private var currentConversationId = "public"
    private var isChatExpanded = false
    private var isChatHidden = false

    companion object {
        var lastKnownGridX: Int = 5
        var lastKnownGridY: Int = 20
        var lastKnownDirection: String = "frente"
    }

    private var stationBroadcastChannel: RealtimeChannel? = null
    private var stationBroadcastJob: Job? = null
    private val remoteMovements = ConcurrentHashMap<String, PlayerMovePayload>()
    private val initialSyncedUsers = ConcurrentHashMap.newKeySet<String>()
    private val processedPublicMessages = ConcurrentHashMap.newKeySet<String>().let { ConcurrentHashMap<String, Long>() }
    private var latestPresenceList: List<PresenceManager.PresencePayload> = emptyList()

    private var lastSentPayload: PlayerMovePayload? = null
    private var pendingPayload: PlayerMovePayload? = null
    private var lastSendTimestamp: Long = 0L
    private val movementIntervalMs = 200L
    private var broadcastThrottleJob: Job? = null

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
        gridView.setInitialPosition(lastKnownGridX, lastKnownGridY, lastKnownDirection)
        playerX_direcao_local = lastKnownDirection

        vincularUiChat()
        iniciarCanalBroadcastEstacao(currentStationId)
        iniciarRealtimeMensagensPublicas(currentStationId)
        
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

        val currentUserId = SocialProfileRepository.getCurrentUserId()
        if (currentUserId != null) {
            lifecycleScope.launch {
                PrivateMessageRepository.incomingMessages.collect { msg ->
                    if (msg.senderId == currentUserId) return@collect
                    val senderId = msg.senderId
                    val senderNameStr = latestPresenceList.find { it.user_id == senderId }?.username ?: "Viajante"

                    if (!conversations.containsKey(senderId)) {
                        conversations[senderId] = ChatConversation(
                            id = senderId,
                            title = senderNameStr,
                            isNpc = false
                        )
                    }

                    val conv = conversations[senderId]!!
                    conv.history += "\n$senderNameStr: ${msg.message}"
                    if (conv.privateMessages.none { it.id == msg.id && msg.id != null }) {
                        conv.privateMessages.add(msg)
                    }
                    conv.lastActivityTime = System.currentTimeMillis()

                    if (currentConversationId == senderId && !isChatHidden) {
                        adicionarMensagemPrivadaNaUI(msg, "@$senderNameStr")
                        scrollChat.post { scrollChat.fullScroll(View.FOCUS_DOWN) }
                    } else {
                        atualizarInterfaceAbas()
                    }

                    val movePayload = remoteMovements[senderId]
                    val presencePayload = latestPresenceList.find { it.user_id == senderId }
                    val posX = movePayload?.gridX ?: presencePayload?.gridX ?: 5
                    val posY = movePayload?.gridY ?: presencePayload?.gridY ?: 20

                    gridView.addSpeechDialog(
                        senderId = senderId,
                        senderName = senderNameStr,
                        message = msg.message,
                        worldX = posX,
                        worldY = posY,
                        isPrivate = true,
                        allowedUsers = setOf(senderId, currentUserId)
                    )
                }
            }
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

        val initialGridX = gridView.getPlayerX()
        val initialGridY = gridView.getPlayerY()

        // Atualiza com a posição real obtida do gridView e inicia a presença
        PresenceManager.updatePresenceData(
            stationId = currentStationId,
            gridX = initialGridX,
            gridY = initialGridY,
            direction = dirPresenca,
            gender = player.sexo
        )
    }

    private var playerX_direcao_local = "frente" // Auxiliar para mapeamento de direção

    private fun observarJogadoresOnline() {
        lifecycleScope.launch {
            PresenceManager.onlineUsers.collect { list ->
                latestPresenceList = list
                val currentUserId = SocialProfileRepository.getCurrentUserId() ?: ""
                val onlineInStation = list.filter { it.stationId == currentStationId }.map { it.user_id }.toSet()
                
                // Limpa interações de usuários que saíram ou desconectaram
                SharedNpcManager.cleanDisconnectedUsers(currentStationId, onlineInStation)
                initialSyncedUsers.retainAll(onlineInStation)

                Log.d("StationActivity", "[PRESENCE_DEBUG] RAW online users received: ${list.size}")
                list.forEach { 
                    Log.d("StationActivity", "[PRESENCE_DEBUG] User: ${it.username} (id=${it.user_id}, station=${it.stationId})")
                }

                atualizarJogadoresRemotosNoGrid()

                // Exibe balões de fala para mensagens públicas de jogadores remotos no momento do recebimento
                val filtered = list.filter { 
                    it.stationId == currentStationId && it.user_id != currentUserId 
                }
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

    private fun atualizarJogadoresRemotosNoGrid() {
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: ""
        val filtered = latestPresenceList.filter { 
            it.stationId == currentStationId && it.user_id != currentUserId 
        }.map { presence ->
            val move = remoteMovements[presence.user_id]
            if (move != null) {
                presence.copy(
                    gridX = move.gridX,
                    gridY = move.gridY,
                    direction = move.direction
                )
            } else {
                presence
            }
        }
        Log.d("StationActivity", "[BROADCAST_TRACE] Filtered remote players combined with broadcast (station=$currentStationId, not self): ${filtered.size}")
        gridView.setRemotePlayers(filtered, currentUserId)
    }

    private fun iniciarCanalBroadcastEstacao(stationId: String) {
        remoteMovements.clear()
        initialSyncedUsers.clear()
        processedPublicMessages.clear()
        lastSentPayload = null
        pendingPayload = null
        lastSendTimestamp = 0L
        broadcastThrottleJob?.cancel()
        stationBroadcastJob?.cancel()

        val oldChannel = stationBroadcastChannel
        stationBroadcastChannel = null

        val channelId = "station:$stationId"

        stationBroadcastJob = lifecycleScope.launch {
            try {
                if (oldChannel != null) {
                    try {
                        SupabaseManager.client.realtime.removeChannel(oldChannel)
                    } catch (_: Exception) {}
                }

                val newChannel = SupabaseManager.client.realtime.channel(channelId) {
                    broadcast {
                        receiveOwnBroadcasts = true
                    }
                }
                stationBroadcastChannel = newChannel

                val moveFlow = newChannel.broadcastFlow<PlayerMovePayload>("player_movement")

                launch {
                    newChannel.status.collect { status ->
                        Log.d("StationActivity", "[BROADCAST_TRACE] Status do canal $channelId: $status")
                        if (status == RealtimeChannel.Status.SUBSCRIBED) {
                            Log.d("StationActivity", "[BROADCAST_TRACE] SUBSCRIBED no canal de movimento $channelId")
                            
                            val currentUserId = SocialProfileRepository.getCurrentUserId()
                            if (currentUserId != null && ::gridView.isInitialized) {
                                val netDir = when (lastKnownDirection) {
                                    "costas" -> "cima"
                                    "esquerda" -> "esquerda"
                                    "direita" -> "direita"
                                    else -> "baixo"
                                }
                                pendingPayload = PlayerMovePayload(
                                    user_id = currentUserId,
                                    gridX = gridView.getPlayerX(),
                                    gridY = gridView.getPlayerY(),
                                    direction = netDir
                                )
                                tentaEnviarMovimento()
                            }
                        }
                    }
                }

                launch {
                    moveFlow.collect { payload ->
                        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: ""
                        if (payload.user_id == currentUserId) return@collect

                        Log.d("StationActivity", "[BROADCAST_TRACE] Movimento remoto recebido: user=${payload.user_id}, pos=(${payload.gridX},${payload.gridY}), dir=${payload.direction}")
                        remoteMovements[payload.user_id] = payload
                        atualizarJogadoresRemotosNoGrid()

                        if (initialSyncedUsers.add(payload.user_id)) {
                            val channel = stationBroadcastChannel
                            if (channel != null && channel.status.value == RealtimeChannel.Status.SUBSCRIBED && ::gridView.isInitialized) {
                                val netDir = when (lastKnownDirection) {
                                    "costas" -> "cima"
                                    "esquerda" -> "esquerda"
                                    "direita" -> "direita"
                                    else -> "baixo"
                                }
                                val responsePayload = PlayerMovePayload(
                                    user_id = currentUserId,
                                    gridX = gridView.getPlayerX(),
                                    gridY = gridView.getPlayerY(),
                                    direction = netDir
                                )
                                lifecycleScope.launch {
                                    try {
                                        channel.broadcast("player_movement", responsePayload)
                                        Log.d("StationActivity", "[BROADCAST_TRACE] Resposta de sincronização inicial enviada para user=${payload.user_id}")
                                    } catch (e: Exception) {
                                        Log.e("StationActivity", "[BROADCAST_TRACE] Erro ao enviar resposta de sincronização inicial: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }

                newChannel.subscribe(blockUntilSubscribed = true)
            } catch (e: Exception) {
                Log.e("StationActivity", "[BROADCAST_TRACE] Erro no canal de broadcast: ${e.message}")
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

        publicChatLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.VISIBLE
        }
        privateChatLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
        }
        val scrollContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        scrollChat.removeAllViews()
        scrollContainer.addView(txtChatMessages)
        scrollContainer.addView(publicChatLayout)
        scrollContainer.addView(privateChatLayout)
        scrollChat.addView(scrollContainer)

        btnChatClose.setOnClickListener {
            esconderChat()
        }

        val btnSair = findViewById<Button>(R.id.btnSairEstacao)
        btnSair.setOnClickListener {
            finish()
        }

        val btnVip = findViewById<Button>(R.id.btnVipStation)
        btnVip.setOnClickListener {
            mostrarDialogoVip()
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
        } else if (currentConversationId == "carlos" && id != "carlos") {
            SharedNpcManager.removeInteraction("npc_vendedor_carlos", currentUserId)
        } else if (currentConversationId == "vinicius" && id != "vinicius") {
            SharedNpcManager.removeInteraction("npc_gerente_vinicius", currentUserId)
        } else if (currentConversationId == "henrique" && id != "henrique") {
            SharedNpcManager.removeInteraction("npc_gerente_henrique", currentUserId)
        }
        
        // Se entrar em uma conversa NPC e o chat não estiver escondido, registra/renova interação
        if (id == "antonio" && !isChatHidden) {
            SharedNpcManager.registerOrRenewInteraction("npc_vendedor_antonio", currentUserId)
        } else if (id == "carlos" && !isChatHidden) {
            SharedNpcManager.registerOrRenewInteraction("npc_vendedor_carlos", currentUserId)
        } else if (id == "vinicius" && !isChatHidden) {
            SharedNpcManager.registerOrRenewInteraction("npc_gerente_vinicius", currentUserId)
        } else if (id == "henrique" && !isChatHidden) {
            SharedNpcManager.registerOrRenewInteraction("npc_gerente_henrique", currentUserId)
        }

        currentConversationId = id
        concluirDigitacaoImediata()
        
        txtChatNpcName.text = conv.title
        val isPublic = id == "public"
        val isNpc = conv.isNpc || id == "antonio" || id == "carlos" || id == "vinicius" || id == "henrique"

        txtChatMessages.visibility = if (!isPublic && isNpc) View.VISIBLE else View.GONE
        publicChatLayout.visibility = if (isPublic) View.VISIBLE else View.GONE
        privateChatLayout.visibility = if (!isPublic && !isNpc) View.VISIBLE else View.GONE

        if (!isPublic && !isNpc) {
            renderizarMensagensPrivadasDaConversa(conv)
        } else if (!isPublic && isNpc) {
            txtChatMessages.setTextColor(Color.parseColor("#74C6E0"))
            txtChatMessages.text = conv.history
        }
        
        atualizarInterfaceAbas()
        
        if (isChatHidden) mostrarChat()
        
        scrollChat.post {
            scrollChat.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun fecharConversa(id: String) {
        if (id == "public") return // Não fecha a pública
        
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        if (id == "antonio") {
            SharedNpcManager.removeInteraction("npc_vendedor_antonio", currentUserId)
        } else if (id == "carlos") {
            SharedNpcManager.removeInteraction("npc_vendedor_carlos", currentUserId)
        } else if (id == "vinicius") {
            SharedNpcManager.removeInteraction("npc_gerente_vinicius", currentUserId)
        } else if (id == "henrique") {
            SharedNpcManager.removeInteraction("npc_gerente_henrique", currentUserId)
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
        
        val sortedConversations = conversations.values.sortedWith(compareBy<ChatConversation> { 
            if (it.id == "public") 0 else 1 
        }.thenByDescending { 
            it.lastActivityTime 
        })

        sortedConversations.forEach { conv ->
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
        
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        if (currentConversationId == "antonio") {
            SharedNpcManager.removeInteraction("npc_vendedor_antonio", currentUserId)
        } else if (currentConversationId == "carlos") {
            SharedNpcManager.removeInteraction("npc_vendedor_carlos", currentUserId)
        } else if (currentConversationId == "vinicius") {
            SharedNpcManager.removeInteraction("npc_gerente_vinicius", currentUserId)
        } else if (currentConversationId == "henrique") {
            SharedNpcManager.removeInteraction("npc_gerente_henrique", currentUserId)
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

        // CAPTURA DE POSIÇÃO NO EXATO INSTANTE DO ENVIO (REGRA CRÍTICA)
        val sendX = gridView.getPlayerX()
        val sendY = gridView.getPlayerY()

        // Roteamento de envio baseado no tipo de conversa ativa
        val conv = conversations[currentConversationId]
        val isNpc = conv?.isNpc == true || currentConversationId == "antonio" || currentConversationId == "carlos" || currentConversationId == "vinicius" || currentConversationId == "henrique"

        if (isPublic) {
            edtChatMessage.setText("")
            lifecycleScope.launch {
                try {
                    val data = buildJsonObject {
                        put("sender_id", currentUserId)
                        put("station_id", currentStationId)
                        put("message", msg)
                    }
                    val inserted = SupabaseManager.client.postgrest["public_messages"]
                        .insert(data) {
                            select()
                        }.decodeSingle<PublicMessage>()

                    inserted.id?.let { activeDisplayedPublicMessageIds.add(it) }
                    adicionarMensagemPublicaNaUI(inserted, "@${p.nome}")

                    gridView.addSpeechDialog(
                        senderId = currentUserId,
                        senderName = p.nome.ifEmpty { "Viajante" },
                        message = msg,
                        worldX = sendX,
                        worldY = sendY,
                        isPrivate = false,
                        allowedUsers = emptySet()
                    )

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
                } catch (e: Exception) {
                    Log.e("StationActivity", "Erro ao enviar mensagem pública: ${e.message}")
                    Toast.makeText(this@StationActivity, "Erro ao enviar mensagem: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (isNpc) {
            adicionarMensagem("@${p.nome}: $msg", isNpc = false)
            edtChatMessage.setText("")

            gridView.addSpeechDialog(
                senderId = currentUserId,
                senderName = p.nome.ifEmpty { "Viajante" },
                message = msg,
                worldX = sendX,
                worldY = sendY,
                isPrivate = true,
                allowedUsers = setOf(currentUserId, currentConversationId)
            )

            if (currentConversationId == "antonio") {
                processarDialogoAntonio(msg)
            } else if (currentConversationId == "carlos") {
                processarDialogoCarlos(msg)
            } else if (currentConversationId == "vinicius") {
                processarDialogoVinicius(msg)
            } else if (currentConversationId == "henrique") {
                processarDialogoHenrique(msg)
            }
        } else {
            val authUserId = SocialProfileRepository.getCurrentUserId()
            if (authUserId == null) {
                Log.e("StationActivity", "Erro: Usuário não autenticado. Impossível enviar mensagem privada.")
                return
            }
            val receiverId = currentConversationId
            edtChatMessage.setText("")

            lifecycleScope.launch {
                try {
                    val sentMsg = PrivateMessageRepository.sendMessage(authUserId, receiverId, msg)
                    
                    // Sucesso confirmado pelo banco: adiciona ao histórico e exibe o balão
                    adicionarMensagem("@${p.nome}: $msg", isNpc = false)
                    conversations[receiverId]?.let { conv ->
                        if (conv.privateMessages.none { it.id == sentMsg.id && sentMsg.id != null }) {
                            conv.privateMessages.add(sentMsg)
                        }
                        conv.lastActivityTime = System.currentTimeMillis()
                        atualizarInterfaceAbas()
                    }

                    if (currentConversationId == receiverId && !isChatHidden) {
                        adicionarMensagemPrivadaNaUI(sentMsg, "@${p.nome.ifEmpty { "Viajante" }}")
                    }
                    
                    gridView.addSpeechDialog(
                        senderId = authUserId,
                        senderName = p.nome.ifEmpty { "Viajante" },
                        message = msg,
                        worldX = sendX,
                        worldY = sendY,
                        isPrivate = true,
                        allowedUsers = setOf(authUserId, receiverId)
                    )
                } catch (e: Exception) {
                    Log.e("StationActivity", "Erro ao enviar mensagem privada (não adicionada ao histórico): ${e.message}")
                }
            }
        }
    }

    private var estadoDialogoAntonio = "INICIO" // "INICIO", "PERGUNTOU_COMPRA", "AGUARDANDO_CONFIRMACAO", "PERGUNTOU_HABILITACAO", "CONFIRMAR_HABILITACAO"
    private var estadoDialogoCarlos = "INICIO"  // "INICIO", "PERGUNTOU_COMPRA", "AGUARDANDO_CONFIRMACAO", "PERGUNTOU_HABILITACAO", "CONFIRMAR_HABILITACAO"

    private fun isUsuarioIsentoTarifaFerroviaria(): Boolean {
        val role = SocialProfileRepository.currentProfile?.role ?: "usuario"
        return role == "moderator" || role == "senior_moderator" || role == "administrator"
    }

    private fun traduzirRoleCargo(role: String?): String {
        return when (role) {
            "administrator" -> "ADMINISTRADOR"
            "senior_moderator" -> "MODERADOR SÊNIOR"
            "moderator" -> "MODERADOR"
            else -> "USUÁRIO"
        }
    }

    private fun processarDialogoAntonio(msg: String) {
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.registerOrRenewInteraction("npc_vendedor_antonio", currentUserId)

        val lower = msg.lowercase().trim()
        val player = PlayerManager.player
        val role = SocialProfileRepository.currentProfile?.role ?: "usuario"
        val isIsento = isUsuarioIsentoTarifaFerroviaria()

        val responseText: String

        if (isIsento) {
            responseText = "Antônio: Como ${traduzirRoleCargo(role)}, você possui isenção total de taxas e passagens. Pode embarcar diretamente no trem para o Rio de Janeiro quando desejar."
        } else if (!player.habilitadoFerrovia) {
            when (estadoDialogoAntonio) {
                "INICIO", "PERGUNTOU_HABILITACAO" -> {
                    if (lower.contains("não") || lower.contains("nao") || lower.contains("cancelar")) {
                        estadoDialogoAntonio = "INICIO"
                        responseText = "Antônio: Tudo bem. A taxa de 30.000 Frons é uma tarifa administrativa única para habilitar o acesso ferroviário. Assim que desejar se habilitar, é só falar comigo."
                    } else if (lower.contains("sim") || lower.contains("comprar") || lower.contains("quero") || lower.contains("pagar") || lower == "oi" || lower == "olá" || lower == "ola") {
                        if (player.dinheiro < 30000) {
                            estadoDialogoAntonio = "INICIO"
                            responseText = "Antônio: A taxa administrativa de habilitação custa 30.000 Frons (paga uma única vez). Você possui ${CurrencyUtils.formatar(player.dinheiro)}. Não é possível se habilitar sem saldo suficiente."
                        } else {
                            estadoDialogoAntonio = "CONFIRMAR_HABILITACAO"
                            responseText = "Antônio: A taxa administrativa de habilitação custa 30.000 Frons (paga uma única vez). Ela ativa seu acesso permanente ao transporte ferroviário. Confirmar pagamento de 30.000 Frons?"
                        }
                    } else {
                        responseText = "Antônio: Para utilizar os trens, é necessária uma taxa administrativa única de habilitação de 30.000 Frons. Responda 'Sim' para pagar ou 'Não' para cancelar."
                    }
                }
                "CONFIRMAR_HABILITACAO" -> {
                    if (lower.contains("sim") || lower.contains("confirmar") || lower.contains("quero") || lower.contains("pagar")) {
                        estadoDialogoAntonio = "INICIO"
                        if (player.dinheiro < 30000) {
                            responseText = "Antônio: Você não possui Frons suficientes no momento."
                        } else {
                            player.dinheiro -= 30000
                            player.habilitadoFerrovia = true
                            PlayerManager.save(this)
                            responseText = "Antônio: Habilitação ferroviária concluída com sucesso! Sua taxa administrativa foi paga e você está autorizado a usar o transporte ferroviário. Agora você já pode adquirir passagens por 2.000 Frons!"
                        }
                    } else {
                        estadoDialogoAntonio = "INICIO"
                        responseText = "Antônio: Tudo bem. Assim que desejar realizar sua habilitação ferroviária, é só falar comigo."
                    }
                }
                else -> {
                    estadoDialogoAntonio = "INICIO"
                    responseText = "Antônio: Olá! Para utilizar os trens, é necessária a habilitação administrativa única de 30.000 Frons."
                }
            }
        } else {
            when (estadoDialogoAntonio) {
                "INICIO" -> {
                    if (lower == "oi" || lower == "olá" || lower == "ola" || lower.contains("comprar") || lower.contains("passagem")) {
                        estadoDialogoAntonio = "PERGUNTOU_COMPRA"
                        responseText = "Antônio: Olá! Sua habilitação ferroviária está ativa. Deseja comprar uma passagem para o Rio de Janeiro por 2.000 Frons?"
                    } else {
                        responseText = "Antônio: Olá! Sua habilitação ferroviária está ativa. Digite 'Oi' ou 'Comprar' para adquirir sua passagem por 2.000 Frons."
                    }
                }
                "PERGUNTOU_COMPRA" -> {
                    if (lower.contains("não") || lower.contains("nao") || lower.contains("passando") || lower.contains("cancelar")) {
                        estadoDialogoAntonio = "INICIO"
                        responseText = "Antônio: Tudo bem. Se precisar de uma passagem para o Rio de Janeiro, é só falar comigo."
                    } else if (lower.contains("sim") || lower.contains("comprar") || lower.contains("quero")) {
                        if (player.dinheiro < 2000) {
                            estadoDialogoAntonio = "INICIO"
                            responseText = "Antônio: A passagem para o Rio de Janeiro custa 2.000 Frons. Você possui ${CurrencyUtils.formatar(player.dinheiro)}. Saldo insuficiente no momento."
                        } else {
                            estadoDialogoAntonio = "AGUARDANDO_CONFIRMACAO"
                            responseText = "Antônio: A passagem para o Rio de Janeiro custa 2.000 Frons. Você possui ${CurrencyUtils.formatar(player.dinheiro)}. Deseja comprar a passagem?"
                        }
                    } else {
                        responseText = "Antônio: Deseja comprar uma passagem para o Rio de Janeiro por 2.000 Frons? Responda 'Sim' para continuar ou 'Não' para cancelar."
                    }
                }
                "AGUARDANDO_CONFIRMACAO" -> {
                    if (lower.contains("sim") || lower.contains("comprar") || lower.contains("quero") || lower.contains("passagem")) {
                        estadoDialogoAntonio = "INICIO"
                        if (player.dinheiro < 2000) {
                            responseText = "Antônio: Você não possui Frons suficientes no momento."
                        } else {
                            player.dinheiro -= 2000
                            val qtdAtual = player.mochila["ticket_rio"] ?: 0
                            player.mochila["ticket_rio"] = qtdAtual + 1
                            PlayerManager.save(this)
                            responseText = "Antônio: Passagem para o Rio de Janeiro comprada com sucesso! Ela já está na sua mochila."
                        }
                    } else {
                        estadoDialogoAntonio = "INICIO"
                        responseText = "Antônio: Tudo bem. Se precisar de uma passagem, é só falar comigo."
                    }
                }
                else -> {
                    estadoDialogoAntonio = "INICIO"
                    responseText = "Antônio: Olá! Como posso ajudar?"
                }
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
        val role = SocialProfileRepository.currentProfile?.role ?: "usuario"
        val isIsento = isUsuarioIsentoTarifaFerroviaria()

        val responseText: String

        if (isIsento) {
            responseText = "Carlos: Como ${traduzirRoleCargo(role)}, você possui isenção total de taxas e passagens. Pode embarcar diretamente no trem para São Paulo quando desejar."
        } else if (!player.habilitadoFerrovia) {
            when (estadoDialogoCarlos) {
                "INICIO", "PERGUNTOU_HABILITACAO" -> {
                    if (lower.contains("não") || lower.contains("nao") || lower.contains("cancelar")) {
                        estadoDialogoCarlos = "INICIO"
                        responseText = "Carlos: Tudo bem. A taxa de 30.000 Frons é uma tarifa administrativa única para habilitar o acesso ferroviário. Assim que desejar se habilitar, é só falar comigo."
                    } else if (lower.contains("sim") || lower.contains("comprar") || lower.contains("quero") || lower.contains("pagar") || lower == "oi" || lower == "olá" || lower == "ola") {
                        if (player.dinheiro < 30000) {
                            estadoDialogoCarlos = "INICIO"
                            responseText = "Carlos: A taxa administrativa de habilitação custa 30.000 Frons (paga uma única vez). Você possui ${CurrencyUtils.formatar(player.dinheiro)}. Não é possível se habilitar sem saldo suficiente."
                        } else {
                            estadoDialogoCarlos = "CONFIRMAR_HABILITACAO"
                            responseText = "Carlos: A taxa administrativa de habilitação custa 30.000 Frons (paga uma única vez). Ela ativa seu acesso permanente ao transporte ferroviário. Confirmar pagamento de 30.000 Frons?"
                        }
                    } else {
                        responseText = "Carlos: Para utilizar os trens, é necessária uma taxa administrativa única de habilitação de 30.000 Frons. Responda 'Sim' para pagar ou 'Não' para cancelar."
                    }
                }
                "CONFIRMAR_HABILITACAO" -> {
                    if (lower.contains("sim") || lower.contains("confirmar") || lower.contains("quero") || lower.contains("pagar")) {
                        estadoDialogoCarlos = "INICIO"
                        if (player.dinheiro < 30000) {
                            responseText = "Carlos: Você não possui Frons suficientes no momento."
                        } else {
                            player.dinheiro -= 30000
                            player.habilitadoFerrovia = true
                            PlayerManager.save(this)
                            responseText = "Carlos: Habilitação ferroviária concluída com sucesso! Sua taxa administrativa foi paga e você está autorizado a usar o transporte ferroviário. Agora você já pode adquirir passagens por 2.000 Frons!"
                        }
                    } else {
                        estadoDialogoCarlos = "INICIO"
                        responseText = "Carlos: Tudo bem. Assim que desejar realizar sua habilitação ferroviária, é só falar comigo."
                    }
                }
                else -> {
                    estadoDialogoCarlos = "INICIO"
                    responseText = "Carlos: Olá! Para utilizar os trens, é necessária a habilitação administrativa única de 30.000 Frons."
                }
            }
        } else {
            when (estadoDialogoCarlos) {
                "INICIO" -> {
                    if (lower == "oi" || lower == "olá" || lower == "ola" || lower.contains("comprar") || lower.contains("passagem")) {
                        estadoDialogoCarlos = "PERGUNTOU_COMPRA"
                        responseText = "Carlos: Olá! Sua habilitação ferroviária está ativa. Deseja comprar uma passagem para São Paulo por 2.000 Frons?"
                    } else {
                        responseText = "Carlos: Olá! Sua habilitação ferroviária está ativa. Digite 'Oi' ou 'Comprar' para adquirir sua passagem por 2.000 Frons."
                    }
                }
                "PERGUNTOU_COMPRA" -> {
                    if (lower.contains("não") || lower.contains("nao") || lower.contains("passando") || lower.contains("cancelar")) {
                        estadoDialogoCarlos = "INICIO"
                        responseText = "Carlos: Tudo bem. Se precisar de uma passagem para São Paulo, é só falar comigo."
                    } else if (lower.contains("sim") || lower.contains("comprar") || lower.contains("quero")) {
                        if (player.dinheiro < 2000) {
                            estadoDialogoCarlos = "INICIO"
                            responseText = "Carlos: A passagem para São Paulo custa 2.000 Frons. Você possui ${CurrencyUtils.formatar(player.dinheiro)}. Saldo insuficiente no momento."
                        } else {
                            estadoDialogoCarlos = "AGUARDANDO_CONFIRMACAO"
                            responseText = "Carlos: A passagem para São Paulo custa 2.000 Frons. Você possui ${CurrencyUtils.formatar(player.dinheiro)}. Deseja comprar a passagem?"
                        }
                    } else {
                        responseText = "Carlos: Deseja comprar uma passagem para São Paulo por 2.000 Frons? Responda 'Sim' para continuar ou 'Não' para cancelar."
                    }
                }
                "AGUARDANDO_CONFIRMACAO" -> {
                    if (lower.contains("sim") || lower.contains("comprar") || lower.contains("quero") || lower.contains("passagem")) {
                        estadoDialogoCarlos = "INICIO"
                        if (player.dinheiro < 2000) {
                            responseText = "Carlos: Você não possui Frons suficientes no momento."
                        } else {
                            player.dinheiro -= 2000
                            val qtdAtual = player.mochila["ticket_sao_paulo"] ?: 0
                            player.mochila["ticket_sao_paulo"] = qtdAtual + 1
                            PlayerManager.save(this)
                            responseText = "Carlos: Passagem para São Paulo comprada com sucesso! Ela já está na sua mochila."
                        }
                    } else {
                        estadoDialogoCarlos = "INICIO"
                        responseText = "Carlos: Tudo bem. Se precisar de uma passagem, é só falar comigo."
                    }
                }
                else -> {
                    estadoDialogoCarlos = "INICIO"
                    responseText = "Carlos: Olá! Como posso ajudar?"
                }
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
            val viniciusSprites = mapOf(
                "cima" to R.drawable.npc_vinicius_cima,
                "baixo" to R.drawable.npc_vinicius_baixo,
                "esquerda" to R.drawable.npc_vinicius_esquerda,
                "direita" to R.drawable.npc_vinicius_direita
            )
            gridView.setNpcData(
                name = "NPC Vinícius",
                x = 3,
                y = 6,
                direction = "baixo",
                sprites = viniciusSprites,
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
            val henriqueSprites = mapOf(
                "cima" to R.drawable.npc_henrique_cima,
                "baixo" to R.drawable.npc_henrique_baixo,
                "esquerda" to R.drawable.npc_henrique_esquerda,
                "direita" to R.drawable.npc_henrique_direita
            )
            gridView.setNpcData(
                name = "NPC Henrique",
                x = 3,
                y = 6,
                direction = "baixo",
                sprites = henriqueSprites,
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
        } else if (npcName == "NPC Vinícius" || npcName == "Vinícius") {
            abrirChatVinicius()
        } else if (npcName == "NPC Henrique" || npcName == "Henrique") {
            abrirChatHenrique()
        }
    }

    override fun onRemotePlayerTapped(userId: String, username: String) {
        if (!conversations.containsKey(userId)) {
            conversations[userId] = ChatConversation(
                id = userId,
                title = username,
                isNpc = false
            )
        }
        selecionarConversa(userId)
        if (isChatHidden) mostrarChat()
    }

    private fun mostrarDialogoVip() {
        val context = this
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (150 * resources.displayMetrics.density).toInt()
            ).apply { topMargin = 16.0f.toInt() }
        }

        val listLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(listLayout)

        var dialog: AlertDialog? = null
        var refreshList: () -> Unit = {}

        refreshList = fun() {
            listLayout.removeAllViews()
            val vipList = VipManager.getVipList(context)
            val onlineSet = PresenceManager.onlineUsers.value.map { it.user_id }.toSet()
            
            // Ordena: online primeiro
            val sortedVips = vipList.sortedByDescending { onlineSet.contains(it.userId) }

            dialog?.setTitle("⭐ Lista VIP (${vipList.size}/50)")

            if (sortedVips.isEmpty()) {
                val emptyTv = TextView(context).apply {
                    text = "Sua lista VIP está vazia."
                    setTextColor(Color.parseColor("#889099"))
                    setPadding(0, 24, 0, 24)
                    gravity = Gravity.CENTER
                }
                listLayout.addView(emptyTv)
                return
            }

            sortedVips.forEach { vip ->
                val isOnline = onlineSet.contains(vip.userId)
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        onRemotePlayerTapped(vip.userId, vip.username)
                        dialog?.dismiss()
                    }
                }

                val indicator = TextView(context).apply {
                    text = "●"
                    textSize = 16f
                    setTextColor(if (isOnline) Color.parseColor("#4CAF50") else Color.parseColor("#889099"))
                    setPadding(0, 0, 16, 0)
                }
                row.addView(indicator)

                val nameTv = TextView(context).apply {
                    text = vip.username
                    textSize = 16f
                    setTextColor(if (isOnline) Color.WHITE else Color.parseColor("#889099"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(nameTv)

                val btnRemove = Button(context).apply {
                    text = "Remover"
                    textSize = 12f
                    setTextColor(Color.parseColor("#FF5252"))
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        VipManager.removeVip(context, vip.userId)
                        refreshList()
                    }
                }
                row.addView(btnRemove)

                listLayout.addView(row)
            }
        }

        val btnAdd = Button(context).apply {
            text = "+ Adicionar VIP"
            setBackgroundColor(Color.parseColor("#286680"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                mostrarDialogoAdicionarVip {
                    refreshList()
                }
            }
        }
        container.addView(btnAdd)
        container.addView(scrollView)

        refreshList()

        dialog = AlertDialog.Builder(context, R.style.Theme_TypingFrontier_ShopDialog)
            .setTitle("⭐ Lista VIP (${VipManager.getVipList(this).size}/50)")
            .setView(container)
            .setPositiveButton("Fechar", null)
            .create()

        dialog.show()

        val presenceJob = lifecycleScope.launch {
            PresenceManager.onlineUsers.collect {
                refreshList()
            }
        }
        dialog.setOnDismissListener {
            presenceJob.cancel()
        }
    }

    private fun mostrarDialogoAdicionarVip(onAdded: () -> Unit) {
        val input = EditText(this).apply {
            hint = "Digite o username..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#889099"))
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
            .setTitle("Adicionar VIP")
            .setView(input)
            .setPositiveButton("Adicionar") { _, _ ->
                val usernameInput = input.text.toString().trim().removePrefix("@")
                if (usernameInput.isEmpty()) return@setPositiveButton

                val currentUserId = SocialProfileRepository.getCurrentUserId() ?: ""
                
                lifecycleScope.launch {
                    try {
                        val profile = ModerationRepository.getProfileByUsername(usernameInput)
                        if (profile == null) {
                            Toast.makeText(this@StationActivity, "Jogador não encontrado.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        if (profile.id == currentUserId) {
                            Toast.makeText(this@StationActivity, "Você não pode adicionar a si mesmo.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val added = VipManager.addVip(this@StationActivity, profile.id, profile.username)
                        if (!added) {
                            Toast.makeText(this@StationActivity, "Lista cheia (máx 50) ou jogador já adicionado.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@StationActivity, "${profile.username} adicionado aos VIPs!", Toast.LENGTH_SHORT).show()
                            onAdded()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@StationActivity, "Erro ao buscar jogador: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onTrainTapped() {
        if (currentStationId == "sao_paulo") {
            processarEmbarqueTrainSP()
        } else {
            processarEmbarqueTrainRio()
        }
    }

    override fun onNorthStairsTapped() {
        if (currentStationId == "sao_paulo") {
            if (!isExitPromptVisible) {
                isExitPromptVisible = true
                mostrarDialogoSaida(isNorth = true)
            }
        }
    }

    private fun processarEmbarqueTrainSP() {
        val player = PlayerManager.player
        val role = SocialProfileRepository.currentProfile?.role ?: "usuario"
        val isIsento = isUsuarioIsentoTarifaFerroviaria()

        if (isIsento) {
            AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
                .setTitle("🚆 Trem para o Rio de Janeiro")
                .setMessage("Como ${traduzirRoleCargo(role)}, você possui isenção total de taxas e passagens.\n\nDeseja embarcar no trem para o Rio de Janeiro agora?")
                .setPositiveButton("Embarcar") { _, _ ->
                    exibirTransicaoEIrParaEstacao(
                        transicaoResId = R.drawable.bg_transicao_sp_rio,
                        novaEstacaoId = "rio_de_janeiro",
                        bgNovaEstacaoResId = R.drawable.bg_estacao_rio_01,
                        tituloNovaEstacao = "Estação Rio de Janeiro"
                    )
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }

        if (!player.habilitadoFerrovia) {
            AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
                .setTitle("🚆 Acesso Ferroviário Bloqueado")
                .setMessage("Para utilizar o serviço ferroviário, é necessária a taxa administrativa única de habilitação de 30.000 Frons.\n\nFale com o NPC Antônio para realizar sua habilitação.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val qtdTicket = player.mochila["ticket_rio"] ?: 0

        if (qtdTicket <= 0) {
            AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
                .setTitle("🚆 Trem para o Rio de Janeiro")
                .setMessage("Para embarcar no trem para o Rio de Janeiro, você precisa ter uma passagem na sua mochila.\n\nFale com o NPC Antônio para adquirir sua passagem por 2.000 Frons.")
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
        val role = SocialProfileRepository.currentProfile?.role ?: "usuario"
        val isIsento = isUsuarioIsentoTarifaFerroviaria()

        if (isIsento) {
            AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
                .setTitle("🚆 Trem para São Paulo")
                .setMessage("Como ${traduzirRoleCargo(role)}, você possui isenção total de taxas e passagens.\n\nDeseja embarcar no trem para São Paulo agora?")
                .setPositiveButton("Embarcar") { _, _ ->
                    exibirTransicaoEIrParaEstacao(
                        transicaoResId = R.drawable.bg_transicao_rio_sp,
                        novaEstacaoId = "sao_paulo",
                        bgNovaEstacaoResId = R.drawable.bg_estacao_sp_01,
                        tituloNovaEstacao = "Estação São Paulo"
                    )
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }

        if (!player.habilitadoFerrovia) {
            AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
                .setTitle("🚆 Acesso Ferroviário Bloqueado")
                .setMessage("Para utilizar o serviço ferroviário, é necessária a taxa administrativa única de habilitação de 30.000 Frons.\n\nFale com o NPC Carlos para realizar sua habilitação.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val qtdTicket = player.mochila["ticket_sao_paulo"] ?: 0

        if (qtdTicket <= 0) {
            AlertDialog.Builder(this, R.style.Theme_TypingFrontier_ShopDialog)
                .setTitle("🚆 Trem para São Paulo")
                .setMessage("Para embarcar no trem para São Paulo, você precisa ter uma passagem na sua mochila.\n\nFale com o NPC Carlos para adquirir sua passagem por 2.000 Frons.")
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
            iniciarCanalBroadcastEstacao(novaEstacaoId)
            iniciarRealtimeMensagensPublicas(novaEstacaoId)
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
        lastKnownGridX = x
        lastKnownGridY = y
        lastKnownDirection = direction

        Log.d("StationActivity", "[STAIR_DEBUG] playerPosition x=$x, y=$y, direction=$direction")
        Log.d("StationActivity", "[STAIR_CELL_TRACE] EXIT_CHECK position=($x,$y)")
        // Converte a direção interna do grid para o padrão da rede
        val netDir = when (direction) {
            "costas" -> "cima"
            "esquerda" -> "esquerda"
            "direita" -> "direita"
            else -> "baixo"
        }

        val currentUserId = SocialProfileRepository.getCurrentUserId()
        if (currentUserId != null) {
            val payload = PlayerMovePayload(
                user_id = currentUserId,
                gridX = x,
                gridY = y,
                direction = netDir
            )
            if (payload != lastSentPayload) {
                pendingPayload = payload
                tentaEnviarMovimento()
            }
        }

        // Verifica saída da Estação São Paulo pela escada inferior (x = 1, y = 19 ou x = 2, y = 19)
        if (currentStationId == "sao_paulo" && ((x == 1 && y == 19) || (x == 2 && y == 19))) {
            Log.d("StationActivity", "[STAIR_CELL_TRACE] EXIT_CHECK_MATCH position=($x,$y)")
            if (!isExitPromptVisible && (lastCheckedExitX != x || lastCheckedExitY != y)) {
                Log.d("StationActivity", "[STAIR_DEBUG] EXIT_DIALOG_TRIGGERED")
                isExitPromptVisible = true
                lastCheckedExitX = x
                lastCheckedExitY = y
                mostrarDialogoSaida(isNorth = false)
            }
        } else {
            Log.d("StationActivity", "[STAIR_CELL_TRACE] EXIT_CHECK_NO_MATCH position=($x,$y)")
            if (!((x == 1 && y == 19) || (x == 2 && y == 19))) {
                lastCheckedExitX = -1
                lastCheckedExitY = -1
            }
        }
    }

    private fun mostrarDialogoSaida(isNorth: Boolean = false) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Estação São Paulo")
            .setMessage("Você quer voltar para as Aventuras em São Paulo?")
            .setPositiveButton("Voltar para as Aventuras") { _, _ ->
                isExitPromptVisible = false
                if (::gridView.isInitialized) {
                    lastKnownGridX = gridView.getPlayerX()
                    lastKnownGridY = gridView.getPlayerY()
                }
                if (isNorth) {
                    val intent = Intent(this, ExplorationActivity::class.java).apply {
                        putExtra("REGIAO_ID", "sao_paulo_norte")
                    }
                    startActivity(intent)
                }
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
            val player = PlayerManager.player
            val role = SocialProfileRepository.currentProfile?.role ?: "usuario"
            val isIsento = isUsuarioIsentoTarifaFerroviaria()

            val greetMsg = if (isIsento) {
                "Antônio: Olá! Como ${traduzirRoleCargo(role)}, você possui isenção total de taxas e passagens. Pode embarcar diretamente no trem para o Rio de Janeiro!"
            } else if (!player.habilitadoFerrovia) {
                estadoDialogoAntonio = "PERGUNTOU_HABILITACAO"
                "Antônio: Olá! Para utilizar o serviço ferroviário, é necessária uma taxa administrativa única de habilitação de 30.000 Frons (paga apenas uma vez). Deseja pagar a taxa de habilitação agora?"
            } else {
                estadoDialogoAntonio = "PERGUNTOU_COMPRA"
                "Antônio: Olá! Sua habilitação ferroviária está ativa. Deseja comprar uma passagem para o Rio de Janeiro por 2.000 Frons?"
            }

            adicionarMensagem(greetMsg, isNpc = true)

            val npcPos = gridView.getNpcPosition("NPC Antônio")
            if (npcPos != null) {
                val cleanMsg = greetMsg.removePrefix("Antônio: ")
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
    }

    private fun abrirChatCarlos() {
        if (!conversations.containsKey("carlos")) {
            conversations["carlos"] = ChatConversation("carlos", "Carlos", isNpc = true)
        }
        
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.registerOrRenewInteraction("npc_vendedor_carlos", currentUserId)

        selecionarConversa("carlos")
        
        if (conversations["carlos"]?.history?.isEmpty() == true) {
            val player = PlayerManager.player
            val role = SocialProfileRepository.currentProfile?.role ?: "usuario"
            val isIsento = isUsuarioIsentoTarifaFerroviaria()

            val greetMsg = if (isIsento) {
                "Carlos: Olá! Como ${traduzirRoleCargo(role)}, você possui isenção total de taxas e passagens. Pode embarcar diretamente no trem para São Paulo!"
            } else if (!player.habilitadoFerrovia) {
                estadoDialogoCarlos = "PERGUNTOU_HABILITACAO"
                "Carlos: Olá! Para utilizar o serviço ferroviário, é necessária uma taxa administrativa única de habilitação de 30.000 Frons (paga apenas uma vez). Deseja pagar a taxa de habilitação agora?"
            } else {
                estadoDialogoCarlos = "PERGUNTOU_COMPRA"
                "Carlos: Olá! Sua habilitação ferroviária está ativa. Deseja comprar uma passagem para São Paulo por 2.000 Frons?"
            }

            adicionarMensagem(greetMsg, isNpc = true)

            val npcPos = gridView.getNpcPosition("NPC Carlos")
            if (npcPos != null) {
                val cleanMsg = greetMsg.removePrefix("Carlos: ")
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
    }

    private fun abrirChatVinicius() {
        if (!conversations.containsKey("vinicius")) {
            conversations["vinicius"] = ChatConversation("vinicius", "Vinícius", isNpc = true)
        }
        
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.registerOrRenewInteraction("npc_gerente_vinicius", currentUserId)

        selecionarConversa("vinicius")
        
        if (conversations["vinicius"]?.history?.isEmpty() == true) {
            val greetMsg = "Vinícius: Olá! Sou o gerente da agência bancária da Estação São Paulo. Os serviços bancários estarão disponíveis em breve!"

            adicionarMensagem(greetMsg, isNpc = true)

            val npcPos = gridView.getNpcPosition("NPC Vinícius")
            if (npcPos != null) {
                val cleanMsg = greetMsg.removePrefix("Vinícius: ")
                gridView.addSpeechDialog(
                    senderId = "npc_vinicius",
                    senderName = "NPC Vinícius",
                    message = cleanMsg,
                    worldX = npcPos.x,
                    worldY = npcPos.y,
                    isPrivate = true,
                    allowedUsers = setOf(currentUserId, "vinicius")
                )
            }
        }
    }

    private fun processarDialogoVinicius(msg: String) {
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.registerOrRenewInteraction("npc_gerente_vinicius", currentUserId)

        val responseText = "Vinícius: Olá! Sou o gerente bancário da Estação São Paulo. O sistema bancário estará disponível em breve!"
        adicionarMensagem(responseText, isNpc = true)

        val npcPos = gridView.getNpcPosition("NPC Vinícius")
        if (npcPos != null) {
            val cleanMsg = responseText.removePrefix("Vinícius: ")
            gridView.addSpeechDialog(
                senderId = "npc_vinicius",
                senderName = "NPC Vinícius",
                message = cleanMsg,
                worldX = npcPos.x,
                worldY = npcPos.y,
                isPrivate = true,
                allowedUsers = setOf(currentUserId, "vinicius")
            )
        }
    }

    private fun abrirChatHenrique() {
        if (!conversations.containsKey("henrique")) {
            conversations["henrique"] = ChatConversation("henrique", "Henrique", isNpc = true)
        }
        
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.registerOrRenewInteraction("npc_gerente_henrique", currentUserId)

        selecionarConversa("henrique")
        
        if (conversations["henrique"]?.history?.isEmpty() == true) {
            val greetMsg = "Henrique: Olá! Sou o gerente da agência bancária da Estação Rio de Janeiro. Os serviços bancários estarão disponíveis em breve!"

            adicionarMensagem(greetMsg, isNpc = true)

            val npcPos = gridView.getNpcPosition("NPC Henrique")
            if (npcPos != null) {
                val cleanMsg = greetMsg.removePrefix("Henrique: ")
                gridView.addSpeechDialog(
                    senderId = "npc_henrique",
                    senderName = "NPC Henrique",
                    message = cleanMsg,
                    worldX = npcPos.x,
                    worldY = npcPos.y,
                    isPrivate = true,
                    allowedUsers = setOf(currentUserId, "henrique")
                )
            }
        }
    }

    private fun processarDialogoHenrique(msg: String) {
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.registerOrRenewInteraction("npc_gerente_henrique", currentUserId)

        val responseText = "Henrique: Olá! Sou o gerente bancário da Estação Rio de Janeiro. O sistema bancário estará disponível em breve!"
        adicionarMensagem(responseText, isNpc = true)

        val npcPos = gridView.getNpcPosition("NPC Henrique")
        if (npcPos != null) {
            val cleanMsg = responseText.removePrefix("Henrique: ")
            gridView.addSpeechDialog(
                senderId = "npc_henrique",
                senderName = "NPC Henrique",
                message = cleanMsg,
                worldX = npcPos.x,
                worldY = npcPos.y,
                isPrivate = true,
                allowedUsers = setOf(currentUserId, "henrique")
            )
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
        val targetArmario = player.getArmarioEstacao(currentStationId)
        val layoutMochila = view.findViewById<LinearLayout>(R.id.layoutMochilaTransfer)
        val layoutArmario = view.findViewById<LinearLayout>(R.id.layoutArmarioTransfer)
        val txtCapacidade = view.findViewById<TextView>(R.id.txtCapacidadeArmario)

        layoutMochila.removeAllViews()
        layoutArmario.removeAllViews()

        val totalArmario = targetArmario.values.sum()
        txtCapacidade.text = "Capacidade: $totalArmario/${player.capacidadeArmario}"

        player.mochila.forEach { (id, qtd) ->
            if (qtd > 0) {
                val itemView = criarItemTransfer(id, qtd, true, view)
                layoutMochila.addView(itemView)
            }
        }

        targetArmario.forEach { (id, qtd) ->
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
            val action = if (isMochila) GameAction.DepositItem(id, currentStationId) else GameAction.WithdrawItem(id, currentStationId)
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

    private fun tentaEnviarMovimento() {
        val channel = stationBroadcastChannel ?: return
        if (channel.status.value != RealtimeChannel.Status.SUBSCRIBED) return
        val payload = pendingPayload ?: return
        val now = System.currentTimeMillis()
        val elapsed = now - lastSendTimestamp

        if (elapsed >= movementIntervalMs) {
            lastSendTimestamp = now
            lastSentPayload = payload
            pendingPayload = null

            lifecycleScope.launch {
                try {
                    channel.broadcast("player_movement", payload)
                    Log.d("StationActivity", "[BROADCAST_TRACE] Envio de movimento realizado: pos=(${payload.gridX},${payload.gridY}), dir=${payload.direction}")
                } catch (e: Exception) {
                    Log.e("StationActivity", "[BROADCAST_TRACE] Erro ao enviar broadcast de movimento: ${e.message}")
                }
            }
        } else {
            if (broadcastThrottleJob?.isActive != true) {
                val delayNeeded = movementIntervalMs - elapsed
                broadcastThrottleJob = lifecycleScope.launch {
                    delay(delayNeeded)
                    tentaEnviarMovimento()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SoundManager.play(this, "aventura")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::gridView.isInitialized) {
            lastKnownGridX = gridView.getPlayerX()
            lastKnownGridY = gridView.getPlayerY()
        }
        stationBroadcastJob?.cancel()
        stationBroadcastChannel?.let { ch ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    SupabaseManager.client.realtime.removeChannel(ch)
                } catch (_: Exception) {}
            }
        }
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: "local_user"
        SharedNpcManager.removeInteraction("npc_vendedor_antonio", currentUserId)
        SharedNpcManager.removeInteraction("npc_vendedor_carlos", currentUserId)
        SharedNpcManager.removeInteraction("npc_gerente_vinicius", currentUserId)
        SharedNpcManager.removeInteraction("npc_gerente_henrique", currentUserId)
        SharedNpcManager.stopNpcSystem()

        // Limpa os dados da estação ao sair
        PresenceManager.updatePresenceData(
            stationId = null,
            gridX = -1,
            gridY = -1,
            direction = "baixo",
            gender = ""
        )
        PresenceManager.stopPresence()
    }
}
