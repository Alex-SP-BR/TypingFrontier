package com.typingfrontier.social

import android.util.Log
import com.typingfrontier.PlayerManager
import com.typingfrontier.TypingFrontierApp
import com.typingfrontier.collection.CollectionRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.signInAnonymously
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Repositório responsável pela gestão da identidade e perfil social no Supabase.
 * Garante o vínculo entre o jogador local e a nuvem de forma transparente.
 */
object SocialProfileRepository {

    private const val TAG = "SocialProfileRepo"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    /**
     * Referência ao perfil social carregado do Supabase.
     * Utilizado apenas para controle de interface (ex: exibir painel admin).
     * A autorização real de operações sensíveis é feita no servidor.
     */
    var currentProfile: SocialProfile? = null
        private set

    private var initializationJob: Job? = null

    /**
     * Retorna o ID do usuário autenticado na sessão atual do Supabase,
     * independente de possuir um perfil social criado ou não.
     */
    fun getCurrentUserId(): String? {
        return SupabaseManager.client.auth.currentUserOrNull()?.id
    }

    /**
     * Inicializa a identidade social do jogador.
     * Realiza login anônimo e garante a existência do perfil na tabela 'profiles'.
     */
    fun initializeSocialIdentity(forceRefresh: Boolean = false, onComplete: (() -> Unit)? = null) {
        if (initializationJob?.isActive == true && !forceRefresh) return

        initializationJob = scope.launch {
            mutex.withLock {
                try {
                    val auth = SupabaseManager.client.auth
                    
                    // 1. Aguarda a restauração da sessão persistente se ainda estiver carregando
                    if (auth.sessionStatus.value is SessionStatus.Initializing) {
                        Log.d(TAG, "Aguardando restauração da sessão...")
                        auth.sessionStatus.filter { it !is SessionStatus.Initializing }.first()
                    }

                    // 2. Autenticação: Garante que o jogador tenha uma identidade única no Supabase
                    if (auth.currentUserOrNull() == null) {
                        Log.d(TAG, "Nenhuma sessão ativa. Tentando login anônimo...")
                        try {
                            auth.signInAnonymously()
                        } catch (e: Exception) {
                            Log.e(TAG, "Erro no signInAnonymously: ${e.message}")
                        }
                    }

                    val user = auth.currentUserOrNull()
                    if (user != null) {
                        val uid = user.id
                        Log.d(TAG, "ID Social identificado: $uid")
                        
                        // 3. Perfil: Verifica se o registro social já existe para este UUID
                        if (currentProfile == null || forceRefresh) {
                            val profile = try {
                                SupabaseManager.client.postgrest["profiles"]
                                    .select {
                                        filter {
                                            eq("id", uid)
                                        }
                                    }.decodeSingleOrNull<SocialProfile>()
                            } catch (e: Exception) {
                                Log.e(TAG, "Erro ao buscar perfil: ${e.message}")
                                null
                            }

                            if (profile == null) {
                                Log.d(TAG, "Perfil não encontrado para $uid. Aguardando definição de identidade social.")
                                // Não cria o perfil automaticamente se o nome do jogador estiver vazio (Novo Jogador)
                                // O perfil será criado explicitamente em CreateCharacterActivity via createSocialProfile
                            } else {
                                currentProfile = profile
                                Log.d(TAG, "Perfil social recuperado: ${profile.username} (Role: ${profile.role})")
                                
                                // Validação de Avatar Administrativo (Segurança contra perda de Role)
                                val player = PlayerManager.player
                                if (!CollectionRepository.isAvatarValidoParaPlayer(player.avatarEquipadoId, player)) {
                                    Log.w(TAG, "Avatar equipado inválido para a role atual. Resetando para padrão.")
                                    player.avatarEquipadoId = null
                                    // Sincronização será feita no bloco seguinte se houver mudança
                                }
                            }
                        }

                        // Sincroniza dados mutáveis (nome, avatar equipado) se houver alteração local
                        val current = currentProfile
                        if (current != null) {
                            val player = PlayerManager.player
                            val hasChanged = player.nome != current.character_name ||
                                    player.avatarEquipadoId != current.avatar_equipped_id

                            if (hasChanged && player.nome.isNotBlank()) {
                                try {
                                    SupabaseManager.client.postgrest["profiles"].update({
                                        set("character_name", player.nome)
                                        set("avatar_equipped_id", player.avatarEquipadoId)
                                    }) {
                                        filter { eq("id", current.id) }
                                    }
                                    // Atualiza o objeto local também
                                    currentProfile = current.copy(
                                        character_name = player.nome,
                                        avatar_equipped_id = player.avatarEquipadoId
                                    )
                                    Log.d(TAG, "Perfil social (nome/avatar) sincronizado local e remoto.")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Erro ao atualizar dados mutáveis do perfil: ${e.message}")
                                }
                            }
                        }

                        onComplete?.invoke()
                    }
                } catch (e: Exception) {
                    // Falhas de rede ou backend não impedem o gameplay (Offline-first)
                    Log.e(TAG, "Falha na sincronização da identidade social: ${e.message}")
                }
            }
        }
    }

