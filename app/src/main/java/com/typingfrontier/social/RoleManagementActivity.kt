package com.typingfrontier.social

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarRecycler()
        configurarBotoes()
        carregarUsuarios()
    }

    private fun configurarRecycler() {
        binding.recyclerUsers.layoutManager = LinearLayoutManager(this)
        binding.swipeRefresh.setOnRefreshListener { carregarUsuarios() }
    }

    private fun configurarBotoes() {
        binding.btnVoltar.setOnClickListener { finish() }
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

                exibirUsuarios(users)
            } catch (e: Exception) {
                Toast.makeText(this@RoleManagementActivity, "Erro ao carregar usuários.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
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
