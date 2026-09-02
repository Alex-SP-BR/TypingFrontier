package com.typingfrontier.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.databinding.ItemDiscussionReplyBinding

class DiscussionReplyAdapter(
    private var replies: List<DiscussionReply>,
    private val onItemLongClick: (DiscussionReply) -> Unit,
    private val onAuthorClick: (String) -> Unit
) : RecyclerView.Adapter<DiscussionReplyAdapter.ReplyViewHolder>() {

    class RankingViewHolder(val binding: ItemDiscussionReplyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val binding = ItemDiscussionReplyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReplyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        val reply = replies[position]
        val binding = holder.binding

        binding.txtReplyAuthor.text = "@${reply.authorUsername} · Nível ${reply.authorLevel}"
        binding.txtReplyContent.text = reply.content
        binding.txtReplyDate.text = reply.createdAt?.take(10) ?: ""

        binding.txtReplyAuthor.setOnClickListener { onAuthorClick(reply.authorId) }

        binding.btnReplyMenu.setOnClickListener {
            onItemLongClick(reply)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(reply)
            true
        }
    }

    override fun getItemCount(): Int = replies.size

    fun updateData(newList: List<DiscussionReply>) {
        replies = newList
        notifyDataSetChanged()
    }

    // Fixed inner class name to match ReplyViewHolder
    class ReplyViewHolder(val binding: ItemDiscussionReplyBinding) : RecyclerView.ViewHolder(binding.root)
}
