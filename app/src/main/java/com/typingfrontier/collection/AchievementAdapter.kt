package com.typingfrontier.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.PlayerManager
import com.typingfrontier.databinding.ItemAchievementBinding
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import com.typingfrontier.utils.ViewUtils

class AchievementAdapter(
    private val conquistas: List<Achievement>
) : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(val binding: ItemAchievementBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val binding = ItemAchievementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AchievementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = conquistas[position]
        val p = PlayerManager.player
        val binding = holder.binding

        binding.txtNome.text = achievement.nome
        binding.txtDescricao.text = achievement.descricao
        binding.txtRequisito.text = "Objetivo: ${achievement.requisito}"
        binding.imgInsignia.setImageResource(achievement.insigniaRes)

        val desbloqueada = p.conquistasDesbloqueadas.contains(achievement.id)

        // Visualização ampliada (Insignia)
        binding.imgInsignia.setOnClickListener {
            val matrix = ColorMatrix()
            if (!desbloqueada) matrix.setSaturation(0f)
            
            ViewUtils.showZoomDialog(
                holder.itemView.context,
                achievement.insigniaRes,
                achievement.nome,
                achievement.descricao,
                if (!desbloqueada) ColorMatrixColorFilter(matrix) else null,
                if (!desbloqueada) 0.4f else 1.0f
            )
        }
        
        // Recompensa texto
        if (achievement.recompensaDinheiro > 0) {
            binding.txtRecompensa.text = "+${achievement.recompensaDinheiro} Frons"
            binding.txtRecompensa.visibility = View.VISIBLE
        } else {
            binding.txtRecompensa.visibility = View.GONE
        }

        binding.txtAvatarBonus.visibility = if (achievement.avatarAssociadoId != null) View.VISIBLE else View.GONE

        if (desbloqueada) {
            binding.txtStatus.text = "🏆"
            binding.imgInsignia.colorFilter = null
            binding.imgInsignia.alpha = 1.0f
            binding.txtNome.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        } else {
            binding.txtStatus.text = "🔒"
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            binding.imgInsignia.colorFilter = ColorMatrixColorFilter(matrix)
            binding.imgInsignia.alpha = 0.4f
            binding.txtNome.setTextColor(android.graphics.Color.parseColor("#333333"))
        }
    }

    override fun getItemCount(): Int = conquistas.size
}
