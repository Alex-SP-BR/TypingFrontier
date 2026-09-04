package com.typingfrontier.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.databinding.ItemUserRoleBinding

class RoleManagementAdapter(
    private var allUsers: List<SocialProfile>,
    private val currentAdminId: String,
    private val onRoleClick: (SocialProfile) -> Unit
) : RecyclerView.Adapter<RoleManagementAdapter.ViewHolder>() {

    private var filteredUsers: List<SocialProfile> = allUsers
    private var currentQuery: String = ""
    private var currentRoleFilter: String? = null
    
    // Set de IDs de usuários online para renderização rápida
    private var onlineUserIds: Set<String> = emptySet()
    private var isPresenceActive: Boolean = false

    class ViewHolder(val binding: ItemUserRoleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserRoleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = filteredUsers[position]
        val binding = holder.binding

        binding.txtUsername.text = "@${user.username}"
        binding.txtCurrentRole.text = "Role: ${user.role}"

        // Atualiza status de presença
        if (!isPresenceActive) {
            binding.txtPresenceStatus.text = "○ STATUS INDISPONÍVEL"
            binding.txtPresenceStatus.setTextColor(android.graphics.Color.parseColor("#999999"))
        } else if (onlineUserIds.contains(user.id)) {
            binding.txtPresenceStatus.text = "● ONLINE"
            binding.txtPresenceStatus.setTextColor(android.graphics.Color.parseColor("#388E3C"))
        } else {
            binding.txtPresenceStatus.text = "○ OFFLINE"
            binding.txtPresenceStatus.setTextColor(android.graphics.Color.parseColor("#999999"))
        }

        // O administrador não pode alterar a própria role nem a de outros administradores
        val canModify = user.id != currentAdminId && user.role != "administrator"
        
        if (canModify) {
            binding.btnChangeRole.visibility = View.VISIBLE
            binding.btnChangeRole.setOnClickListener { onRoleClick(user) }
        } else {
            binding.btnChangeRole.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = filteredUsers.size

    fun updateData(newList: List<SocialProfile>) {
        allUsers = newList
        applyFilters()
    }

    fun filterBySearch(query: String) {
        currentQuery = query
        applyFilters()
    }

    fun filterByRole(role: String?) {
        currentRoleFilter = role
        applyFilters()
    }

    fun updatePresenceStatus(newOnlineIds: Set<String>, active: Boolean) {
        onlineUserIds = newOnlineIds
        isPresenceActive = active
        notifyDataSetChanged()
    }

    private fun applyFilters() {
        val normalizedQuery = currentQuery.lowercase().trim().removePrefix("@")
        
        filteredUsers = allUsers.filter { user ->
            val matchesSearch = normalizedQuery.isEmpty() || user.username.lowercase().contains(normalizedQuery)
            val matchesRole = currentRoleFilter == null || user.role == currentRoleFilter
            matchesSearch && matchesRole
        }
        notifyDataSetChanged()
    }
}
