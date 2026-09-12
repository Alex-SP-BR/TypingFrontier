package com.typingfrontier.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.PlayerManager
import com.typingfrontier.databinding.ItemAchievementBinding
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.appcompat.content.res.AppCompatResources
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
        
        val desbloqueada = p.conquistasDesbloqueadas.contains(achievement.id)

        // Aplica a insígnia com contorno de silhueta
        val drawableComContorno = ViewUtils.getInsigniaWithSilhouette(holder.itemView.context, achievement.insigniaRes, desbloqueada)
        binding.imgInsignia.setImageDrawable(drawableComContorno)

        if (!desbloqueada) {
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            binding.imgInsignia.colorFilter = ColorMatrixColorFilter(matrix)
            binding.imgInsignia.alpha = 0.5f
        } else {
            binding.imgInsignia.colorFilter = null
            binding.imgInsignia.alpha = 1.0f
        }

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
                if (!desbloqueada) 0.5f else 1.0f,
                applySilhouette = true
            )
        }
        
        // Recompensa texto
        if (achievement.recompensaDinheiro > 0) {
            // Configuração do ícone da moeda com tamanho controlado (20dp) para igualar ao AvatarAdapter
            val coinIcon = androidx.core.content.ContextCompat.getDrawable(binding.root.context, com.typingfrontier.R.drawable.fron_coin)
            val size = (20 * binding.root.context.resources.displayMetrics.density).toInt()
            coinIcon?.setBounds(0, 0, size, size)
            binding.txtRecompensa.setCompoundDrawables(coinIcon, null, null, null)

            binding.txtRecompensa.text = "+${achievement.recompensaDinheiro} Frons"
            binding.txtRecompensa.visibility = View.VISIBLE
        } else {
            binding.txtRecompensa.visibility = View.GONE
        }

        binding.txtAvatarBonus.visibility = if (achievement.avatarAssociadoId != null) View.VISIBLE else View.GONE

        if (desbloqueada) {
            binding.txtStatus.text = "🏆"
            binding.txtNome.setTextColor(android.graphics.Color.WHITE)
        } else {
            binding.txtStatus.text = "🔒"
            binding.txtNome.setTextColor(android.graphics.Color.parseColor("#889099"))
        }
    }

    override fun getItemCount(): Int = conquistas.size
}
