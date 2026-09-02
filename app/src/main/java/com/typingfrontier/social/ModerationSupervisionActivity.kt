package com.typingfrontier.social

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.databinding.ActivityModerationSupervisionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren

class ModerationSupervisionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModerationSupervisionBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isRefreshing = false

    private data class ModeratorStats(
        val id: String,
        var username: String = "",
        var role: String = "",
        var inAnalysis: Int = 0,
        var resolved: Int = 0,
        var dismissed: Int = 0,
        var totalAssigned: Int = 0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Validar Acesso (administrator exclusivo)
        val profile = SocialProfileRepository.currentProfile
        if (profile?.role != "administrator") {
            Toast.makeText(this, "Acesso negado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding = ActivityModerationSupervisionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.swipeRefresh.setOnRefreshListener { carregarDados() }
        binding.btnVoltar.setOnClickListener { finish() }

        carregarDenunciasEAtividade()
    }

    private fun carregarDenunciasEAtividade() {
        if (isRefreshing) return
        isRefreshing = true
        binding.swipeRefresh.isRefreshing = true

        scope.launch {
            try {
                // 1. Carregar Denúncias Recentes (Limite 100)
                val reports = ModerationRepository.getReports()
                
                // 2. Carregar Logs de Auditoria Recentes
                val logs = ModerationSupervisionRepository.getAdminLogs()

                atualizarInterface(reports, logs)
            } catch (e: Exception) {
                Toast.makeText(this@ModerationSupervisionActivity, "Erro ao carregar supervisão.", Toast.LENGTH_SHORT).show()
            } finally {
                isRefreshing = false
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    // Alias para manter compatibilidade com listener do swipe
    private fun carregarDados() = carregarDenunciasEAtividade()

    private suspend fun atualizarInterface(reports: List<Report>, logs: List<AdminLogWithProfile>) {
        // Métrica: Visão Geral
        val pending = reports.count { it.status.lowercase() == "pending" }
        val reviewing = reports.count { it.status.lowercase() == "reviewing" }
        val resolved = reports.count { it.status.lowercase() == "resolved" }
        val dismissed = reports.count { it.status.lowercase() == "dismissed" }

        binding.txtStatsReports.text = "Pendentes: $pending\nEm Análise: $reviewing\nResolvidas: $resolved\nDescartadas: $dismissed"

        // Métrica: Moderação em Andamento (Resumo Rápido)
        val activeAnalyses = reports.filter { it.status.lowercase() == "reviewing" && it.moderator_id != null }
        if (activeAnalyses.isEmpty()) {
            binding.txtActiveModerators.text = "Nenhuma análise em andamento no momento."
        } else {
            val countsByMod = activeAnalyses.groupBy { it.moderator_id!! }
            val activeText = StringBuilder()
            
            countsByMod.forEach { (modId, modReports) ->
                val modProfile = ModerationRepository.getProfileById(modId)
                val username = modProfile?.username ?: "ID: ${modId.take(8)}..."
                activeText.append("@$username — ${modReports.size} análise(s)\n")
            }
            binding.txtActiveModerators.text = activeText.toString().trim()
        }

        // Métrica: Atuação da Equipe (Agregação Individual)
        val statsMap = mutableMapOf<String, ModeratorStats>()
        reports.forEach { report ->
            val modId = report.moderator_id ?: return@forEach
            val stats = statsMap.getOrPut(modId) { ModeratorStats(modId) }
            stats.totalAssigned++
            when (report.status.lowercase()) {
                "reviewing" -> stats.inAnalysis++
                "resolved" -> stats.resolved++
                "dismissed" -> stats.dismissed++
            }
        }

        if (statsMap.isEmpty()) {
            binding.txtTeamPerformance.text = "Nenhuma atividade de equipe registrada nos dados recentes."
        } else {
            // Resolver perfis para obter usernames e roles
            val modProfiles = ModerationSupervisionRepository.getProfilesByIds(statsMap.keys.toList())
            val performanceList = mutableListOf<ModeratorStats>()
            
            modProfiles.forEach { profile ->
                // Filtrar somente moderator e senior_moderator
                if (profile.role == "moderator" || profile.role == "senior_moderator") {
                    val stats = statsMap[profile.id]!!
                    stats.username = profile.username
                    stats.role = profile.role
                    performanceList.add(stats)
                }
            }
            
            // Ordenação: Em análise DESC, Total DESC, Username ASC
            performanceList.sortWith(compareByDescending<ModeratorStats> { it.inAnalysis }
                .thenByDescending { it.totalAssigned }
                .thenBy { it.username })

            if (performanceList.isEmpty()) {
                binding.txtTeamPerformance.text = "Sem dados operacionais de moderadores/seniores."
            } else {
                val teamText = StringBuilder()
                performanceList.forEach { stats ->
                    val roleLabel = if (stats.role == "senior_moderator") "Senior Moderator" else "Moderator"
                    teamText.append("@${stats.username} ($roleLabel)\n")
                    teamText.append("Em análise: ${stats.inAnalysis} | Resolvidas: ${stats.resolved} | Descartadas: ${stats.dismissed} | Assumidas: ${stats.totalAssigned}\n\n")
                }
                binding.txtTeamPerformance.text = teamText.toString().trim()
            }
        }

        // Métrica: Atividade Recente (Admin Logs)
        if (logs.isEmpty()) {
            binding.txtRecentActivity.text = "Nenhum log de atividade recente."
        } else {
            val activityText = StringBuilder()
            logs.forEach { log ->
                val admin = log.profiles?.username ?: "Admin"
                val action = traduzirAcao(log.action)
                activityText.append("@$admin — $action\n")
            }
            binding.txtRecentActivity.text = activityText.toString().trim()
        }
    }

    private fun traduzirAcao(action: String): String {
        return when (action) {
            "resolve_report" -> "resolveu uma denúncia"
            "dismiss_report" -> "descartou uma denúncia"
            "ban_user" -> "aplicou banimento permanente"
            "suspend_user" -> "aplicou suspensão temporária"
            "unban_user" -> "removeu banimento"
            "claim_report" -> "assumiu uma análise"
            "change_role" -> "alterou o cargo de um usuário"
            else -> action
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext.cancelChildren()
    }
}
