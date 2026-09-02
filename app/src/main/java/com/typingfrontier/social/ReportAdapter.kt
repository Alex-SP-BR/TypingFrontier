package com.typingfrontier.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.typingfrontier.databinding.ItemReportBinding

class ReportAdapter(
    private var reports: List<Report>,
    private val onItemClick: (Report) -> Unit
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        val binding = holder.binding

        binding.txtReportTarget.text = report.target_type.uppercase()
        binding.txtReportStatus.text = report.status.uppercase()
        binding.txtReportReason.text = report.reason
        binding.txtReportDesc.text = report.description ?: "Sem descrição."
        binding.txtReportDate.text = report.created_at.take(16).replace("T", " ")

        // Dados do Autor e Denunciante
        val authorName = report.targetAuthorUsername ?: "..."
        val reporterName = report.reporterUsername ?: "..."
        
        binding.txtReportAuthor.text = "Autor: @$authorName"
        binding.txtReportAuthorRole.text = "Cargo: ${traduzirRole(report.targetAuthorRole)}"
        binding.txtReportReporter.text = "Denunciante: @$reporterName"
        
        binding.txtReportAuthor.visibility = android.view.View.VISIBLE
        binding.txtReportAuthorRole.visibility = android.view.View.VISIBLE
        binding.txtReportReporter.visibility = android.view.View.VISIBLE

        // Cores por status
        val statusColor = when (report.status.lowercase()) {
            "pending" -> android.graphics.Color.parseColor("#E53935") // Vermelho
            "reviewing" -> android.graphics.Color.parseColor("#FBC02D") // Amarelo/Ouro
            "resolved" -> android.graphics.Color.parseColor("#4CAF50") // Verde
            else -> android.graphics.Color.GRAY
        }
        binding.txtReportStatus.setTextColor(statusColor)

        holder.itemView.setOnClickListener {
            onItemClick(report)
        }
    }

    override fun getItemCount(): Int = reports.size

    fun updateData(newList: List<Report>) {
        reports = newList
        notifyDataSetChanged()
    }

    private fun traduzirRole(role: String?): String {
        return when (role) {
            "administrator" -> "ADMINISTRADOR"
            "senior_moderator" -> "MODERADOR SÊNIOR"
            "moderator" -> "MODERADOR"
            else -> "USUÁRIO"
        }
    }
}
