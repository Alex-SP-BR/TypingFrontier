package com.typingfrontier.social

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.typingfrontier.PlayerManager
import com.typingfrontier.collection.CollectionRepository
import com.typingfrontier.databinding.ActivitySocialProfileBinding
import com.typingfrontier.utils.ViewUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SocialProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySocialProfileBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("EXTRA_USER_ID")

        binding.btnVoltar.setOnClickListener { finish() }

        carregarDados()
    }

    private fun carregarDados() {
        scope.launch {
            // Garante inicialização para identificar a sessão atual
            SocialProfileRepository.initializeSocialIdentity()
            SocialProfileRepository.awaitInitialization()

            // Se não foi passado um ID externo, tentamos o ID do perfil carregado
            // ou o ID da sessão autenticada (UID do Auth)
            if (userId == null) {
                userId = SocialProfileRepository.currentProfile?.id ?: SocialProfileRepository.getCurrentUserId()
            }

            if (userId == null) {
                Toast.makeText(this@SocialProfileActivity, "Erro ao carregar perfil.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val isOwnProfile = userId == SocialProfileRepository.currentProfile?.id || 
                               userId == SocialProfileRepository.getCurrentUserId()

            val profile = if (isOwnProfile && SocialProfileRepository.currentProfile != null) {
                val base = SocialProfileRepository.currentProfile!!
                val player = PlayerManager.player
                // Para o próprio perfil, mesclamos a identidade social com os status locais atuais
                // que são os mesmos destinados à sincronização.
                base.copy(
                    character_name = player.nome,
                    level = player.nivel,
                    strength = player.forca,
                    resistance = player.resistencia,
                    speed = player.velocidade,
                    intelligence = player.inteligencia,
                    charisma = player.carisma,
                    adventures_completed = player.zonasExploradas.size,
                    best_streak = player.mentalStreak,
                    avatar_equipped_id = player.avatarEquipadoId
                )
            } else {
                SocialProfileRepository.getRemoteProfile(userId!!)
            }

            if (profile != null) {
                binding.btnRegistrarIdentidade.visibility = View.GONE
                exibirPerfil(profile)
                carregarInsignias(profile.id)
            } else {
                if (isOwnProfile) {
                    binding.btnRegistrarIdentidade.visibility = View.VISIBLE
                    binding.btnRegistrarIdentidade.setOnClickListener { mostrarDialogRegistroSocial() }
                    exibirPerfilVazio()
                } else {
                    Toast.makeText(this@SocialProfileActivity, "Perfil não encontrado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun exibirPerfilVazio() {
        val player = PlayerManager.player
        binding.txtProfileUsername.text = "Identidade Não Registrada"
        binding.txtProfileCharacterName.text = player.nome
        binding.txtProfileLevel.text = "Nível: ${player.nivel}"
        binding.txtProfileExp.text = "Aventuras: ${player.zonasExploradas.size}"
        binding.txtProfileStr.text = "Força: ${player.forca}"
        binding.txtProfileRes.text = "Resistência: ${player.resistencia}"
        binding.txtProfileSpd.text = "Velocidade: ${player.velocidade}"
        binding.txtProfileInt.text = "Inteligência: ${player.inteligencia}"
        binding.txtProfileCha.text = "Carisma: ${player.carisma}"
        binding.txtProfileStreak.text = "Seq. Mental: ${player.mentalStreak}"

        val imageRes = if (player.sexo.equals("Feminino", ignoreCase = true)) {
            com.typingfrontier.R.drawable.mulher
        } else {
            com.typingfrontier.R.drawable.homem
        }
        binding.imgProfileAvatar.setImageResource(imageRes)
    }

    private fun mostrarDialogRegistroSocial() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Registrar Nome Social")
        
        val input = android.widget.EditText(this)
        input.hint = "Escolha seu username social"
        builder.setView(input)

        builder.setPositiveButton("Registrar") { _, _ ->
            val username = input.text.toString().trim()
            if (username.isEmpty()) return@setPositiveButton
            
            scope.launch {
                try {
                    val disponivel = SocialProfileRepository.isUsernameAvailable(username)
                    if (!disponivel) {
                        Toast.makeText(this@SocialProfileActivity, "Username ocupado.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val ok = SocialProfileRepository.createSocialProfile(username, PlayerManager.player.nome)
                    if (ok) {
                        Toast.makeText(this@SocialProfileActivity, "Registrado!", Toast.LENGTH_SHORT).show()
                        carregarDados()
                    } else {
                        Toast.makeText(this@SocialProfileActivity, "Erro ao registrar.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@SocialProfileActivity, "Sem internet.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Depois", null)
        builder.show()
    }

    private fun exibirPerfil(profile: SocialProfile) {
        binding.txtProfileUsername.text = "@${profile.username}"
        binding.txtProfileCharacterName.text = profile.character_name ?: "Herói Errante"
        binding.txtProfileLevel.text = "Nível: ${profile.level}"
        binding.txtProfileExp.text = "Aventuras: ${profile.adventures_completed}"
        binding.txtProfileStr.text = "Força: ${profile.strength}"
        binding.txtProfileRes.text = "Resistência: ${profile.resistance}"
        binding.txtProfileSpd.text = "Velocidade: ${profile.speed}"
        binding.txtProfileInt.text = "Inteligência: ${profile.intelligence}"
        binding.txtProfileCha.text = "Carisma: ${profile.charisma}"
        binding.txtProfileStreak.text = "Seq. Mental: ${profile.best_streak}"

        // Avatar Logic
        val avatar = CollectionRepository.getAvatarById(profile.avatar_equipped_id)
        val imageRes = if (avatar != null) {
            avatar.imagemRes
        } else {
            // Fallback para Avatar Original
            if (PlayerManager.player.sexo.equals("Feminino", ignoreCase = true)) {
                com.typingfrontier.R.drawable.mulher
            } else {
                com.typingfrontier.R.drawable.homem
            }
        }

        binding.imgProfileAvatar.setImageResource(imageRes)
        binding.imgProfileAvatar.setOnClickListener {
            ViewUtils.showZoomDialog(
                this,
                imageRes,
                profile.username,
                if (avatar != null) "Avatar de Coleção" else "Avatar Original"
            )
        }
    }

    private fun carregarInsignias(id: String) {
        scope.launch {
            val ids = if (id == SocialProfileRepository.currentProfile?.id) {
                // Para o próprio jogador, usa os dados locais (Offline-first e Instantâneo)
                PlayerManager.player.conquistasDesbloqueadas.toList()
            } else {
                // Para outros jogadores, busca no servidor
                SocialProfileRepository.getUnlockedItems(id, "insignia")
            }

            val achievements = ids.mapNotNull { CollectionRepository.getAchievementById(it) }
            
            binding.recyclerProfileBadges.layoutManager = GridLayoutManager(this@SocialProfileActivity, 4)
            binding.recyclerProfileBadges.adapter = ProfileBadgeAdapter(achievements)
        }
    }
}

class ProfileBadgeAdapter(private val list: List<com.typingfrontier.collection.Achievement>) : 
    androidx.recyclerview.widget.RecyclerView.Adapter<ProfileBadgeAdapter.BadgeViewHolder>() {

    class BadgeViewHolder(val img: android.widget.ImageView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(img)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): BadgeViewHolder {
        val img = android.widget.ImageView(parent.context)
        val size = (parent.context.resources.displayMetrics.density * 60).toInt()
        img.layoutParams = android.view.ViewGroup.LayoutParams(size, size)
        img.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        img.setPadding(8, 8, 8, 8)
        return BadgeViewHolder(img)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val ach = list[position]
        holder.img.setImageResource(ach.insigniaRes)
        holder.img.setOnClickListener {
            ViewUtils.showZoomDialog(holder.img.context, ach.insigniaRes, ach.nome, ach.descricao)
        }
    }

    override fun getItemCount(): Int = list.size
}
