package com.typingfrontier.social

import kotlinx.serialization.Serializable

@Serializable
data class SocialProfile(
    val id: String,
    val username: String,
    val character_name: String? = null,
    val role: String = "usuario",
    val avatar_equipped_id: String? = null,
    val insignia_equipped_id: String? = null,
    
    // Estatísticas para Ranking
    val level: Int = 1,
    val strength: Int = 0,
    val resistance: Int = 0,
    val speed: Int = 0,
    val intelligence: Int = 0,
    val charisma: Int = 0,
    val adventures_completed: Int = 0,
    val best_streak: Int = 0,
    val insignias_count: Int = 0,
    val avatars_count: Int = 0
)
