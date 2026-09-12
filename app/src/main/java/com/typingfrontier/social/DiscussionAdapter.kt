package com.typingfrontier.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.databinding.ItemDiscussionBinding

class DiscussionAdapter(
    private var discussions: List<Discussion>,
    private val category: String = "general",
    private val onItemClick: (Discussion) -> Unit,
    private val onItemLongClick: (Discussion) -> Unit,
    private val onAuthorClick: (String) -> Unit
) : RecyclerView.Adapter<DiscussionAdapter.DiscussionViewHolder>() {

    class DiscussionViewHolder(val binding: ItemDiscussionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscussionViewHolder {
        val binding = ItemDiscussionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiscussionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiscussionViewHolder, position: Int) {
        val discussion = discussions[position]
        val binding = holder.binding

        if (category == "general") {
            // Modo Fórum Tradicional
            binding.txtDiscussionTitle.text = discussion.title
            binding.txtDiscussionContent.text = if (discussion.content.length > 120) discussion.content.take(117) + "..." else discussion.content
            
            binding.txtAuthorName.text = "@${discussion.authorUsername}"
            binding.txtDiscussionMeta.text = " · Nível ${discussion.authorLevel} · ${discussion.createdAt?.take(10) ?: ""}"
            
            // Clique no autor
            binding.txtAuthorName.setOnClickListener { onAuthorClick(discussion.authorId) }
        } else {
            // Modo Mural Social
            binding.txtAuthorName.text = "@${discussion.authorUsername}"
            binding.txtDiscussionTitle.text = " · Nível ${discussion.authorLevel}"
            binding.txtDiscussionContent.text = discussion.content
            binding.txtDiscussionMeta.text = discussion.createdAt?.take(10) ?: ""
            
            // Clique no autor
            binding.txtAuthorName.setOnClickListener { onAuthorClick(discussion.authorId) }
        }

        binding.txtReplyCount.text = "💬 ${discussion.replyCount} respostas"

        binding.btnMenuOptions.setOnClickListener {
            onItemLongClick(discussion)
        }

        holder.itemView.setOnClickListener {
            onItemClick(discussion)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(discussion)
            true
        }
    }

    override fun getItemCount(): Int = discussions.size

    fun updateData(newList: List<Discussion>) {
        discussions = newList
        notifyDataSetChanged()
    }
}
