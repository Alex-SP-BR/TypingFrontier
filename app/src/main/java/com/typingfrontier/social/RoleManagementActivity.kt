package com.typingfrontier.social

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.typingfrontier.databinding.ActivityRoleManagementBinding
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoleManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleManagementBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var adapter: RoleManagementAdapter? = null
    
    private var isUpdating = false
    private var presenceJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarInterfaceInicial()
        configurarRecycler()
        configurarBotoes()
        configurarBusca()
        iniciarObservacaoPresenca()
        carregarUsuarios()
    }

    private fun iniciarObservacaoPresenca() {
        presenceJob?.cancel()
        presenceJob = scope.launch {
            PresenceManager.onlineUsers.collect { presenceList ->
                atualizarInterfacePresenca(presenceList)
            }
        }
    }

    private fun atualizarInterfacePresenca(list: List<PresenceManager.PresencePayload>) {
        val isActive = PresenceManager.isPresenceActive()
        
        // Deduplicação por user_id
        val uniqueUsers = list.distinctBy { it.user_id }
        val totalOnline = uniqueUsers.size
        
        if (isActive) {
            val labelJogadores = if (totalOnline == 1) "jogador" else "jogadores"
            binding.txtOnlineAgora.text = "Online agora: $totalOnline $labelJogadores"
            binding.txtOnlineAgora.setTextColor(android.graphics.Color.parseColor("#388E3C"))
        } else {
            binding.txtOnlineAgora.text = "Online agora: Status Indisponível"
            binding.txtOnlineAgora.setTextColor(android.graphics.Color.parseColor("#999999"))
        }

        // Atualiza o status individual na lista através do adapter
        val onlineIds = uniqueUsers.map { it.user_id }.toSet()
        adapter?.updatePresenceStatus(onlineIds, isActive)

        val equipeOnline = uniqueUsers.filter { 
            it.role == "moderator" || it.role == "senior_moderator" || it.role == "administrator"
        }

        if (isActive) {
            val labelMembros = if (equipeOnline.size == 1) "membro" else "membros"
            binding.txtEquipeOnlineCount.text = "Equipe online: ${equipeOnline.size} $labelMembros"
        } else {
            binding.txtEquipeOnlineCount.text = "Equipe online: --"
        }

        if (!isActive) {
            binding.txtEquipeOnlineLista.text = "Serviço de presença indisponível."
        } else if (equipeOnline.isEmpty()) {
            binding.txtEquipeOnlineLista.text = "Nenhum membro da equipe está online."
        } else {
            val sb = StringBuilder()
            equipeOnline.forEach { member ->
                val roleLabel = when (member.role) {
                    "administrator" -> "Administrador"
                    "senior_moderator" -> "Moderador Sênior"
                    "moderator" -> "Moderador"
                    else -> member.role
                }
                sb.append("@${member.username} — $roleLabel\n")
            }
            binding.txtEquipeOnlineLista.text = sb.toString().trim()
        }
    }

    private fun configurarInterfaceInicial() {
        val profile = SocialProfileRepository.currentProfile
        val role = profile?.role ?: "usuario"
        binding.txtSeuCargo.text = "SEU CARGO: ${traduzirRole(role)}"
    }

    private fun traduzirRole(role: String?): String {
        return when (role) {
            "administrator" -> "ADMINISTRADOR"
            "senior_moderator" -> "MODERADOR SÊNIOR"
            "moderator" -> "MODERADOR"
            else -> "USUÁRIO"
        }
    }

    private fun configurarRecycler() {
        binding.recyclerUsers.layoutManager = LinearLayoutManager(this)
        binding.swipeRefresh.setOnRefreshListener { carregarUsuarios() }
    }

    private fun configurarBotoes() {
        binding.btnVoltar.setOnClickListener { finish() }
    }

    private fun configurarBusca() {
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter?.filterBySearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupRoles.setOnCheckedChangeListener { _, checkedId ->
            val roleFilter = when (checkedId) {
                com.typingfrontier.R.id.chipUsuarios -> "usuario"
                com.typingfrontier.R.id.chipModeradores -> "moderator"
                com.typingfrontier.R.id.chipSeniores -> "senior_moderator"
                com.typingfrontier.R.id.chipAdmins -> "administrator"
                else -> null
            }
            adapter?.filterByRole(roleFilter)
        }
    }

    private fun carregarUsuarios() {
        if (isUpdating) return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = true

        scope.launch {
            try {
                val users = withContext(Dispatchers.IO) {
                    SupabaseManager.client.postgrest["profiles"]
                        .select() {
                            order("username", Order.ASCENDING)
                        }.decodeList<SocialProfile>()
                }

                atualizarEstatisticas(users)
                exibirUsuarios(users)
            } catch (e: Exception) {
                Toast.makeText(this@RoleManagementActivity, "Erro ao carregar usuários.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun atualizarEstatisticas(users: List<SocialProfile>) {
        val total = users.size
        binding.txtTotalJogadores.text = "Jogadores cadastrados: $total"

        val countUsuarios = users.count { it.role == "usuario" }
        val countModerators = users.count { it.role == "moderator" }
        val countSeniors = users.count { it.role == "senior_moderator" }
        val countAdmins = users.count { it.role == "administrator" }

        val stats = StringBuilder()
            .append("USUÁRIOS: $countUsuarios\n")
            .append("MODERADORES: $countModerators\n")
            .append("MODERADORES SÊNIOR: $countSeniors\n")
            .append("ADMINISTRADORES: $countAdmins")
            .toString()

        binding.txtStatsRoles.text = stats
    }

    private fun exibirUsuarios(list: List<SocialProfile>) {
        val adminId = SocialProfileRepository.currentProfile?.id ?: ""
        
        if (adapter == null) {
            adapter = RoleManagementAdapter(list, adminId) { user ->
                mostrarMenuRoles(user)
            }
            binding.recyclerUsers.adapter = adapter
        } else {
            adapter?.updateData(list)
        }

        // Garante que o estado de presença atual seja aplicado à nova lista
        val presenceList = PresenceManager.onlineUsers.value
        val onlineIds = presenceList.map { it.user_id }.toSet()
        adapter?.updatePresenceStatus(onlineIds, PresenceManager.isPresenceActive())
    }

    private fun mostrarMenuRoles(user: SocialProfile) {
        if (isUpdating) return

        val options = mutableListOf<String>()
        val roles = mutableListOf<String>()

        when (user.role) {
            "usuario" -> {
                options.add("Promover a Moderator")
                roles.add("moderator")
                options.add("Promover a Senior Moderator")
                roles.add("senior_moderator")
            }
            "moderator" -> {
                options.add("Promover a Senior Moderator")
                roles.add("senior_moderator")
                options.add("Rebaixar para Usuario")
                roles.add("usuario")
            }
            "senior_moderator" -> {
                options.add("Rebaixar para Moderator")
                roles.add("moderator")
                options.add("Rebaixar para Usuario")
                roles.add("usuario")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Gerenciar Role: @${user.username}")
            .setItems(options.toTypedArray()) { _, which ->
                confirmarAlteracao(user, roles[which], options[which])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarAlteracao(user: SocialProfile, newRole: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Alteração")
            .setMessage("${message} para @${user.username}?")
            .setPositiveButton("Confirmar") { _, _ ->
                executarAlteracao(user.id, newRole)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executarAlteracao(userId: String, newRole: String) {
        if (isUpdating) return
        isUpdating = true
        binding.progressBar.visibility = View.VISIBLE

        scope.launch {
            try {
                ModerationRepository.updateUserRole(userId, newRole)
                Toast.makeText(this@RoleManagementActivity, "Role atualizada com sucesso.", Toast.LENGTH_SHORT).show()
                carregarUsuarios()
            } catch (e: Exception) {
                Toast.makeText(this@RoleManagementActivity, "Erro ao atualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdating = false
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
