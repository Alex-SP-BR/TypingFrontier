package com.typingfrontier.social

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthorProfile(
    val username: String,
    val level: Int = 1,
    val role: String = "usuario"
)

@Serializable
data class Discussion(
    val id: String? = null,
    val category: String,
    val title: String,
    val content: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("profiles") val authorProfile: AuthorProfile? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("reply_count") val replyCount: Int = 0
) {
    val authorUsername: String get() = authorProfile?.username ?: "Herói Errante"
    val authorLevel: Int get() = authorProfile?.level ?: 1
}

@Serializable
data class DiscussionReply(
    val id: String? = null,
    @SerialName("discussion_id") val discussionId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("profiles") val authorProfile: AuthorProfile? = null,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null
) {
    val authorUsername: String get() = authorProfile?.username ?: "Herói Errante"
    val authorLevel: Int get() = authorProfile?.level ?: 1
}
