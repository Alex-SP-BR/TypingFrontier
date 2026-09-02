package com.typingfrontier.social

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repositório responsável pelas operações do Fórum no Supabase.
 */
object DiscussionRepository {

    suspend fun getDiscussions(category: String): List<Discussion> = withContext(Dispatchers.IO) {
        SupabaseManager.client.postgrest["discussions"]
            .select(columns = Columns.raw("*, profiles(username, level, role)")) {
                filter {
                    eq("category", category)
                }
                order("created_at", Order.DESCENDING)
                limit(50)
            }.decodeList<Discussion>()
    }

    suspend fun createDiscussion(discussion: Discussion) = withContext(Dispatchers.IO) {
        val data = buildJsonObject {
            put("category", discussion.category)
            put("title", discussion.title)
            put("content", discussion.content)
            put("author_id", discussion.authorId)
        }
        SupabaseManager.client.postgrest["discussions"].insert(data)
    }

    suspend fun updateDiscussion(discussionId: String, title: String, content: String) = withContext(Dispatchers.IO) {
        val updates = buildJsonObject {
            put("title", title)
            put("content", content)
            // updated_at será atualizado no banco via trigger ou deixado como está se não houver trigger automático
        }
        SupabaseManager.client.postgrest["discussions"].update(updates) {
            filter { eq("id", discussionId) }
        }
    }

    suspend fun deleteDiscussion(discussionId: String) = withContext(Dispatchers.IO) {
        val params = buildJsonObject {
            put("p_discussion_id", discussionId)
        }
        SupabaseManager.client.postgrest.rpc("author_delete_discussion", params)
    }

    suspend fun getReplies(discussionId: String): List<DiscussionReply> = withContext(Dispatchers.IO) {
        SupabaseManager.client.postgrest["discussion_replies"]
            .select(columns = Columns.raw("*, profiles(username, level, role)")) {
                filter {
                    eq("discussion_id", discussionId)
                }
                order("created_at", Order.ASCENDING)
            }.decodeList<DiscussionReply>()
    }

    suspend fun createReply(reply: DiscussionReply) = withContext(Dispatchers.IO) {
        val data = buildJsonObject {
            put("discussion_id", reply.discussionId)
            put("author_id", reply.authorId)
            put("content", reply.content)
        }
        SupabaseManager.client.postgrest["discussion_replies"].insert(data)
    }

    suspend fun updateReply(replyId: String, content: String) = withContext(Dispatchers.IO) {
        val updates = buildJsonObject {
            put("content", content)
        }
        SupabaseManager.client.postgrest["discussion_replies"].update(updates) {
            filter { eq("id", replyId) }
        }
    }

    suspend fun deleteReply(replyId: String) = withContext(Dispatchers.IO) {
        val params = buildJsonObject {
            put("p_reply_id", replyId)
        }
        SupabaseManager.client.postgrest.rpc("author_delete_reply", params)
    }
}
