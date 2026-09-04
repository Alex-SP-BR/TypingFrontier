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
import com.typingfrontier.utils.CurrencyUtils

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

        val isAdmin = avatar.categoria == CollectionCategory.ADMINISTRATIVO
        val isComercial = avatar.categoria == CollectionCategory.COLECAO
        val isPadrao = avatar.id == "default"

        binding.txtAvatarNome.text = avatar.nome
        binding.imgAvatar.setImageResource(avatar.imagemRes)

        // Enquadramento especial para avatares administrativos e comerciais (Corpo inteiro -> Foco no torso/cabeça)
        if (isAdmin || isComercial) {
            binding.imgAvatar.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        } else {
            binding.imgAvatar.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }
        
        // Regra de Desbloqueio: Padrão, já desbloqueado ou Administrativo com Role válida
        val desbloqueado = isPadrao || 
                          p.avataresDesbloqueados.contains(avatar.id) ||
                          (isAdmin && CollectionRepository.isAvatarValidoParaPlayer(avatar.id, p))

        val equipado = (p.avatarEquipadoId == avatar.id) || (isPadrao && p.avatarEquipadoId == null)
        
        // Requisito de nível: Ignorado para Administrativo, Comercial e Padrão
        val nivelAlcancado = isPadrao || isAdmin || isComercial || p.nivel >= avatar.nivelRequisito
        
        val adsAssistidos = if (isPadrao || isAdmin) 0 else p.avataresProgressoAds[avatar.id] ?: 0

        // Visualização ampliada (Apenas se desbloqueado)
        binding.imgAvatar.setOnClickListener {
            if (desbloqueado) {
                val subtitulo = when {
                    isPadrao -> "Avatar Original"
                    isAdmin -> "Administrativo"
                    isComercial -> "Coleção"
                    else -> "Nível ${avatar.nivelRequisito}"
                }
                ViewUtils.showZoomDialog(
                    holder.itemView.context,
                    avatar.imagemRes,
                    avatar.nome,
                    subtitulo
                )
            } else {
                Toast.makeText(holder.itemView.context, "Desbloqueie para ampliar", Toast.LENGTH_SHORT).show()
            }
        }

        // Reset views
        binding.txtRequisito.visibility = View.VISIBLE
        binding.txtAdsProgresso.visibility = View.GONE
        binding.txtPrecoFrons.visibility = View.GONE
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

        binding.txtRequisito.text = when {
            isPadrao -> "Sempre disponível"
            isAdmin -> "ADMINISTRATIVO"
            isComercial -> "COLEÇÃO"
            else -> "NÍVEL ${avatar.nivelRequisito}"
        }
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
                    } else if (CollectionRepository.isAvatarValidoParaPlayer(avatar.id, p)) {
                        p.avatarEquipadoId = avatar.id
                        notifyDataSetChanged()
                        onAvatarEquipado()
                    } else {
                        Toast.makeText(holder.itemView.context, "Acesso negado.", Toast.LENGTH_SHORT).show()
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
            isComercial -> {
                // Comercial Bloqueado
                binding.txtPrecoFrons.text = "💰 ${CurrencyUtils.formatar(avatar.precoFrons)}"
                binding.txtPrecoFrons.visibility = View.VISIBLE
                
                binding.btnAcao.text = "COMPRAR"
                binding.btnAcao.visibility = View.VISIBLE
                
                if (avatar.adsNecessarios > 0) {
                    val textoAds = if (adsAssistidos == 0) {
                        "Assistir a ${avatar.adsNecessarios} anúncios OU"
                    } else {
                        "Anúncios assistidos: $adsAssistidos de ${avatar.adsNecessarios}"
                    }
                    binding.txtAdsProgresso.text = textoAds
                    binding.txtAdsProgresso.visibility = View.VISIBLE
                }

                binding.btnAcao.setOnClickListener {
                    mostrarOpcoesCompra(avatar, holder)
                }
            }
            else -> {
                // Progressão Bloqueado (Anúncios)
                binding.txtAdsProgresso.text = "Anúncios: $adsAssistidos de ${avatar.adsNecessarios}"
                binding.txtAdsProgresso.visibility = View.VISIBLE
                binding.txtAdsProgresso.setTextColor(android.graphics.Color.parseColor("#1976D2"))
                
                binding.btnAcao.text = "ASSISTIR ANÚNCIO"
                binding.btnAcao.visibility = View.VISIBLE
                binding.btnAcao.isEnabled = true
                
                binding.btnAcao.setOnClickListener {
                    processarCliqueAnuncio(avatar, holder)
                }
            }
        }
    }

    private fun mostrarOpcoesCompra(avatar: Avatar, holder: AvatarViewHolder) {
        val context = holder.itemView.context
        val activity = context as? Activity ?: return
        val p = PlayerManager.player

        val options = mutableListOf<String>()
        options.add("Comprar por ${CurrencyUtils.formatar(avatar.precoFrons)}")
        if (avatar.adsNecessarios > 0) {
            val assistidos = p.avataresProgressoAds[avatar.id] ?: 0
            options.add("Assistir a anúncios ($assistidos de ${avatar.adsNecessarios})")
        }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Adquirir ${avatar.nome}")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    options[0] -> { // COMPRAR FRONS
                        if (p.dinheiro >= avatar.precoFrons) {
                            p.dinheiro -= avatar.precoFrons
                            p.avataresDesbloqueados.add(avatar.id)
                            PlayerManager.save(context)
                            notifyDataSetChanged()
                            Toast.makeText(context, "🎉 Avatar ${avatar.nome} adquirido!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Saldo insuficiente!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> { // ASSISTIR AD
                        processarCliqueAnuncio(avatar, holder)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
                            Toast.makeText(activity, "Anúncios assistidos: $novoValor de ${avatar.adsNecessarios}", Toast.LENGTH_SHORT).show()
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

    override fun getItemCount(): Int = avatares.size
    
    fun updateList(newList: List<Avatar>) {
        avatares = newList
        notifyDataSetChanged()
    }
}
