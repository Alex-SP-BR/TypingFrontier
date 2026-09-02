package com.typingfrontier.social

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ModerationSupervisionRepository {

    suspend fun getAdminLogs(): List<AdminLogWithProfile> = withContext(Dispatchers.IO) {
        try {
            // Consulta os logs e faz join com profiles para obter o username do administrador
            SupabaseManager.client.postgrest["admin_logs"]
                .select(columns = Columns.raw("*, profiles(username)")) {
                    order("created_at", Order.DESCENDING)
                    limit(20)
                }.decodeList<AdminLogWithProfile>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProfilesByIds(ids: List<String>): List<SocialProfile> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        try {
            SupabaseManager.client.postgrest["profiles"]
                .select {
                    filter {
                        isIn("id", ids)
                    }
                }.decodeList<SocialProfile>()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@kotlinx.serialization.Serializable
data class AdminLogWithProfile(
    val id: String,
    val admin_id: String,
    val action: String,
    val target_id: String? = null,
    val reason: String? = null,
    val created_at: String,
    val profiles: AdminProfile? = null
)

@kotlinx.serialization.Serializable
data class AdminProfile(
    val username: String
)
