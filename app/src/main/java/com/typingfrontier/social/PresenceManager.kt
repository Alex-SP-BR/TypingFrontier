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
        val role: String
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
        val profile = SocialProfileRepository.currentProfile
        val uid = SocialProfileRepository.getCurrentUserId()

        Log.d(TAG, "[PRESENCE_DEBUG] auth.uid = $uid")
        if (profile != null) {
            Log.d(TAG, "[PRESENCE_DEBUG] currentProfile.id = ${profile.id}")
            Log.d(TAG, "[PRESENCE_DEBUG] currentProfile.username = ${profile.username}")
            Log.d(TAG, "[PRESENCE_DEBUG] currentProfile.role = ${profile.role}")
            Log.d(TAG, "[PRESENCE_DEBUG] auth.uid == profile.id = ${uid == profile.id}")
        } else {
            Log.d(TAG, "[PRESENCE_DEBUG] currentProfile is NULL")
        }

        if (profile == null || uid == null) {
            Log.w(TAG, "Tentativa de iniciar presença sem perfil ou sessão ativa.")
            return
        }

        // Se o canal já existe e os monitores estão ativos, não há necessidade de reiniciar
        if (channel != null && observationJob?.isActive == true && statusJob?.isActive == true) {
            return
        }

        // Se o app já estiver em foreground, trackPresence cuidará da inicialização e inscrição.
        if (isAppInForeground) {
            trackPresence()
        }
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
        val profile = SocialProfileRepository.currentProfile ?: return
        
        presenceJob?.cancel()
        presenceJob = scope.launch {
            try {
                // Se o canal não existe ou os observers morreram, recriamos tudo (Causa da Falha identificada na auditoria)
                if (channel == null || observationJob?.isActive != true || statusJob?.isActive != true) {
                    Log.d(TAG, "[PRESENCE_DEBUG] channel created = $CHANNEL_ID")
                    Log.d(TAG, "Configurando canal de presença: $CHANNEL_ID")
                    
                    channel = SupabaseManager.client.realtime.channel(CHANNEL_ID) {
                        presence {
                            key = profile.id
                        }
                    }
                    iniciarObservacaoInterna()
                }
                
                // Aguarda a confirmação de que a subscrição foi aceita pelo servidor antes de prosseguir
                Log.d(TAG, "[PRESENCE_DEBUG] subscribe started")
                channel?.subscribe(blockUntilSubscribed = true)
                Log.d(TAG, "[PRESENCE_DEBUG] subscribe returned")
                
                // O anúncio real (track) será disparado pelo statusFlow quando o estado for SUBSCRIBED.
            } catch (e: Exception) {
                Log.e(TAG, "[PRESENCE_DEBUG] subscribe ERROR: ${e.javaClass.simpleName} - ${e.message}")
                Log.e(TAG, "Erro ao assinar canal de presença: ${e.message}")
            }
        }
    }

    private suspend fun doTrack() {
        Log.d(TAG, "[PRESENCE_DEBUG] doTrack START")
        val profile = SocialProfileRepository.currentProfile ?: return
        val currentChannel = channel ?: return
        try {
            val payload = PresencePayload(
                user_id = profile.id,
                username = profile.username,
                role = profile.role
            )
            
            Log.d(TAG, "[PRESENCE_DEBUG] track user_id = ${payload.user_id}")
            Log.d(TAG, "[PRESENCE_DEBUG] track username = ${payload.username}")
            Log.d(TAG, "[PRESENCE_DEBUG] track role = ${payload.role}")

            currentChannel.track(payload)
            Log.d(TAG, "[PRESENCE_DEBUG] track SUCCESS")
            Log.d(TAG, "Presença anunciada (Online): @${profile.username}")
        } catch (e: Exception) {
            Log.e(TAG, "[PRESENCE_DEBUG] track ERROR: ${e.javaClass.simpleName} - ${e.message}")
            e.cause?.let { Log.e(TAG, "[PRESENCE_DEBUG] Cause: ${it.message}") }
            Log.e(TAG, "Erro ao anunciar presença: ${e.message}")
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
