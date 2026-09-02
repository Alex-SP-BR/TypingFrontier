package com.typingfrontier.social

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.databinding.ItemRankingBinding

class RankingAdapter(
    private var entries: List<SocialProfile>,
    private val currentUserId: String?,
    private val category: RankingCategory,
    private val onProfileClick: (String) -> Unit
) : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {

    class RankingViewHolder(val binding: ItemRankingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val binding = ItemRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RankingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        val profile = entries[position]
        val binding = holder.binding

        val rank = position + 1
        binding.txtPosicao.text = "${rank}º"
        binding.txtUsername.text = profile.username
        binding.txtCharacterName.text = profile.character_name ?: "Herói Errante"

        val valor = when (category) {
            RankingCategory.LEVEL -> profile.level
            RankingCategory.STRENGTH -> profile.strength
            RankingCategory.RESISTANCE -> profile.resistance
            RankingCategory.SPEED -> profile.speed
            RankingCategory.INTELLIGENCE -> profile.intelligence
            RankingCategory.CHARISMA -> profile.charisma
            RankingCategory.EXPLORATION -> profile.adventures_completed
            RankingCategory.MENTAL -> profile.best_streak
        }
        binding.txtValor.text = valor.toString()

        // Destaque para o jogador atual
        if (profile.id == currentUserId) {
            binding.cardContainer.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            binding.txtUsername.setTextColor(Color.parseColor("#1976D2"))
        } else {
            binding.cardContainer.setCardBackgroundColor(Color.WHITE)
            binding.txtUsername.setTextColor(Color.parseColor("#333333"))
        }

        // Destaque para o podium
        when (rank) {
            1 -> binding.txtPosicao.setTextColor(Color.parseColor("#FFD700")) // Ouro
            2 -> binding.txtPosicao.setTextColor(Color.parseColor("#C0C0C0")) // Prata
            3 -> binding.txtPosicao.setTextColor(Color.parseColor("#CD7F32")) // Bronze
            else -> binding.txtPosicao.setTextColor(Color.parseColor("#1976D2"))
        }

        holder.itemView.setOnClickListener {
            onProfileClick(profile.id)
        }
    }

    override fun getItemCount(): Int = entries.size

    fun updateData(newEntries: List<SocialProfile>, newCategory: RankingCategory) {
        entries = newEntries
        // Categoria não muda o adapter mas muda como lemos os dados
        // Para simplificar, o adapter é recriado ou atualizado
        notifyDataSetChanged()
    }
}

enum class RankingCategory(val displayName: String, val columnName: String) {
    LEVEL("Nível", "level"),
    STRENGTH("Força", "strength"),
    RESISTANCE("Resistência", "resistance"),
    SPEED("Velocidade", "speed"),
    INTELLIGENCE("Inteligência", "intelligence"),
    CHARISMA("Carisma", "charisma"),
    EXPLORATION("Exploração", "adventures_completed"),
    MENTAL("Treino Mental", "best_streak")
}
