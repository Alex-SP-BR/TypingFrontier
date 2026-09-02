package com.typingfrontier.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.PlayerManager
import com.typingfrontier.databinding.ItemAvatarBinding
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.widget.Toast
import android.app.Activity
import com.typingfrontier.utils.AdManager
import com.typingfrontier.utils.ViewUtils

class AvatarAdapter(
    private var avatares: List<Avatar>,
    private val onAvatarEquipado: () -> Unit
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    class AvatarViewHolder(val binding: ItemAvatarBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val binding = ItemAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AvatarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val avatar = avatares[position]
        val p = PlayerManager.player
        val binding = holder.binding

        binding.txtAvatarNome.text = avatar.nome
        binding.imgAvatar.setImageResource(avatar.imagemRes)

        val isPadrao = avatar.id == "default"
        val desbloqueado = isPadrao || p.avataresDesbloqueados.contains(avatar.id)
        val equipado = (p.avatarEquipadoId == avatar.id) || (isPadrao && p.avatarEquipadoId == null)
        val nivelAlcancado = isPadrao || p.nivel >= avatar.nivelRequisito
        val adsAssistidos = if (isPadrao) 0 else p.avataresProgressoAds[avatar.id] ?: 0

        // Visualização ampliada (Apenas se desbloqueado)
        binding.imgAvatar.setOnClickListener {
            if (desbloqueado) {
                ViewUtils.showZoomDialog(
                    holder.itemView.context,
                    avatar.imagemRes,
                    avatar.nome,
                    if (isPadrao) "Avatar Original" else "Nível ${avatar.nivelRequisito}"
                )
            } else {
                Toast.makeText(holder.itemView.context, "Desbloqueie para ampliar", Toast.LENGTH_SHORT).show()
            }
        }

        // Reset views
        binding.txtRequisito.visibility = View.VISIBLE
        binding.txtAdsProgresso.visibility = View.GONE
        binding.btnAcao.visibility = View.GONE
        binding.txtStatus.visibility = View.GONE
        
        // Efeito Grayscale para requisitos não atingidos
        if (!nivelAlcancado) {
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            binding.imgAvatar.colorFilter = ColorMatrixColorFilter(matrix)
            binding.imgAvatar.alpha = 0.5f
        } else {
            binding.imgAvatar.colorFilter = null
            binding.imgAvatar.alpha = 1.0f
        }

        binding.txtRequisito.text = if (isPadrao) "Sempre disponível" else "NÍVEL ${avatar.nivelRequisito}"
        binding.txtRequisito.setTextColor(if (nivelAlcancado) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#E53935"))

        when {
            equipado -> {
                binding.txtStatus.text = "EQUIPADO"
                binding.txtStatus.visibility = View.VISIBLE
                binding.txtRequisito.visibility = View.GONE
            }
            desbloqueado -> {
                binding.btnAcao.text = "EQUIPAR"
                binding.btnAcao.visibility = View.VISIBLE
                binding.btnAcao.isEnabled = true
                binding.btnAcao.setOnClickListener {
                    if (isPadrao) {
                        p.avatarEquipadoId = null
                        notifyDataSetChanged()
                        onAvatarEquipado()
                    } else if (validarEquipar(avatar, holder.itemView.context)) {
                        p.avatarEquipadoId = avatar.id
                        notifyDataSetChanged()
                        onAvatarEquipado()
                    }
                }
            }
            !nivelAlcancado -> {
                // Bloqueado por nível
                binding.txtAdsProgresso.text = "Alcance o nível ${avatar.nivelRequisito} para desbloquear"
                binding.txtAdsProgresso.visibility = View.VISIBLE
                binding.txtAdsProgresso.setTextColor(android.graphics.Color.GRAY)
                binding.btnAcao.visibility = View.GONE
            }
            else -> {
                // Disponível para desbloqueio por anúncios
                binding.txtAdsProgresso.text = "$adsAssistidos/${avatar.adsNecessarios}"
                binding.txtAdsProgresso.visibility = View.VISIBLE
                binding.txtAdsProgresso.setTextColor(android.graphics.Color.parseColor("#1976D2"))
                
                binding.btnAcao.text = "DESBLOQUEAR"
                binding.btnAcao.visibility = View.VISIBLE
                binding.btnAcao.isEnabled = true
                
                binding.btnAcao.setOnClickListener {
                    processarCliqueAnuncio(avatar, holder)
                }
            }
        }
    }

    private fun processarCliqueAnuncio(avatar: Avatar, holder: AvatarViewHolder) {
        val activity = holder.itemView.context as? Activity ?: return
        val p = PlayerManager.player

        // 1. Validação Inicial (Antes do Anúncio)
        if (avatar.id == "default") return
        
        if (!avatar.sexo.equals(p.sexo, ignoreCase = true)) {
            Toast.makeText(activity, "Este avatar não é compatível com seu personagem.", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (p.nivel < avatar.nivelRequisito) {
            Toast.makeText(activity, "Você precisa atingir o nível ${avatar.nivelRequisito}.", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (p.avataresDesbloqueados.contains(avatar.id)) {
            notifyDataSetChanged()
            return
        }

        val assistidosAntes = p.avataresProgressoAds[avatar.id] ?: 0
        if (assistidosAntes >= avatar.adsNecessarios) {
            // Caso raro onde o progresso está completo mas não marcado como desbloqueado
            p.avataresDesbloqueados.add(avatar.id)
            PlayerManager.save(activity)
            notifyDataSetChanged()
            return
        }

        // 2. Chamar o AdManager para exibir o Rewarded Ad
        AdManager.showRewardedAd(
            activity,
            onRewardEarned = {
                // 3. Callback de Recompensa Oficial - VALIDAR NOVAMENTE (Segurança)
                val pAtual = PlayerManager.player
                
                val podeReceber = avatar.sexo.equals(pAtual.sexo, ignoreCase = true) &&
                        pAtual.nivel >= avatar.nivelRequisito &&
                        !pAtual.avataresDesbloqueados.contains(avatar.id)
                
                if (podeReceber) {
                    val assistidos = pAtual.avataresProgressoAds[avatar.id] ?: 0
                    if (assistidos < avatar.adsNecessarios) {
                        // INCREMENTAR PROGRESSO UMA ÚNICA VEZ
                        val novoValor = assistidos + 1
                        pAtual.avataresProgressoAds[avatar.id] = novoValor
                        
                        // Verificar se desbloqueou completamente
                        if (novoValor >= avatar.adsNecessarios) {
                            pAtual.avataresDesbloqueados.add(avatar.id)
                            Toast.makeText(activity, "🎉 Avatar ${avatar.nome} desbloqueado!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(activity, "Progresso: $novoValor/${avatar.adsNecessarios}", Toast.LENGTH_SHORT).show()
                        }
                        
                        // Persistência imediata
                        PlayerManager.save(activity)
                        
                        // Atualizar interface na Thread Principal
                        activity.runOnUiThread {
                            notifyDataSetChanged()
                        }
                    }
                }
            },
            onAdClosed = {
                // Anúncio fechado. A atualização já ocorre no onRewardEarned se houver sucesso.
            },
            onAdFailed = { mensagemErro ->
                Toast.makeText(activity, mensagemErro, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun validarEquipar(avatar: Avatar, context: android.content.Context): Boolean {
        val p = PlayerManager.player
        
        // 1. Verificar se o avatar pertence ao sexo do jogador
        if (!avatar.sexo.equals(p.sexo, ignoreCase = true)) {
            Toast.makeText(context, "Este avatar não é compatível com seu personagem.", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // 2. Verificar se está desbloqueado
        if (!p.avataresDesbloqueados.contains(avatar.id)) {
            Toast.makeText(context, "Avatar ainda não desbloqueado.", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }

    override fun getItemCount(): Int = avatares.size
    
    fun updateList(newList: List<Avatar>) {
        avatares = newList
        notifyDataSetChanged()
    }
}
