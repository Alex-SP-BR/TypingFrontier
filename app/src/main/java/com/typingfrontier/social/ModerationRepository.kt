package com.typingfrontier.social

import android.util.Log
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositório responsável por ações de moderação e denúncias no Supabase.
 */
object ModerationRepository {

    suspend fun createReport(targetType: String, targetId: String, reason: String, description: String? = null) = withContext(Dispatchers.IO) {
        val profile = SocialProfileRepository.currentProfile ?: throw Exception("Identidade social não carregada.")
        
        val report = buildJsonObject {
            put("reporter_id", profile.id)
            put("target_type", targetType)
            put("target_id", targetId)
            put("reason", reason)
            put("description", description)
        }

        try {
            SupabaseManager.client.postgrest["reports"].insert(report)
        } catch (e: Exception) {
            logReportError(targetType, targetId, e)
            throw e
        }
    }

    private fun logReportError(targetType: String, targetId: String, e: Exception) {
        Log.e("PrivateReportDebug", "=== ERRO NA CRIAÇÃO DE DENÚNCIA ===")
        Log.e("PrivateReportDebug", "targetType: $targetType, targetId: $targetId")
        Log.e("PrivateReportDebug", "Classe da Exceção: ${e.javaClass.name}")
        Log.e("PrivateReportDebug", "Mensagem da Exceção: ${e.message}")
        if (e is RestException) {
            Log.e("PrivateReportDebug", "RestException - statusCode: ${e.statusCode}")
            Log.e("PrivateReportDebug", "RestException - error: ${e.error}")
            Log.e("PrivateReportDebug", "RestException - description: ${e.description}")
        }
        Log.e("PrivateReportDebug", "Stack Trace Completa:", e)
        Log.e("PrivateReportDebug", "====================================")
    }

    suspend fun getReports(): List<Report> = withContext(Dispatchers.IO) {
        SupabaseManager.client.postgrest["reports"]
            .select {
                order("created_at", Order.DESCENDING)
                limit(100) // Aumentado para dar mais base para busca de autores
            }.decodeList<Report>()
    }

