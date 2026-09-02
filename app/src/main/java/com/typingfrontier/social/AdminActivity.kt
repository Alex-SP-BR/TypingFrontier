package com.typingfrontier.social

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.databinding.ActivityAdminBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren

/**
 * Ponto de entrada para ferramentas administrativas do Typing Frontier.
 * Atualmente atua como uma tela base que valida privilégios de acesso.
 */
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Verificação de Segurança (Proteção da Interface)
        val profile = SocialProfileRepository.currentProfile
        
        // Se o perfil ainda não carregou, tentamos carregar a identidade social novamente
        // e avisamos o usuário, pois não podemos conceder acesso sem saber a role.
        if (profile == null) {
            Toast.makeText(this, "Validando identidade social... Tente novamente.", Toast.LENGTH_LONG).show()
            SocialProfileRepository.initializeSocialIdentity()
            finish()
            return
        }

        val role = profile.role
        val temAcesso = role == "moderator" || role == "senior_moderator" || role == "administrator"

        if (!temAcesso) {
            Toast.makeText(this, "Acesso restrito.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtSeuCargo.text = "SEU CARGO: ${traduzirRole(role)}"

        // 2. Configurar Interface
        binding.btnVerDenuncias.setOnClickListener {
            startActivity(android.content.Intent(this, ReportActivity::class.java))
        }

        binding.btnGerenciarBan.setOnClickListener {
            mostrarDialogBanimento()
        }

        binding.btnGerenciarRoles.setOnClickListener {
            startActivity(android.content.Intent(this, RoleManagementActivity::class.java))
        }

        binding.btnSupervisaoMod.setOnClickListener {
            startActivity(android.content.Intent(this, ModerationSupervisionActivity::class.java))
        }

        if (profile.role == "administrator") {
            binding.btnGerenciarRoles.visibility = android.view.View.VISIBLE
            binding.btnSupervisaoMod.visibility = android.view.View.VISIBLE
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun traduzirRole(role: String?): String {
        return when (role) {
            "administrator" -> "ADMINISTRADOR"
            "senior_moderator" -> "MODERADOR SÊNIOR"
            "moderator" -> "MODERADOR"
            else -> "USUÁRIO"
        }
    }

    private fun mostrarDialogBanimento() {
        if (isProcessing) return

        val dialogView = layoutInflater.inflate(com.typingfrontier.R.layout.dialog_manage_ban, null)
        val edtSearch = dialogView.findViewById<android.widget.EditText>(com.typingfrontier.R.id.edtSearchUsername)
        val btnSearch = dialogView.findViewById<android.widget.Button>(com.typingfrontier.R.id.btnSearch)
        val progressSearch = dialogView.findViewById<android.widget.ProgressBar>(com.typingfrontier.R.id.progressSearch)
        
        val layoutUserInfo = dialogView.findViewById<android.view.View>(com.typingfrontier.R.id.layoutUserInfo)
        val txtUserDisplay = dialogView.findViewById<android.widget.TextView>(com.typingfrontier.R.id.txtUserDisplay)
        val txtUserRole = dialogView.findViewById<android.widget.TextView>(com.typingfrontier.R.id.txtUserRole)
        val txtUserStatus = dialogView.findViewById<android.widget.TextView>(com.typingfrontier.R.id.txtUserStatus)
        val txtBanReason = dialogView.findViewById<android.widget.TextView>(com.typingfrontier.R.id.txtBanReason)
        
        val layoutBanAction = dialogView.findViewById<android.view.View>(com.typingfrontier.R.id.layoutBanAction)
        val edtBanReason = dialogView.findViewById<android.widget.EditText>(com.typingfrontier.R.id.edtBanReason)
        val spinnerDuration = dialogView.findViewById<android.widget.Spinner>(com.typingfrontier.R.id.spinnerDuration)
        val btnExecuteBan = dialogView.findViewById<android.widget.Button>(com.typingfrontier.R.id.btnExecuteBan)
        val btnExecuteUnban = dialogView.findViewById<android.widget.Button>(com.typingfrontier.R.id.btnExecuteUnban)

        val options = arrayOf("24 horas", "7 dias", "14 dias", "21 dias", "30 dias", "Permanente")
        spinnerDuration.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, options)

        var selectedProfile: SocialProfile? = null

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Gerenciar Banimento")
            .setView(dialogView)
            .setNegativeButton("Fechar", null)
            .create()

        btnSearch.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            val username = edtSearch.text.toString().trim()
            if (username.isEmpty()) return@setOnClickListener

            isProcessing = true
            progressSearch.visibility = View.VISIBLE
            layoutUserInfo.visibility = View.GONE
            layoutBanAction.visibility = View.GONE
            btnExecuteUnban.visibility = View.GONE

            scope.launch {
                try {
                    // Busca limitada a autores que possuem denúncias (Ponto Crítico da Etapa)
                    val reportedProfiles = ModerationRepository.getReportedAuthors(username)
                    
                    if (reportedProfiles.isEmpty()) {
                        runOnUiThread {
                            Toast.makeText(this@AdminActivity, "Nenhum usuário denunciado encontrado.", Toast.LENGTH_SHORT).show()
                            progressSearch.visibility = View.GONE
                            isProcessing = false
                        }
                        return@launch
                    }

                    // Se houver múltiplos (ex: busca parcial), pegamos o primeiro exato ou o primeiro da lista
                    val profile = reportedProfiles.firstOrNull { it.username.equals(username.removePrefix("@"), ignoreCase = true) } 
                                  ?: reportedProfiles.first()

                    val isBanned = ModerationRepository.isUserBanned(profile.id)
                    val banStatus = if (isBanned) ModerationRepository.getBanStatus(profile.id) else null

                    runOnUiThread {
                        selectedProfile = profile
                        txtUserDisplay.text = "@${profile.username}"
                        txtUserRole.text = "Cargo: ${traduzirRole(profile.role)}"
                        
                        if (isBanned) {
                            txtUserStatus.text = "Status: BANIDO"
                            txtUserStatus.setTextColor(android.graphics.Color.RED)
                            txtBanReason.visibility = View.VISIBLE
                            txtBanReason.text = "Motivo: ${banStatus?.reason ?: "N/A"}"
                            btnExecuteUnban.visibility = View.VISIBLE
                            layoutBanAction.visibility = View.GONE
                        } else {
                            txtUserStatus.text = "Status: ATIVO"
                            txtUserStatus.setTextColor(android.graphics.Color.parseColor("#388E3C"))
                            txtBanReason.visibility = View.GONE
                            btnExecuteUnban.visibility = View.GONE
                            
                            // Só permite banir se não for o próprio e seguir hierarquia
                            val myRole = SocialProfileRepository.currentProfile?.role ?: "usuario"
                            if (profile.id != SocialProfileRepository.currentProfile?.id && 
                                getRoleWeight(myRole) > getRoleWeight(profile.role)) {
                                layoutBanAction.visibility = View.VISIBLE
                            }
                        }

                        layoutUserInfo.visibility = View.VISIBLE
                        progressSearch.visibility = View.GONE
                        isProcessing = false
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@AdminActivity, "Erro ao buscar: ${e.message}", Toast.LENGTH_SHORT).show()
                        progressSearch.visibility = View.GONE
                        isProcessing = false
                    }
                }
            }
        }

        btnExecuteBan.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            val profile = selectedProfile ?: return@setOnClickListener
            val reason = edtBanReason.text.toString().trim()
            if (reason.isEmpty()) {
                Toast.makeText(this, "Motivo obrigatório", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val durationIndex = spinnerDuration.selectedItemPosition
            val durationText = options[durationIndex]

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirmar Banimento")
                .setMessage("Banir @${profile.username} por $durationText?")
                .setPositiveButton("Confirmar") { _, _ ->
                    executarAcaoBanimento(profile.id, reason, durationIndex, true, dialog)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnExecuteUnban.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            val profile = selectedProfile ?: return@setOnClickListener
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirmar Desbanimento")
                .setMessage("Deseja desbanir @${profile.username}?")
                .setPositiveButton("Confirmar") { _, _ ->
                    executarAcaoBanimento(profile.id, "", -1, false, dialog)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        dialog.show()
    }

    private fun executarAcaoBanimento(userId: String, reason: String, durationIndex: Int, isBan: Boolean, parentDialog: androidx.appcompat.app.AlertDialog) {
        if (isProcessing) return
        isProcessing = true
        
        scope.launch {
            try {
                if (isBan) {
                    val hours = when (durationIndex) {
                        0 -> 24
                        1 -> 168
                        2 -> 336
                        3 -> 504
                        4 -> 720
                        else -> null
                    }
                    ModerationRepository.banUser(userId, reason, hours)
                } else {
                    ModerationRepository.unbanUser(userId)
                }
                
                runOnUiThread {
                    Toast.makeText(this@AdminActivity, "Operação concluída.", Toast.LENGTH_SHORT).show()
                    parentDialog.dismiss()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@AdminActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isProcessing = false
            }
        }
    }

    private fun getRoleWeight(role: String?): Int {
        return when (role) {
            "administrator" -> 3
            "senior_moderator" -> 2
            "moderator" -> 1
            else -> 0
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext.cancelChildren()
    }
}
