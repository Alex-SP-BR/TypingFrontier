package com.typingfrontier.social

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.typingfrontier.databinding.ActivityReportBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var adapter: ReportAdapter? = null
    private var isProcessing = false
    
    private var allReports: List<Report> = emptyList()
    private var currentTab = 0
    private var pendingReportId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Proteção redundante
        val role = SocialProfileRepository.currentProfile?.role ?: "usuario"
        if (role != "moderator" && role != "senior_moderator" && role != "administrator") {
            finish()
            return
        }

        pendingReportId = intent.getStringExtra("EXTRA_REPORT_ID")

        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarAbas()
        binding.recyclerReports.layoutManager = LinearLayoutManager(this)
        binding.swipeRefresh.setOnRefreshListener { carregarDenuncias() }
        binding.btnVoltar.setOnClickListener { finish() }

        carregarDenuncias()
    }

    private fun configurarAbas() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                atualizarListaPorAba()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun carregarDenuncias() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = true

        scope.launch {
            try {
                allReports = ModerationRepository.getReports()
                
                // Inicializa o adapter se for nulo, mas a lista será filtrada por atualizarListaPorAba
                if (adapter == null) {
                    adapter = ReportAdapter(emptyList()) { report ->
                        analisarDenuncia(report)
                    }
                    binding.recyclerReports.adapter = adapter
                }
                
                atualizarListaPorAba()
                
                // Tratar abertura automática de report específico
                if (pendingReportId != null) {
                    val reportId = pendingReportId!!
                    pendingReportId = null // Limpa para não abrir novamente em refresh
                    
                    var targetReport = allReports.find { it.id == reportId }
                    if (targetReport == null) {
                        // Busca individual caso não esteja nos últimos 100
                        targetReport = ModerationRepository.getReportById(reportId)
                    }
                    
                    targetReport?.let { 
                        // Enriquecer apenas esse antes de abrir
                        enriquecerDenunciaIndividual(it)
                        analisarDenuncia(it) 
                    }
                }
                
                // Enriquecer dados em background
                enriquecerDenuncias(allReports)
                
            } catch (e: Exception) {
                Toast.makeText(this@ReportActivity, "Erro ao carregar: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private suspend fun enriquecerDenunciaIndividual(report: Report) {
        try {
            if (report.reporterUsername == null) {
                val reporter = ModerationRepository.getProfileById(report.reporter_id)
                report.reporterUsername = reporter?.username ?: "Usuário Desconhecido"
            }
            if (report.targetAuthorId == null) {
                report.targetAuthorId = ModerationRepository.getContentAuthorId(report.target_type, report.target_id)
            }
            if (report.targetAuthorId != null && report.targetAuthorUsername == null) {
                val author = ModerationRepository.getProfileById(report.targetAuthorId!!)
                report.targetAuthorUsername = author?.username
                report.targetAuthorRole = author?.role
                report.targetAuthorLevel = author?.level
            }
            if (report.targetContentText == null) {
                val details = ModerationRepository.getContentDetails(report.target_type, report.target_id)
                if (details != null) {
                    report.targetContentText = details.content
                    report.targetTitle = details.title
                    report.targetCategory = details.category ?: "general"
                } else {
                    report.targetContentText = "CONTEÚDO NÃO ENCONTRADO — POSSIVELMENTE EXCLUÍDO"
                }
            }
        } catch (e: Exception) {}
    }

    private fun atualizarListaPorAba() {
        val myUid = SocialProfileRepository.currentProfile?.id ?: ""
        
        val filtrada = when (currentTab) {
            0 -> { // PENDENTES: Status pending, mais antigas primeiro
                allReports.filter { it.status.lowercase() == "pending" }
                    .sortedBy { it.created_at }
            }
            1 -> { // MINHAS: Status reviewing + meu moderator_id, mais antigas primeiro
                allReports.filter { it.status.lowercase() == "reviewing" && it.moderator_id == myUid }
                    .sortedBy { it.created_at }
            }
            2 -> { // HISTÓRICO: Status resolved ou dismissed, mais recentes primeiro
                allReports.filter { it.status.lowercase() == "resolved" || it.status.lowercase() == "dismissed" }
                    .sortedByDescending { it.created_at }
            }
            else -> emptyList()
        }

        adapter?.updateData(filtrada)
        
        // Atualizar contadores nas abas
        atualizarContadores()
    }

    private fun atualizarContadores() {
        val myUid = SocialProfileRepository.currentProfile?.id ?: ""
        
        val countPendentes = allReports.count { it.status.lowercase() == "pending" }
        val countMinhas = allReports.count { it.status.lowercase() == "reviewing" && it.moderator_id == myUid }
        val countHistorico = allReports.count { it.status.lowercase() == "resolved" || it.status.lowercase() == "dismissed" }

        binding.tabLayout.getTabAt(0)?.text = "Pendentes ($countPendentes)"
        binding.tabLayout.getTabAt(1)?.text = "Minhas ($countMinhas)"
        binding.tabLayout.getTabAt(2)?.text = "Histórico ($countHistorico)"
    }

    private suspend fun enriquecerDenuncias(list: List<Report>) {
        list.forEach { report ->
            try {
                // 1. Resolver Denunciante
                if (report.reporterUsername == null) {
                    val reporter = ModerationRepository.getProfileById(report.reporter_id)
                    report.reporterUsername = reporter?.username ?: "Usuário Desconhecido"
                }

                // 2. Resolver Autor do Conteúdo e Texto
                if (report.targetAuthorId == null) {
                    report.targetAuthorId = ModerationRepository.getContentAuthorId(report.target_type, report.target_id)
                }

                if (report.targetAuthorId != null && report.targetAuthorUsername == null) {
                    val author = ModerationRepository.getProfileById(report.targetAuthorId!!)
                    report.targetAuthorUsername = author?.username
                    report.targetAuthorRole = author?.role
                    report.targetAuthorLevel = author?.level
                }

                if (report.targetContentText == null) {
                    val details = ModerationRepository.getContentDetails(report.target_type, report.target_id)
                    if (details != null) {
                        report.targetContentText = details.content
                        report.targetTitle = details.title
                        report.targetCategory = details.category ?: "general"
                    } else {
                        report.targetContentText = "CONTEÚDO NÃO ENCONTRADO — POSSIVELMENTE EXCLUÍDO"
                    }
                }
            } catch (e: Exception) {
                // Ignora erro individual para não travar a lista
            }
        }
        runOnUiThread { adapter?.notifyDataSetChanged() }
    }

    private fun analisarDenuncia(report: Report) {
        if (isProcessing) return

        val currentUid = SocialProfileRepository.currentProfile?.id
        val status = report.status.lowercase()

        // Identificar Autor do Conteúdo para impedir auto-moderação (Caso ainda não carregado)
        scope.launch {
            if (report.targetAuthorId == null) {
                report.targetAuthorId = ModerationRepository.getContentAuthorId(report.target_type, report.target_id)
            }
            
            val authorId = report.targetAuthorId

            if (authorId != null && authorId == currentUid) {
                runOnUiThread { Toast.makeText(this@ReportActivity, "Você não pode moderar seu próprio conteúdo.", Toast.LENGTH_SHORT).show() }
                return@launch
            }

            // Carrega role do autor se necessário para hierarquia visual
            if (authorId != null && report.targetAuthorRole == null) {
                val author = ModerationRepository.getProfileById(authorId)
                report.targetAuthorUsername = author?.username
                report.targetAuthorRole = author?.role
                report.targetAuthorLevel = author?.level
            }

            if (report.targetContentText == null) {
                val details = ModerationRepository.getContentDetails(report.target_type, report.target_id)
                if (details != null) {
                    report.targetContentText = details.content
                    report.targetTitle = details.title
                    report.targetCategory = details.category ?: "general"
                } else {
                    report.targetContentText = "CONTEÚDO NÃO ENCONTRADO — POSSIVELMENTE EXCLUÍDO"
                }
            }

            val authorRole = report.targetAuthorRole
            val myRole = SocialProfileRepository.currentProfile?.role ?: "usuario"

            runOnUiThread {
                processarLogicaAnalise(report, status, currentUid, myRole, authorRole)
            }
        }
    }

    private fun processarLogicaAnalise(report: Report, status: String, currentUid: String?, myRole: String, authorRole: String?) {
        when (status) {
            "pending" -> {
                // Bloqueia claim se o autor for de cargo igual ou superior (UX)
                if (getRoleWeight(myRole) <= getRoleWeight(authorRole)) {
                    val msg = if (authorRole == null) "Carregando dados do autor..." else "Esta denúncia envolve um cargo superior ou igual."
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    return
                }

                AlertDialog.Builder(this)
                    .setTitle("Nova Denúncia")
                    .setMessage("Deseja assumir a análise desta denúncia?")
                    .setPositiveButton("Analisar") { _, _ -> assumirDenuncia(report.id) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            "reviewing" -> {
                val claimedModId = report.moderator_id
                val isOwner = claimedModId == currentUid
                val canSupervise = getRoleWeight(myRole) > 1 // Senior ou Admin podem tentar intervir

                if (isOwner || canSupervise) {
                    mostrarPainelDecisao(report, isOwner, myRole, false)
                } else {
                    Toast.makeText(this, "Esta denúncia está sendo analisada por outro moderador.", Toast.LENGTH_SHORT).show()
                }
            }
            "resolved", "dismissed" -> {
                mostrarPainelDecisao(report, false, myRole, true)
            }
            else -> {
                Toast.makeText(this, "Esta denúncia está em um estado desconhecido.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarPainelDecisao(report: Report, isOwner: Boolean, myRole: String, isReadOnly: Boolean) {
        val authorName = report.targetAuthorUsername ?: (if (report.targetAuthorId != null) "Desconhecido" else "AUTOR NÃO IDENTIFICADO")
        val authorCargo = traduzirRole(report.targetAuthorRole)
        
        val typeLabel = if (report.target_type == "discussion") "TÓPICO" else "RESPOSTA"
        val originLabel = when {
            report.targetCategory == "general" -> "FÓRUM"
            report.targetCategory != null -> "MURAL — ${report.targetCategory!!.uppercase()}"
            else -> "CONTEÚDO"
        }

        // Inflar layout customizado
        val dialogView = layoutInflater.inflate(com.typingfrontier.R.layout.dialog_report_analysis, null)
        
        // Identificar elementos
        val txtTitle = dialogView.findViewById<TextView>(com.typingfrontier.R.id.txtDialogTitle)
        val btnHelpGeneral = dialogView.findViewById<ImageButton>(com.typingfrontier.R.id.btnHelpGeneral)
        
        val txtReportMeta = dialogView.findViewById<TextView>(com.typingfrontier.R.id.txtReportMeta)
        val txtReporterInfo = dialogView.findViewById<TextView>(com.typingfrontier.R.id.txtReporterInfo)
        val txtAuthorDetails = dialogView.findViewById<TextView>(com.typingfrontier.R.id.txtAuthorDetails)
        val txtContentDetails = dialogView.findViewById<TextView>(com.typingfrontier.R.id.txtContentDetails)
        
        val layoutActionDelete = dialogView.findViewById<View>(com.typingfrontier.R.id.layoutActionDelete)
        val layoutActionResolve = dialogView.findViewById<View>(com.typingfrontier.R.id.layoutActionResolve)
        val layoutActionDismiss = dialogView.findViewById<View>(com.typingfrontier.R.id.layoutActionDismiss)
        val layoutActionBan = dialogView.findViewById<View>(com.typingfrontier.R.id.layoutActionBan)
        
        val btnDelete = dialogView.findViewById<android.widget.Button>(com.typingfrontier.R.id.btnExcluirConteudo)
        val btnResolve = dialogView.findViewById<android.widget.Button>(com.typingfrontier.R.id.btnResolverSemExcluir)
        val btnDismiss = dialogView.findViewById<android.widget.Button>(com.typingfrontier.R.id.btnDescartarDenuncia)
        val btnBan = dialogView.findViewById<android.widget.Button>(com.typingfrontier.R.id.btnBanirAutor)
        
        val btnHelpExcluir = dialogView.findViewById<ImageButton>(com.typingfrontier.R.id.btnHelpExcluir)
        val btnHelpResolver = dialogView.findViewById<ImageButton>(com.typingfrontier.R.id.btnHelpResolver)
        val btnHelpDescartar = dialogView.findViewById<ImageButton>(com.typingfrontier.R.id.btnHelpDescartar)
        val btnHelpBanir = dialogView.findViewById<ImageButton>(com.typingfrontier.R.id.btnHelpBanir)
        
        val txtUserBannedLabel = dialogView.findViewById<TextView>(com.typingfrontier.R.id.txtUserBannedLabel)

        // Preencher dados
        txtTitle.text = when {
            isReadOnly -> "Histórico de Denúncia"
            isOwner -> "Análise de Denúncia"
            else -> "Supervisão de Moderação"
        }

        txtReportMeta.text = "Origem: $originLabel\nMotivo: ${report.reason}\nData: ${report.created_at.take(16).replace("T", " ")}\nDescrição: ${report.description ?: "N/A"}"
        txtReporterInfo.text = "@${report.reporterUsername ?: "..."}"
        txtAuthorDetails.text = "Autor: @$authorName\nCargo: $authorCargo\nNível: ${report.targetAuthorLevel ?: "?"}"

        val contentDisplay = StringBuilder()
        if (report.target_type == "discussion" && !report.targetTitle.isNullOrBlank()) {
            contentDisplay.append("TÍTULO: ${report.targetTitle}\n\n")
        }
        contentDisplay.append(report.targetContentText ?: "Carregando conteúdo...")
        txtContentDetails.text = contentDisplay.toString()

        // Configurar Ajuda
        btnHelpGeneral.setOnClickListener { mostrarAjudaGeral() }
        btnHelpExcluir.setOnClickListener { mostrarAjudaAcao("EXCLUIR CONTEÚDO", "O conteúdo denunciado será removido permanentemente. A denúncia ficará como 'resolvida'. O autor NÃO é banido automaticamente por esta ação.") }
        btnHelpResolver.setOnClickListener { mostrarAjudaAcao("RESOLVER SEM EXCLUIR", "A denúncia é encerrada como tratada, mas o conteúdo permanece visível. Útil quando o conteúdo não quebra regras mas a situação foi resolvida.") }
        btnHelpDescartar.setOnClickListener { mostrarAjudaAcao("DESCARTAR DENÚNCIA", "A denúncia é considerada improcedente. O conteúdo permanece e nenhuma ação é tomada contra o autor.") }
        btnHelpBanir.setOnClickListener { mostrarAjudaAcao("BANIR AUTOR", "Aplica uma punição social ao autor (suspensão temporária ou permanente). O conteúdo NÃO é excluído automaticamente; você deve usar 'EXCLUIR CONTEÚDO' se desejar removê-lo.") }

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        if (isReadOnly) {
            layoutActionDelete.visibility = View.GONE
            layoutActionResolve.visibility = View.GONE
            layoutActionDismiss.visibility = View.GONE
            layoutActionBan.visibility = View.GONE
            builder.setPositiveButton("Fechar", null)
        }

        val dialog = builder.create()

        if (!isReadOnly) {
            // Lógica de Visibilidade e Ações
            val contentExists = report.targetContentText != null && 
                                report.targetContentText != "CONTEÚDO NÃO ENCONTRADO — POSSIVELMENTE EXCLUÍDO"

            val canModerate = getRoleWeight(myRole) > getRoleWeight(report.targetAuthorRole)

            if (contentExists && canModerate) {
                layoutActionDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Confirmar Exclusão")
                        .setMessage("Deseja realmente excluir este conteúdo definitivamente?")
                        .setPositiveButton("Excluir") { _, _ -> 
                            aplicarDecisao(report.id, "resolved", true)
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            } else if (!contentExists) {
                txtContentDetails.text = "CONTEÚDO JÁ REMOVIDO"
                txtContentDetails.setTextColor(android.graphics.Color.RED)
            }

            if (canModerate) {
                layoutActionBan.visibility = View.VISIBLE
                btnBan.setOnClickListener {
                    abrirFluxoBanimento(report)
                }
                
                // Verificar se o usuário já está banido
                scope.launch {
                    val authorId = report.targetAuthorId
                    if (authorId != null) {
                        val isBanned = ModerationRepository.isUserBanned(authorId)
                        if (isBanned) {
                            runOnUiThread { txtUserBannedLabel.visibility = View.VISIBLE }
                        }
                    }
                }
            }

            btnResolve.setOnClickListener {
                aplicarDecisao(report.id, "resolved", false)
                dialog.dismiss()
            }

            btnDismiss.setOnClickListener {
                aplicarDecisao(report.id, "dismissed", false)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun mostrarAjudaGeral() {
        AlertDialog.Builder(this)
            .setTitle("COMO FUNCIONA A MODERAÇÃO?")
            .setMessage("Uma denúncia é apenas uma solicitação de análise feita por um jogador. Ela NÃO significa automaticamente que o conteúdo é culpado ou que o autor será punido.\n\n" +
                    "ESTADOS:\n" +
                    "• PENDENTE: Aguardando análise.\n" +
                    "• EM ANÁLISE: Um moderador assumiu o caso.\n" +
                    "• RESOLVIDA: Ação tomada (exclusão ou apenas encerramento).\n" +
                    "• DESCARTADA: Denúncia improcedente.\n\n" +
                    "HIERARQUIA:\n" +
                    "• MODERADOR: Age sobre usuários.\n" +
                    "• SÊNIOR: Age sobre usuários e moderadores.\n" +
                    "• ADMIN: Age sobre todos (exceto outros admins).\n\n" +
                    "As ações 'EXCLUIR CONTEÚDO' e 'BANIR AUTOR' são independentES. Você pode fazer uma sem a outra.")
            .setPositiveButton("Entendi", null)
            .show()
    }

    private fun mostrarAjudaAcao(titulo: String, mensagem: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(titulo + ":\n\n" + mensagem)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun abrirFluxoBanimento(report: Report) {
        val authorId = report.targetAuthorId ?: return
        val username = report.targetAuthorUsername ?: "..."
        val authorRole = report.targetAuthorRole

        val dialogView = layoutInflater.inflate(com.typingfrontier.R.layout.dialog_manage_ban, null)
        // Ocultar busca pois já temos o usuário
        dialogView.findViewById<View>(com.typingfrontier.R.id.edtSearchUsername).visibility = View.GONE
        dialogView.findViewById<View>(com.typingfrontier.R.id.btnSearch).visibility = View.GONE
        
        val layoutUserInfo = dialogView.findViewById<View>(com.typingfrontier.R.id.layoutUserInfo)
        val txtUserDisplay = dialogView.findViewById<android.widget.TextView>(com.typingfrontier.R.id.txtUserDisplay)
        val txtUserRole = dialogView.findViewById<android.widget.TextView>(com.typingfrontier.R.id.txtUserRole)
        val txtUserStatus = dialogView.findViewById<android.widget.TextView>(com.typingfrontier.R.id.txtUserStatus)
        
        val layoutBanAction = dialogView.findViewById<View>(com.typingfrontier.R.id.layoutBanAction)
        val edtBanReason = dialogView.findViewById<android.widget.EditText>(com.typingfrontier.R.id.edtBanReason)
        val spinnerDuration = dialogView.findViewById<android.widget.Spinner>(com.typingfrontier.R.id.spinnerDuration)
        val btnExecuteBan = dialogView.findViewById<android.widget.Button>(com.typingfrontier.R.id.btnExecuteBan)

        val options = arrayOf("24 horas", "7 dias", "14 dias", "21 dias", "30 dias", "Permanente")
        spinnerDuration.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, options)

        txtUserDisplay.text = "@$username"
        txtUserRole.text = "Cargo: ${traduzirRole(authorRole)}"
        txtUserStatus.text = "Status: IDENTIFICADO"
        layoutUserInfo.visibility = View.VISIBLE
        layoutBanAction.visibility = View.VISIBLE

        val banDialog = AlertDialog.Builder(this)
            .setTitle("Banir Autor")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .create()

        btnExecuteBan.setOnClickListener {
            val reason = edtBanReason.text.toString().trim()
            if (reason.isEmpty()) {
                Toast.makeText(this, "Motivo obrigatório", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val durationIndex = spinnerDuration.selectedItemPosition
            
            AlertDialog.Builder(this)
                .setTitle("Confirmar")
                .setMessage("Banir @$username por ${options[durationIndex]}?")
                .setPositiveButton("Confirmar") { _, _ ->
                    isProcessing = true
                    scope.launch {
                        try {
                            val hours = when (durationIndex) {
                                0 -> 24
                                1 -> 168
                                2 -> 336
                                3 -> 504
                                4 -> 720
                                else -> null
                            }
                            ModerationRepository.banUser(authorId, reason, hours)
                            runOnUiThread {
                                Toast.makeText(this@ReportActivity, "Usuário banido.", Toast.LENGTH_SHORT).show()
                                banDialog.dismiss()
                            }
                        } catch (e: Exception) {
                            runOnUiThread { Toast.makeText(this@ReportActivity, "Erro ao banir.", Toast.LENGTH_SHORT).show() }
                        } finally {
                            isProcessing = false
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        banDialog.show()
    }

    private fun assumirDenuncia(reportId: String) {
        if (isProcessing) return
        isProcessing = true
        binding.progressBar.visibility = View.VISIBLE

        scope.launch {
            try {
                ModerationRepository.claimReport(reportId)
                Toast.makeText(this@ReportActivity, "Denúncia atribuída a você.", Toast.LENGTH_SHORT).show()
                carregarDenuncias()
            } catch (e: Exception) {
                Toast.makeText(this@ReportActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun aplicarDecisao(reportId: String, status: String, delete: Boolean) {
        if (isProcessing) return
        isProcessing = true
        binding.progressBar.visibility = View.VISIBLE

        scope.launch {
            try {
                ModerationRepository.resolveReport(reportId, status, delete)
                val msg = if (delete) "Conteúdo removido." else if (status == "resolved") "Denúncia resolvida." else "Denúncia descartada."
                runOnUiThread { Toast.makeText(this@ReportActivity, msg, Toast.LENGTH_SHORT).show() }
                carregarDenuncias()
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@ReportActivity, "Erro ao processar.", Toast.LENGTH_SHORT).show() }
            } finally {
                isProcessing = false
                binding.progressBar.visibility = View.GONE
            }
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

    private fun getRoleWeight(role: String?): Int {
        return when (role) {
            "administrator" -> 3
            "senior_moderator" -> 2
            "moderator" -> 1
            else -> 0
        }
    }
}
