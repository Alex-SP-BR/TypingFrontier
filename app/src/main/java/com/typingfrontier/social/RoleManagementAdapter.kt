package com.typingfrontier.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.databinding.ItemUserRoleBinding

class RoleManagementAdapter(
    private var users: List<SocialProfile>,
    private val currentAdminId: String,
    private val onRoleClick: (SocialProfile) -> Unit
) : RecyclerView.Adapter<RoleManagementAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemUserRoleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserRoleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        val binding = holder.binding

        binding.txtUsername.text = "@${user.username}"
        binding.txtCurrentRole.text = "Role: ${user.role}"

        // O administrador não pode alterar a própria role nem a de outros administradores
        val canModify = user.id != currentAdminId && user.role != "administrator"
        
        if (canModify) {
            binding.btnChangeRole.visibility = View.VISIBLE
            binding.btnChangeRole.setOnClickListener { onRoleClick(user) }
        } else {
            binding.btnChangeRole.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newList: List<SocialProfile>) {
        users = newList
        notifyDataSetChanged()
    }
}