    suspend fun awaitInitialization() {
        initializationJob?.join()
    }

    /**
     * Verifica se um username está disponível no Supabase.
     */
    suspend fun isUsernameAvailable(username: String): Boolean = withContext(Dispatchers.IO) {
        val result = SupabaseManager.client.postgrest["profiles"]
            .select {
                filter {
                    eq("username", username.lowercase().trim())
                }
            }.decodeSingleOrNull<SocialProfile>()
        result == null
    }

    /**
     * Cria o perfil social inicial para um novo jogador.
     */
    suspend fun createSocialProfile(username: String, characterName: String): Boolean = withContext(Dispatchers.IO) {
        val auth = SupabaseManager.client.auth
        val user = auth.currentUserOrNull() ?: return@withContext false
        val uid = user.id
        val player = PlayerManager.player

        val newProfile = SocialProfile(
            id = uid,
            username = username.lowercase().trim(),
            character_name = characterName,
            level = player.nivel,
            strength = player.forca,
            resistance = player.resistencia,
            speed = player.velocidade,
            intelligence = player.inteligencia,
            charisma = player.carisma,
            adventures_completed = player.zonasExploradas.size,
            best_streak = player.mentalStreak
        )

        SupabaseManager.client.postgrest["profiles"].insert(newProfile)
        currentProfile = newProfile
        true
    }

    /**
     * Sincroniza as estatísticas atuais do save local com o Supabase para o ranking.
     * Utiliza a RPC 'sync_social_data' para garantir integridade e segurança.
     */
    fun syncStatistics() {
        scope.launch {
            try {
                val auth = SupabaseManager.client.auth
                if (auth.currentUserOrNull() == null) return@launch
                
                val player = PlayerManager.player

                Log.d(TAG, "Iniciando sincronização de estatísticas e itens para ranking...")

                // Filtra os avatares desbloqueados (Remove o 'default' que é o avatar original)
                val avataresParaSincronizar = player.avataresDesbloqueados.filter { it != "default" }
                val conquistasParaSincronizar = player.conquistasDesbloqueadas.toList()

                // Constrói o objeto JSON para a RPC
                val params = buildJsonObject {
                    put("p_level", player.nivel)
                    put("p_str", player.forca)
                    put("p_res", player.resistencia)
                    put("p_spd", player.velocidade)
                    put("p_int", player.inteligencia)
                    put("p_cha", player.carisma)
                    put("p_adv", player.zonasExploradas.size)
                    put("p_streak", player.mentalStreak)
                    
                    // Sincroniza a lista de itens desbloqueados
                    putJsonArray("p_new_avatars") {
                        avataresParaSincronizar.forEach { add(it) }
                    }
                    putJsonArray("p_new_insignias") {
                        conquistasParaSincronizar.forEach { add(it) }
                    }
                }

                SupabaseManager.client.postgrest.rpc(
                    function = "sync_social_data",
                    parameters = params
                )
                
                Log.d(TAG, "Estatísticas sincronizadas com sucesso (Ranking Social).")
            } catch (e: Exception) {
                // Silencioso para o usuário (Offline-first)
                Log.e(TAG, "Erro na sincronização de ranking: ${e.message}")
            }
        }
    }

    /**
     * Carrega o perfil social de outro jogador a partir do Supabase.
     */
    suspend fun getRemoteProfile(userId: String): SocialProfile? = withContext(Dispatchers.IO) {
        try {
            SupabaseManager.client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }.decodeSingleOrNull<SocialProfile>()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar perfil remoto: ${e.message}")
            null
        }
    }

    /**
     * Carrega a lista de IDs de itens desbloqueados (avatares ou insígnias) de um jogador.
     */
    suspend fun getUnlockedItems(userId: String, itemType: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val result = SupabaseManager.client.postgrest["unlocked_items"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("item_type", itemType)
                    }
                }.decodeList<UnlockedItem>()
            result.map { it.itemId }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar itens desbloqueados: ${e.message}")
            emptyList()
        }
    }
}

@kotlinx.serialization.Serializable
data class UnlockedItem(
    @kotlinx.serialization.SerialName("item_id") val itemId: String,
    @kotlinx.serialization.SerialName("item_type") val itemType: String
)
