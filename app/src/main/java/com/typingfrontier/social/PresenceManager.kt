package com.typingfrontier.social

import android.util.Log
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.track
import io.github.jan.supabase.realtime.presenceDataFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Gerenciador de Presença Online via Supabase Realtime.
 * Mantém o status do jogador ativo enquanto o app estiver em foreground.
 */
object PresenceManager {

    @Serializable
    data class PresencePayload(
        val user_id: String,
        val username: String,
        val role: String,
        val gender: String = "",
        val stationId: String? = null,
        val gridX: Int = -1,
        val gridY: Int = -1,
        val direction: String = "baixo",
        val lastMessage: String = "",
        val lastMessageTime: Long = 0L
    )

    private const val TAG = "PresenceManager"
    private const val CHANNEL_ID = "typing-frontier-presence"
    
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var channel: RealtimeChannel? = null
    
    private val _onlineUsers = MutableStateFlow<List<PresencePayload>>(emptyList())
    val onlineUsers = _onlineUsers.asStateFlow()
    
    /**
     * Indica se a infraestrutura de presença está ativa e conectada.
     */
    fun isPresenceActive(): Boolean = channel != null

    private var isAppInForeground = false
    private var presenceJob: Job? = null
    private var observationJob: Job? = null
    private var statusJob: Job? = null

    /**
     * Inicia a infraestrutura de presença. 
     * Chamado quando a identidade social (sessão + perfil) está pronta.
     */
    fun startPresence() {
        Log.d(TAG, "[PRESENCE_DEBUG] startPresence requested")
        val uid = SocialProfileRepository.getCurrentUserId()

        Log.d(TAG, "[PRESENCE_DEBUG] auth.uid = $uid")
        
        if (uid == null) {
            Log.w(TAG, "Tentativa de iniciar presença sem sessão ativa.")
            return
        }

        isAppInForeground = true

        // Se o canal já existe e os monitores estão ativos, não há necessidade de reiniciar
        if (channel != null && observationJob?.isActive == true && statusJob?.isActive == true) {
            Log.d(TAG, "[PRESENCE_DEBUG] Presence infra already active")
            return
        }

        trackPresence()
    }