    suspend fun getReportedAuthors(query: String): List<SocialProfile> = withContext(Dispatchers.IO) {
        try {
            // 1. Buscar denúncias (limitadas para performance)
            val reports = getReports()
            val authorIds = mutableSetOf<String>()

            // 2. Resolver os IDs dos autores do conteúdo denunciado
            reports.forEach { report ->
                val authorId = getContentAuthorId(report.target_type, report.target_id, report.id)
                if (authorId != null) authorIds.add(authorId)
            }

            if (authorIds.isEmpty()) return@withContext emptyList()

            // 3. Buscar os perfis desses autores que batem com a busca (username)
            val normalizedQuery = query.lowercase().trim().removePrefix("@")
            
            SupabaseManager.client.postgrest["profiles"]
                .select {
                    filter {
                        and {
                            isIn("id", authorIds.toList())
                            ilike("username", "%$normalizedQuery%")
                        }
                    }
                }.decodeList<SocialProfile>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProfileByUsername(username: String): SocialProfile? = withContext(Dispatchers.IO) {
        try {
            SupabaseManager.client.postgrest["profiles"]
                .select {
                    filter { eq("username", username.lowercase().trim().removePrefix("@")) }
                }.decodeSingleOrNull<SocialProfile>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProfileById(userId: String): SocialProfile? = withContext(Dispatchers.IO) {
        try {
            SupabaseManager.client.postgrest["profiles"]
                .select {
                    filter { eq("id", userId) }
                }.decodeSingleOrNull<SocialProfile>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getReportedPrivateMessage(reportId: String): ReportedPrivateMessage? = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_report_id", reportId)
            }
            SupabaseManager.client.postgrest.rpc("get_reported_private_message_with_context", params)
                .decodeList<ReportedPrivateMessage>()
                .find { it.messageType == "target" } ?: SupabaseManager.client.postgrest.rpc("get_reported_private_message_with_context", params)
                .decodeList<ReportedPrivateMessage>()
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getReportedPrivateMessageContext(reportId: String): List<ReportedPrivateMessage> = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_report_id", reportId)
            }
            SupabaseManager.client.postgrest.rpc("get_reported_private_message_with_context", params)
                .decodeList<ReportedPrivateMessage>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getReportedPublicMessage(reportId: String): ReportedPublicMessage? = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_report_id", reportId)
            }
            SupabaseManager.client.postgrest.rpc("get_reported_public_message_with_context", params)
                .decodeList<ReportedPublicMessage>()
                .find { it.messageType == "target" } ?: SupabaseManager.client.postgrest.rpc("get_reported_public_message_with_context", params)
                .decodeList<ReportedPublicMessage>()
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getReportedPublicMessageContext(reportId: String): List<ReportedPublicMessage> = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_report_id", reportId)
            }
            SupabaseManager.client.postgrest.rpc("get_reported_public_message_with_context", params)
                .decodeList<ReportedPublicMessage>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getContentAuthorId(targetType: String, targetId: String, reportId: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            if (targetType == "private_message") {
                if (reportId == null) return@withContext null
                val reportedMsg = getReportedPrivateMessage(reportId)
                return@withContext reportedMsg?.senderId
            }
            if (targetType == "public_message") {
                if (reportId == null) return@withContext null
                val reportedMsg = getReportedPublicMessage(reportId)
                return@withContext reportedMsg?.senderId
            }
            val table = if (targetType == "discussion") "discussions" else "discussion_replies"
            val result = SupabaseManager.client.postgrest[table]
                .select(Columns.raw("author_id")) {
                    filter { eq("id", targetId) }
                }.decodeSingleOrNull<ContentAuthorId>()
            result?.authorId
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getContentDetails(targetType: String, targetId: String, reportId: String? = null): ContentDetails? = withContext(Dispatchers.IO) {
        try {
            if (targetType == "private_message") {
                if (reportId == null) return@withContext null
                val messages = getReportedPrivateMessageContext(reportId)
                if (messages.isEmpty()) return@withContext null

                val sb = StringBuilder()
                messages.forEach { msg ->
                    val senderProfile = getProfileById(msg.senderId)
                    val username = senderProfile?.username ?: "Usuário"
                    val time = msg.createdAt.take(16).replace("T", " ")

                    when (msg.messageType) {
                        "prior" -> {
                            sb.append("[Mensagem anterior]\n")
                            sb.append("@$username ($time):\n\"${msg.message}\"\n\n")
                        }
                        "target" -> {
                            sb.append("━━━━━━━━━━━━━━━━\n")
                            sb.append("MENSAGEM DENUNCIADA\n")
                            sb.append("@$username ($time):\n\"${msg.message}\"\n")
                            sb.append("━━━━━━━━━━━━━━━━")
                        }
                        "posterior" -> {
                            sb.append("\n\n[Mensagem posterior]\n")
                            sb.append("@$username ($time):\n\"${msg.message}\"")
                        }
                    }
                }
                return@withContext ContentDetails(content = sb.toString().trim(), title = "MENSAGEM PRIVADA")
            }
            if (targetType == "public_message") {
                if (reportId == null) return@withContext null
                val messages = getReportedPublicMessageContext(reportId)
                if (messages.isEmpty()) return@withContext null

                val sb = StringBuilder()
                messages.forEach { msg ->
                    val senderProfile = getProfileById(msg.senderId)
                    val username = senderProfile?.username ?: "Usuário"
                    val time = msg.createdAt.take(16).replace("T", " ")

                    when (msg.messageType) {
                        "prior" -> {
                            sb.append("[Mensagem anterior]\n")
                            sb.append("@$username ($time):\n\"${msg.message}\"\n\n")
                        }
                        "target" -> {
                            sb.append("━━━━━━━━━━━━━━━━\n")
                            sb.append("MENSAGEM DENUNCIADA (ESTAÇÃO: ${msg.stationId.uppercase()})\n")
                            sb.append("@$username ($time):\n\"${msg.message}\"\n")
                            sb.append("━━━━━━━━━━━━━━━━")
                        }
                        "posterior" -> {
                            sb.append("\n\n[Mensagem posterior]\n")
                            sb.append("@$username ($time):\n\"${msg.message}\"")
                        }
                    }
                }
                return@withContext ContentDetails(content = sb.toString().trim(), title = "MENSAGEM PÚBLICA DA ESTAÇÃO")
            }
            val table = if (targetType == "discussion") "discussions" else "discussion_replies"
            SupabaseManager.client.postgrest[table]
                .select {
                    filter { eq("id", targetId) }
                }.decodeSingleOrNull<ContentDetails>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun resolveReport(reportId: String, decision: String, deleteContent: Boolean) = withContext(Dispatchers.IO) {
        val params = buildJsonObject {
            put("p_report_id", reportId)
            put("p_decision", decision)
            put("p_delete_content", deleteContent)
        }

        SupabaseManager.client.postgrest.rpc("moderate_resolve_report", params)
    }

    suspend fun isCurrentUserBanned(): Boolean = withContext(Dispatchers.IO) {
        val profile = SocialProfileRepository.currentProfile ?: return@withContext false
        isUserBanned(profile.id)
    }

    suspend fun isUserBanned(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_user_id", userId)
            }
            val response = SupabaseManager.client.postgrest.rpc("is_user_banned_rpc", params)
            // O retorno do PostgREST para boolean via RPC costuma vir no corpo da resposta
            response.data.toString().trim().lowercase() == "true"
        } catch (e: Exception) {
            // Em caso de erro (ex: rede), por segurança social, assumimos não banido 
            // mas logs de rede podem ajudar a diagnosticar falhas de moderação.
            false
        }
    }

    suspend fun getBanStatus(userId: String): UserBan? = withContext(Dispatchers.IO) {
        try {
            SupabaseManager.client.postgrest["user_bans"]
                .select {
                    filter { eq("user_id", userId) }
                }.decodeSingleOrNull<UserBan>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun banUser(userId: String, reason: String, durationHours: Int?) = withContext(Dispatchers.IO) {
        val params = buildJsonObject {
            put("p_user_id", userId)
            put("p_reason", reason)
            if (durationHours != null) {
                put("p_duration_hours", durationHours)
            } else {
                put("p_duration_hours", null as Int?)
            }
        }
        SupabaseManager.client.postgrest.rpc("admin_ban_user", params)
    }

    suspend fun unbanUser(userId: String) = withContext(Dispatchers.IO) {
        val params = buildJsonObject {
            put("p_user_id", userId)
        }
        SupabaseManager.client.postgrest.rpc("admin_unban_user", params)
    }

    suspend fun updateUserRole(targetUserId: String, newRole: String) = withContext(Dispatchers.IO) {
        val params = buildJsonObject {
            put("p_target_user_id", targetUserId)
            put("p_new_role", newRole)
        }
        SupabaseManager.client.postgrest.rpc("admin_update_user_role", params)
    }

    suspend fun claimReport(reportId: String) = withContext(Dispatchers.IO) {
        val params = buildJsonObject {
            put("p_report_id", reportId)
        }
        SupabaseManager.client.postgrest.rpc("moderate_claim_report", params)
    }

    suspend fun getReportByTarget(targetType: String, targetId: String): Report? = withContext(Dispatchers.IO) {
        try {
            // Busca o report mais antigo (created_at ASC) para este alvo
            SupabaseManager.client.postgrest["reports"]
                .select {
                    filter {
                        eq("target_type", targetType)
                        eq("target_id", targetId)
                    }
                    order("created_at", Order.ASCENDING)
                    limit(1)
                }.decodeSingleOrNull<Report>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getReportById(reportId: String): Report? = withContext(Dispatchers.IO) {
        try {
            SupabaseManager.client.postgrest["reports"]
                .select {
                    filter { eq("id", reportId) }
                }.decodeSingleOrNull<Report>()
        } catch (e: Exception) {
            null
        }
    }
}

@kotlinx.serialization.Serializable
data class Report(
    val id: String,
    val reporter_id: String,
    val target_type: String,
    val target_id: String,
    val reason: String,
    val description: String? = null,
    val status: String,
    val moderator_id: String? = null,
    val decision: String? = null,
    val created_at: String,
    
    // Campos de UI (não persistidos na tabela reports, inicializados com default para evitar erro de serialização)
    var reporterUsername: String? = null,
    var targetAuthorUsername: String? = null,
    var targetAuthorRole: String? = null,
    var targetAuthorId: String? = null,
    var targetAuthorLevel: Int? = null,
    var targetContentText: String? = null,
    var targetTitle: String? = null,
    var targetCategory: String? = null
)

@Serializable
data class ReportedPrivateMessage(
    @SerialName("message_id") val messageId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val message: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("message_type") val messageType: String = "target"
)

@Serializable
data class ReportedPublicMessage(
    @SerialName("message_id") val messageId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("station_id") val stationId: String,
    val message: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("message_type") val messageType: String = "target"
)

@kotlinx.serialization.Serializable
data class ContentAuthorId(
    @kotlinx.serialization.SerialName("author_id") val authorId: String
)

@kotlinx.serialization.Serializable
data class ContentDetails(
    val content: String,
    val title: String? = null,
    val category: String? = null
)

@kotlinx.serialization.Serializable
data class UserBan(
    val id: String,
    @kotlinx.serialization.SerialName("user_id") val userId: String,
    val reason: String,
    @kotlinx.serialization.SerialName("banned_at") val bannedAt: String,
    @kotlinx.serialization.SerialName("expires_at") val expiresAt: String? = null
)

@kotlinx.serialization.Serializable
data class AdminLog(
    val id: String,
    val admin_id: String,
    val action: String,
    val target_id: String? = null,
    val reason: String? = null,
    val created_at: String
)