    private fun iniciarObservacaoInterna() {
        val currentChannel = channel ?: return
        
        observationJob?.cancel()
        observationJob = scope.launch {
            try {
                currentChannel.presenceDataFlow<PresencePayload>().collect { list ->
                    Log.d(TAG, "[PRESENCE_DEBUG] presenceDataFlow EMISSION: size=${list.size}")
                    list.forEach { 
                        Log.d(TAG, "[PRESENCE_DEBUG] Participant: id=${it.user_id}, user=${it.username}, role=${it.role}")
                    }
                    _onlineUsers.value = list
                    Log.d(TAG, "[PRESENCE_DEBUG] onlineUsers size = ${list.size}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[PRESENCE_DEBUG] presenceDataFlow ERROR: ${e.javaClass.simpleName} - ${e.message}")
                e.cause?.let { Log.e(TAG, "[PRESENCE_DEBUG] Cause: ${it.message}") }
                Log.e(TAG, "Erro na observação de presença: ${e.message}")
            }
        }

        // Observa o status do canal para re-anunciar a presença em caso de reconexão
        statusJob?.cancel()
        statusJob = scope.launch {
            currentChannel.status.collect { status ->
                Log.d(TAG, "[PRESENCE_DEBUG] channel status changed: $status")
                Log.d(TAG, "Status do Canal Realtime: $status")
                if (status == RealtimeChannel.Status.SUBSCRIBED && isAppInForeground) {
                    doTrack()
                }
            }
        }
    }

    /**
     * Atualiza o estado de visibilidade do aplicativo.
     * Integrado via ActivityLifecycleCallbacks no TypingFrontierApp.
     */
    fun updateAppStatus(inForeground: Boolean) {
        if (isAppInForeground == inForeground) return
        isAppInForeground = inForeground
        
        if (inForeground) {
            Log.d(TAG, "[PRESENCE_DEBUG] app foreground")
        } else {
            Log.d(TAG, "[PRESENCE_DEBUG] app background")
        }
        
        Log.d(TAG, "App Status Alterado: Foreground = $inForeground")

        if (inForeground) {
            trackPresence()
        } else {
            untrackPresence()
        }
    }

    private fun trackPresence() {
        Log.d(TAG, "[PRESENCE_DEBUG] trackPresence requested")
        val uid = SocialProfileRepository.getCurrentUserId() ?: return
        
        presenceJob?.cancel()
        presenceJob = scope.launch {
            try {
                // Se o canal não existe ou os observers morreram, recriamos tudo
                if (channel == null || observationJob?.isActive != true || statusJob?.isActive != true) {
                    Log.d(TAG, "[PRESENCE_DEBUG] channel created = $CHANNEL_ID (key=$uid)")
                    Log.d(TAG, "Configurando canal de presença: $CHANNEL_ID")
                    
                    channel = SupabaseManager.client.realtime.channel(CHANNEL_ID) {
                        presence {
                            key = uid
                        }
                    }
                    iniciarObservacaoInterna()
                }
                
                // Aguarda a confirmação de que a subscrição foi aceita pelo servidor antes de prosseguir
                Log.d(TAG, "[PRESENCE_DEBUG] subscribe started")
                channel?.subscribe(blockUntilSubscribed = true)
                Log.d(TAG, "[PRESENCE_DEBUG] subscribe returned (SUBSCRIBED)")
                
                // Anuncia o payload de presença atual assim que a subscrição estiver concluída
                doTrack()
            } catch (e: Exception) {
                Log.e(TAG, "[PRESENCE_DEBUG] subscribe ERROR: ${e.javaClass.simpleName} - ${e.message}")
                Log.e(TAG, "Erro ao assinar canal de presença: ${e.message}")
            }
        }
    }

    private suspend fun doTrack() {
        val currentChannel = channel ?: return
        if (currentChannel.status.value != RealtimeChannel.Status.SUBSCRIBED) {
            Log.d(TAG, "[PRESENCE_DEBUG] doTrack deferred: channel status is ${currentChannel.status.value}")
            return
        }

        Log.d(TAG, "[PRESENCE_DEBUG] doTrack START")
        val uid = SocialProfileRepository.getCurrentUserId() ?: return
        val profile = SocialProfileRepository.currentProfile
        
        try {
            // Se não houver dados específicos de localização (currentPresenceData), 
            // constrói um payload básico com os dados do perfil ou fallbacks.
            val payload = currentPresenceData ?: PresencePayload(
                user_id = uid,
                username = profile?.username ?: com.typingfrontier.PlayerManager.player.nome.ifEmpty { "Viajante" },
                role = profile?.role ?: com.typingfrontier.PlayerManager.player.profissao
            )
            
            Log.d(TAG, "[PRESENCE_DEBUG] tracking: uid=$uid, user=${payload.username}, station=${payload.stationId}")

            currentChannel.track(payload)
            Log.d(TAG, "[PRESENCE_DEBUG] track SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "[PRESENCE_DEBUG] track ERROR: ${e.javaClass.simpleName} - ${e.message}")
            Log.e(TAG, "Erro ao anunciar presença: ${e.message}")
        }
    }

    private var currentPresenceData: PresencePayload? = null

    /**
     * Atualiza os dados de localização e estado do jogador no sistema de presença.
     */
    fun updatePresenceData(
        stationId: String?,
        gridX: Int,
        gridY: Int,
        direction: String,
        gender: String,
        lastMessage: String = "",
        lastMessageTime: Long = 0L
    ) {
        val uid = SocialProfileRepository.getCurrentUserId() ?: return
        val profile = SocialProfileRepository.currentProfile
        
        currentPresenceData = PresencePayload(
            user_id = uid,
            username = profile?.username ?: com.typingfrontier.PlayerManager.player.nome.ifEmpty { "Viajante" },
            role = profile?.role ?: com.typingfrontier.PlayerManager.player.profissao,
            gender = gender,
            stationId = stationId,
            gridX = gridX,
            gridY = gridY,
            direction = direction,
            lastMessage = lastMessage,
            lastMessageTime = lastMessageTime
        )
        
        Log.d(TAG, "[PRESENCE_DEBUG] updatePresenceData: station=$stationId, pos=($gridX,$gridY)")
        
        // Assegura que o canal de presença está iniciado
        startPresence()

        val currentChannel = channel
        if (currentChannel != null && currentChannel.status.value == RealtimeChannel.Status.SUBSCRIBED) {
            scope.launch { doTrack() }
        } else {
            Log.d(TAG, "[PRESENCE_DEBUG] updatePresenceData deferred: channel status is ${currentChannel?.status?.value}, payload saved and will track on SUBSCRIBED")
        }
    }

    private fun untrackPresence() {
        Log.d(TAG, "[PRESENCE_DEBUG] stopPresence / untrackPresence requested")
        presenceJob?.cancel()
        observationJob?.cancel()
        statusJob?.cancel()
        
        val currentChannel = channel
        channel = null // Definir IMEDIATAMENTE como null para evitar estado zumbi (Fix Race Condition)
        
        _onlineUsers.value = emptyList()
        scope.launch {
            try {
                currentChannel?.let {
                    SupabaseManager.client.realtime.removeChannel(it)
                    Log.d(TAG, "Presença removida via removeChannel (Background)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao remover presença: ${e.message}")
            }
        }
    }

    /**
     * Encerra a conexão de presença.
     */
    fun stopPresence() {
        untrackPresence()
    }
}
