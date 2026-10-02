package com.typingfrontier.social

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import com.typingfrontier.station.VipManager
import com.typingfrontier.social.PresenceManager
import com.typingfrontier.social.PrivateMessageRepository
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.typingfrontier.R
import com.typingfrontier.databinding.ActivityDiscussionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiscussionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiscussionBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var adapter: DiscussionAdapter? = null
    private var category: String = "general"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscussionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        category = intent.getStringExtra("EXTRA_CATEGORY") ?: "general"
        
        val displayTitle = when(category) {
            "general" -> "Fórum Geral"
            "level" -> "Mural: Nível"
            "strength" -> "Mural: Força"
            "resistance" -> "Mural: Resistência"
            "speed" -> "Mural: Velocidade"
            "intelligence" -> "Mural: Inteligência"
            "charisma" -> "Mural: Carisma"
            "adventures_completed" -> "Mural: Aventuras Concluídas"
            "best_streak" -> "Mural: Melhor Sequência de Acertos"
            else -> "Mural: ${category.replaceFirstChar { it.uppercase() }}"
        }
        binding.txtForumTitle.text = "💬 $displayTitle"

        configurarBackground()
        configurarRecycler()
        configurarBotoes()
        carregarDiscussoes()

        scope.launch {
            SocialProfileRepository.initializeSocialIdentity()
            SocialProfileRepository.awaitInitialization()
            val currentUserId = SocialProfileRepository.getCurrentUserId()
            if (currentUserId != null) {
                PresenceManager.startPresence()
            }
        }

        scope.launch {
            PrivateMessageRepository.incomingMessages.collect { msg ->
                val currentUserId = SocialProfileRepository.getCurrentUserId() ?: return@collect
                if (msg.senderId == currentUserId) return@collect

                if (msg.id != null && activeDisplayedMessageIds.contains(msg.id)) {
                    return@collect
                }
                msg.id?.let { activeDisplayedMessageIds.add(it) }

                val senderName = PresenceManager.onlineUsers.value.find { it.user_id == msg.senderId }?.username
                    ?: VipManager.getVipList(this@DiscussionActivity).find { it.userId == msg.senderId }?.username
                    ?: "Viajante"

                val existingSession = activeChatSessions[msg.senderId]
                if (existingSession != null) {
                    existingSession.lastReceivedMessage = msg
                    if (existingSession.historyLayout != null && existingSession.scrollView != null) {
                        adicionarMensagemAoHistorico(this@DiscussionActivity, existingSession.historyLayout!!, existingSession.scrollView!!, senderName, msg.message, msg, existingSession.targetUsername)
                    }
                    if (existingSession.isMinimized) {
                        existingSession.unreadCount++
                        val unreadSuffix = if (existingSession.unreadCount > 0) " (${existingSession.unreadCount})" else ""
                        existingSession.minimizedBar?.text = "$senderName$unreadSuffix"
                    }
                } else {
                    abrirPainelChatPrivado(msg.senderId, senderName, initialMessage = msg, startMinimized = true)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PresenceManager.stopPresence()
    }

    private fun configurarRecycler() {
        binding.recyclerDiscussions.layoutManager = LinearLayoutManager(this)
        binding.swipeRefresh.setOnRefreshListener { carregarDiscussoes() }
    }

    private fun configurarBackground() {
        if (category == "general") {
            binding.imgDiscussionBg.setImageResource(R.drawable.bg_forum)
            binding.viewDiscussionOverlay.visibility = View.GONE
        } else {
            binding.imgDiscussionBg.setImageResource(R.drawable.bg_ranking)
            binding.viewDiscussionOverlay.visibility = View.VISIBLE
            binding.viewDiscussionOverlay.setBackgroundColor(android.graphics.Color.parseColor("#4D000000"))
        }
    }

    private fun configurarBotoes() {
        binding.btnVoltar.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { carregarDiscussoes() }
        binding.btnVipForum.setOnClickListener { mostrarDialogoVip() }
        binding.fabNewTopic.setOnClickListener { mostrarDialogNovoTopico() }
    }

    private fun carregarDiscussoes() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = true

        scope.launch {
            // Garante autenticação antes da consulta
            SocialProfileRepository.initializeSocialIdentity()
            SocialProfileRepository.awaitInitialization()

            try {
                // Tentativa cirúrgica de carregamento com retry para falhas transitórias
                val result = try {
                    DiscussionRepository.getDiscussions(category)
                } catch (e: Exception) {
                    // Pequena pausa para estabilização da conexão e nova tentativa
                    kotlinx.coroutines.delay(1000)
                    DiscussionRepository.getDiscussions(category)
                }
                exibirDiscussoes(result)
            } catch (e: Exception) {
                Toast.makeText(this@DiscussionActivity, "Fórum temporariamente indisponível. Verifique sua conexão.", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun exibirDiscussoes(list: List<Discussion>) {
        if (list.isEmpty()) {
            Toast.makeText(this, "Nenhum tópico encontrado.", Toast.LENGTH_SHORT).show()
        }
        adapter = DiscussionAdapter(list, category,
            onItemClick = { discussion ->
                val intent = Intent(this, DiscussionDetailActivity::class.java)
                intent.putExtra("EXTRA_DISCUSSION_ID", discussion.id)
                intent.putExtra("EXTRA_TITLE", discussion.title)
                intent.putExtra("EXTRA_CONTENT", discussion.content)
                intent.putExtra("EXTRA_AUTHOR", discussion.authorUsername)
                intent.putExtra("EXTRA_LEVEL", discussion.authorLevel)
                intent.putExtra("EXTRA_AUTHOR_ID", discussion.authorId)
                intent.putExtra("EXTRA_CATEGORY", category)
                startActivity(intent)
            },
            onItemLongClick = { discussion ->
                mostrarMenuModeracao(discussion)
            },
            onAuthorClick = { profileId ->
                val intent = Intent(this, SocialProfileActivity::class.java)
                intent.putExtra("EXTRA_USER_ID", profileId)
                startActivity(intent)
            }
        )
        binding.recyclerDiscussions.adapter = adapter
    }

    private fun mostrarMenuModeracao(discussion: Discussion) {
        val profile = SocialProfileRepository.currentProfile
        val role = profile?.role ?: "usuario"
        val isAuthor = discussion.authorId == profile?.id
        val isForum = category == "general"

        val options = mutableListOf<String>()
        
        if (isAuthor) {
            // Apenas Fórum Geral tem ações tradicionais de edição/exclusão por enquanto
            if (isForum) {
                options.add("Editar Tópico")
                options.add("Excluir Tópico")
            }
        } else {
            options.add("Denunciar Tópico")
        }
        
        options.add("Ver Perfil de @${discussion.authorUsername}")

        // Hierarquia de Moderação (Visual)
        val isOwnerException = profile?.id == SocialProfileRepository.OWNER_UUID_EXCEPTION
        val canModerate = (!isAuthor || isOwnerException) && (role == "moderator" || role == "senior_moderator" || role == "administrator")
        val targetRole = discussion.authorProfile?.role ?: "usuario"
        
        if (canModerate) {
            val weightExecutor = getRoleWeight(role)
            val weightTarget = getRoleWeight(targetRole)
            
            // Só mostra opções se o executor for estritamente superior ao autor ou for a exceção do proprietário
            if (isOwnerException || weightExecutor > weightTarget) {
                options.add("Moderação: Analisar Tópico")
            }
        }
        

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Opções do Tópico")
        builder.setItems(options.toTypedArray()) { _, which ->
            when (options[which]) {
                "Ver Perfil de @${discussion.authorUsername}" -> {
                    val intent = Intent(this, SocialProfileActivity::class.java)
                    intent.putExtra("EXTRA_USER_ID", discussion.authorId)
                    startActivity(intent)
                }
                "Editar Tópico" -> {
                    scope.launch {
                        if (ModerationRepository.isCurrentUserBanned()) {
                            Toast.makeText(this@DiscussionActivity, "Sua conta está impedida de participar do fórum.", Toast.LENGTH_LONG).show()
                        } else {
                            mostrarDialogEditarTopico(discussion)
                        }
                    }
                }
                "Excluir Tópico" -> {
                    scope.launch {
                        if (ModerationRepository.isCurrentUserBanned()) {
                            Toast.makeText(this@DiscussionActivity, "Sua conta está impedida.", Toast.LENGTH_LONG).show()
                        } else {
                            mostrarConfirmacaoExcluir(discussion.id!!)
                        }
                    }
                }
                "Denunciar Tópico" -> mostrarDialogDenuncia(discussion.id!!, "discussion")
                "Moderação: Analisar Tópico" -> analisarTopico(discussion.id!!)
                else -> Toast.makeText(this, "Selecionado: ${options[which]} (Em desenvolvimento)", Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }

    private fun analisarTopico(discussionId: String) {
        if (isProcessing) return
        isProcessing = true
        binding.progressBar.visibility = View.VISIBLE

        scope.launch {
            try {
                val report = ModerationRepository.getReportByTarget("discussion", discussionId)
                
                if (report == null) {
                    runOnUiThread { 
                        Toast.makeText(this@DiscussionActivity, "Este tópico não possui denúncias pendentes.", Toast.LENGTH_SHORT).show()
                        isProcessing = false
                        binding.progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                val status = report.status.lowercase()
                val myUid = SocialProfileRepository.currentProfile?.id

                when (status) {
                    "pending" -> {
                        // Faz o claim automático e abre
                        ModerationRepository.claimReport(report.id)
                        abrirRelatorio(report.id)
                    }
                    "reviewing" -> {
                        if (report.moderator_id == myUid) {
                            abrirRelatorio(report.id)
                        } else {
                            runOnUiThread { Toast.makeText(this@DiscussionActivity, "Esta denúncia já está sendo analisada por outro moderador.", Toast.LENGTH_SHORT).show() }
                        }
                    }
                    "resolved", "dismissed" -> {
                        runOnUiThread { Toast.makeText(this@DiscussionActivity, "Este caso já foi encerrado.", Toast.LENGTH_SHORT).show() }
                    }
                    else -> {
                        runOnUiThread { Toast.makeText(this@DiscussionActivity, "Estado da denúncia desconhecido.", Toast.LENGTH_SHORT).show() }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@DiscussionActivity, "Erro ao carregar análise.", Toast.LENGTH_SHORT).show() }
            } finally {
                isProcessing = false
                runOnUiThread { binding.progressBar.visibility = View.GONE }
            }
        }
    }

    private fun abrirRelatorio(reportId: String) {
        val intent = Intent(this, ReportActivity::class.java)
        intent.putExtra("EXTRA_REPORT_ID", reportId)
        startActivity(intent)
    }

    private var isProcessing = false

    private fun getRoleWeight(role: String?): Int {
        return when (role) {
            "administrator" -> 3
            "senior_moderator" -> 2
            "moderator" -> 1
            else -> 0
        }
    }

    private fun mostrarDialogEditarTopico(discussion: Discussion) {
        val builder = AlertDialog.Builder(this)
        val isForum = category == "general"
        builder.setTitle(if (isForum) "Editar Tópico" else "Editar Publicação")

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val edtTitle = EditText(this)
        if (isForum) {
            edtTitle.hint = "Título"
            edtTitle.setText(discussion.title)
            layout.addView(edtTitle)
        }

        val edtContent = EditText(this)
        edtContent.hint = "Conteúdo"
        edtContent.minLines = 3
        edtContent.setText(discussion.content)
        layout.addView(edtContent)

        builder.setView(layout)

        builder.setPositiveButton("Salvar") { _, _ ->
            val titleInput = edtTitle.text.toString().trim()
            val content = edtContent.text.toString().trim()

            if (content.isEmpty()) {
                Toast.makeText(this, "A mensagem não pode estar vazia", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (isForum && titleInput.isEmpty()) {
                Toast.makeText(this, "O título não pode estar vazio", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            scope.launch {
                try {
                    if (ModerationRepository.isCurrentUserBanned()) {
                        Toast.makeText(this@DiscussionActivity, "Sua conta está impedida.", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val newTitle = if (isForum) titleInput else {
                        if (content.length > 50) content.take(47) + "..." else content
                    }
                    DiscussionRepository.updateDiscussion(discussion.id!!, newTitle, content)
                    Toast.makeText(this@DiscussionActivity, "Publicação atualizada!", Toast.LENGTH_SHORT).show()
                    carregarDiscussoes()
                } catch (e: Exception) {
                    Toast.makeText(this@DiscussionActivity, "Erro ao atualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarConfirmacaoExcluir(discussionId: String) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Tópico")
            .setMessage("Tem certeza que deseja excluir permanentemente este tópico e todas as suas respostas?")
            .setPositiveButton("Excluir") { _, _ ->
                scope.launch {
                    try {
                        if (ModerationRepository.isCurrentUserBanned()) {
                            Toast.makeText(this@DiscussionActivity, "Não foi possível excluir: sua conta está impedida.", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        DiscussionRepository.deleteDiscussion(discussionId)
                        Toast.makeText(this@DiscussionActivity, "Tópico excluído.", Toast.LENGTH_SHORT).show()
                        carregarDiscussoes()
                    } catch (e: Exception) {
                        Toast.makeText(this@DiscussionActivity, "Erro ao excluir: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogDenuncia(targetId: String, targetType: String) {
        val reasons = arrayOf("Spam", "Conteúdo ofensivo", "Assédio", "Conteúdo inadequado", "Outro")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Denunciar Conteúdo")
        
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val spinner = android.widget.Spinner(this)
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, reasons)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        layout.addView(spinner)

        val edtDesc = EditText(this)
        edtDesc.hint = "Descrição opcional (máx 200)"
        layout.addView(edtDesc)

        builder.setView(layout)

        builder.setPositiveButton("Enviar") { _, _ ->
            val reason = reasons[spinner.selectedItemPosition]
            val desc = edtDesc.text.toString().trim()
            
            scope.launch {
                try {
                    ModerationRepository.createReport(targetType, targetId, reason, desc.ifEmpty { null })
                    Toast.makeText(this@DiscussionActivity, "Denúncia enviada com sucesso.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@DiscussionActivity, "Erro ao enviar denúncia: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarDialogNovoTopico() {
        val profile = SocialProfileRepository.currentProfile
        if (profile == null) {
            Toast.makeText(this, "Identidade social não carregada.", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            if (ModerationRepository.isCurrentUserBanned()) {
                Toast.makeText(this@DiscussionActivity, "Sua conta está impedida de participar do fórum.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val isForum = category == "general"
            val dialogView = layoutInflater.inflate(R.layout.dialog_create_discussion, null)
            
            val txtDialogTitle = dialogView.findViewById<TextView>(R.id.txtDialogTitle)
            val lblTitle = dialogView.findViewById<TextView>(R.id.lblTitle)
            val edtTitle = dialogView.findViewById<EditText>(R.id.edtTitle)
            val edtContent = dialogView.findViewById<EditText>(R.id.edtContent)
            val btnPublish = dialogView.findViewById<Button>(R.id.btnPublish)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

            txtDialogTitle.text = if (isForum) "Novo Tópico" else "Publicar no Mural"
            
            if (!isForum) {
                lblTitle.visibility = View.GONE
                edtTitle.visibility = View.GONE
            }

            val dialog = AlertDialog.Builder(this@DiscussionActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(dialogView)
                .create()

            btnPublish.setOnClickListener {
                val titleInput = edtTitle.text.toString().trim()
                val content = edtContent.text.toString().trim()

                if (isForum && titleInput.isEmpty()) {
                    Toast.makeText(this@DiscussionActivity, "Digite um título", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (content.isEmpty()) {
                    Toast.makeText(this@DiscussionActivity, "Escreva algo para publicar", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (content.length > 2000) {
                    Toast.makeText(this@DiscussionActivity, "Texto muito longo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val title = if (isForum) titleInput else {
                    if (content.length > 50) content.take(47) + "..." else content
                }

                salvarNovoTopico(title, content, profile)
                dialog.dismiss()
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun salvarNovoTopico(title: String, content: String, profile: SocialProfile) {
        scope.launch {
            try {
                // Verificação dupla de banimento antes de persistir
                if (ModerationRepository.isCurrentUserBanned()) {
                    Toast.makeText(this@DiscussionActivity, "Não foi possível publicar: sua conta está impedida.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val newDiscussion = Discussion(
                    category = category,
                    title = title,
                    content = content,
                    authorId = profile.id
                )
                DiscussionRepository.createDiscussion(newDiscussion)
                Toast.makeText(this@DiscussionActivity, "Tópico publicado!", Toast.LENGTH_SHORT).show()
                carregarDiscussoes()
            } catch (e: Exception) {
                Toast.makeText(this@DiscussionActivity, "Erro ao publicar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private class PrivateChatSession(
        val targetUserId: String,
        val targetUsername: String,
        var panel: View? = null,
        var historyLayout: LinearLayout? = null,
        var scrollView: ScrollView? = null,
        var minimizedBar: TextView? = null,
        var isMinimized: Boolean = false,
        var unreadCount: Int = 0,
        var lastReceivedMessage: PrivateMessage? = null
    )

    private val activeChatSessions = mutableMapOf<String, PrivateChatSession>()
    private val activeDisplayedMessageIds = mutableSetOf<String>()

    private fun adicionarMensagemAoHistorico(
        context: Context,
        historyLayout: LinearLayout,
        scrollView: ScrollView,
        senderLabel: String,
        messageText: String,
        msg: PrivateMessage?,
        targetUsername: String
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 4)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tv = TextView(context).apply {
            text = "$senderLabel: $messageText"
            setTextColor(Color.parseColor("#E8EDF2"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(tv)

        if (msg != null && msg.id != null) {
            val btnReportMsg = TextView(context).apply {
                text = "▷"
                textSize = 13f
                setTextColor(Color.parseColor("#889099"))
                setPadding(12, 4, 4, 4)
                setOnClickListener {
                    mostrarDialogDenunciaMensagemPrivada(msg, targetUsername)
                }
            }
            row.addView(btnReportMsg)
        }

        historyLayout.addView(row)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun abrirPainelChatPrivado(
        targetUserId: String,
        targetUsername: String,
        initialMessage: PrivateMessage? = null,
        startMinimized: Boolean = false
    ) {
        val existingSession = activeChatSessions[targetUserId]
        if (existingSession != null) {
            if (initialMessage != null) existingSession.lastReceivedMessage = initialMessage
            if (existingSession.isMinimized && !startMinimized) {
                existingSession.minimizedBar?.let {
                    (it.parent as? ViewGroup)?.removeView(it)
                    existingSession.minimizedBar = null
                }
                existingSession.isMinimized = false
                existingSession.unreadCount = 0
                existingSession.panel?.visibility = View.VISIBLE
            } else if (existingSession.isMinimized && startMinimized) {
                if (initialMessage != null) {
                    if (existingSession.historyLayout != null && existingSession.scrollView != null) {
                        adicionarMensagemAoHistorico(this, existingSession.historyLayout!!, existingSession.scrollView!!, targetUsername, initialMessage.message, initialMessage, targetUsername)
                    }
                    existingSession.unreadCount++
                    val unreadSuffix = if (existingSession.unreadCount > 0) " (${existingSession.unreadCount})" else ""
                    existingSession.minimizedBar?.text = "$targetUsername$unreadSuffix"
                }
            }
            return
        }

        val context = this
        val currentUserId = SocialProfileRepository.getCurrentUserId() ?: return

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E6121212"))
            setPadding(16, 16, 16, 16)
            elevation = 16f
            layoutParams = CoordinatorLayout.LayoutParams(
                (320 * resources.displayMetrics.density).toInt(),
                (210 * resources.displayMetrics.density).toInt()
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        val session = PrivateChatSession(
            targetUserId = targetUserId,
            targetUsername = targetUsername,
            panel = panel,
            unreadCount = if (startMinimized && initialMessage != null) 1 else 0,
            lastReceivedMessage = initialMessage
        )

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 8)
            setBackgroundColor(Color.parseColor("#286680"))
        }

        val onlineSet = PresenceManager.onlineUsers.value.map { it.user_id }.toSet()
        val isOnline = onlineSet.contains(targetUserId)

        val indicator = TextView(context).apply {
            text = "●"
            textSize = 14f
            setTextColor(if (isOnline) Color.parseColor("#4CAF50") else Color.parseColor("#889099"))
            setPadding(0, 0, 8, 0)
        }
        header.addView(indicator)

        val titleTv = TextView(context).apply {
            text = targetUsername
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(titleTv)

        val btnHelpChat = Button(context).apply {
            text = "?"
            textSize = 12f
            setTextColor(Color.CYAN)
            setBackgroundColor(Color.TRANSPARENT)
            minWidth = 0
            minHeight = 0
            setPadding(12, 0, 12, 0)
            setOnClickListener {
                AlertDialog.Builder(context, R.style.Theme_TypingFrontier_AdminDialog)
                    .setTitle("Ajuda - Denunciar Mensagem")
                    .setMessage("Para denunciar uma mensagem específica, toque no símbolo ▷ localizado ao lado daquela mensagem.")
                    .setPositiveButton("Entendi", null)
                    .show()
            }
        }
        header.addView(btnHelpChat)

        val btnMinimize = Button(context).apply {
            text = "_"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            minWidth = 0
            minHeight = 0
            setPadding(12, 0, 12, 0)
            setOnClickListener {
                toggleMinimizeChat(session)
            }
        }
        header.addView(btnMinimize)

        val btnClose = Button(context).apply {
            text = "X"
            textSize = 12f
            setTextColor(Color.parseColor("#FF5252"))
            setBackgroundColor(Color.TRANSPARENT)
            minWidth = 0
            minHeight = 0
            setPadding(12, 0, 12, 0)
            setOnClickListener {
                panel.let { (it.parent as? ViewGroup)?.removeView(it) }
                session.minimizedBar?.let { (it.parent as? ViewGroup)?.removeView(it) }
                activeChatSessions.remove(targetUserId)
            }
        }
        header.addView(btnClose)
        panel.addView(header)

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply { topMargin = 8.0f.toInt(); bottomMargin = 8.0f.toInt() }
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(8, 8, 8, 8)
        }

        val historyLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        scrollView.addView(historyLayout)
        panel.addView(scrollView)

        session.historyLayout = historyLayout
        session.scrollView = scrollView

        if (initialMessage != null) {
            adicionarMensagemAoHistorico(context, historyLayout, scrollView, targetUsername, initialMessage.message, initialMessage, targetUsername)
        }

        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val input = EditText(context).apply {
            hint = "Digite sua mensagem..."
            setTextColor(Color.BLACK)
            setHintTextColor(Color.parseColor("#889099"))
            setBackgroundColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        footer.addView(input)

        val btnSend = Button(context).apply {
            text = "Enviar"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#286680"))
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    input.setText("")
                    scope.launch {
                        try {
                            val sentMsg = PrivateMessageRepository.sendMessage(currentUserId, targetUserId, text)
                            sentMsg.id?.let { activeDisplayedMessageIds.add(it) }
                            adicionarMensagemAoHistorico(context, historyLayout, scrollView, "Você", sentMsg.message, sentMsg, targetUsername)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao enviar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        footer.addView(btnSend)
        panel.addView(footer)

        var dX = 0f
        var dY = 0f
        header.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (isTouchInside(btnMinimize, event.rawX, event.rawY) || isTouchInside(btnClose, event.rawX, event.rawY)) {
                    return@setOnTouchListener false
                }
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = panel.x - event.rawX
                    dY = panel.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    var newX = event.rawX + dX
                    var newY = event.rawY + dY
                    val parent = panel.parent as? View ?: return@setOnTouchListener false
                    newX = newX.coerceIn(0f, (parent.width - panel.width).toFloat().coerceAtLeast(0f))
                    newY = newY.coerceIn(0f, (parent.height - panel.height).toFloat().coerceAtLeast(0f))
                    panel.x = newX
                    panel.y = newY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    header.performClick()
                    true
                }
                else -> false
            }
        }

        activeChatSessions[targetUserId] = session
        binding.root.addView(panel)

        if (startMinimized) {
            toggleMinimizeChat(session)
        }
    }

    private fun mostrarDialogDenunciaMensagemPrivada(msg: PrivateMessage, targetUsername: String) {
        if (msg.id == null) {
            Toast.makeText(this, "ID da mensagem inválido para denúncia.", Toast.LENGTH_SHORT).show()
            return
        }

        val reasons = arrayOf("Spam / Propaganda", "Ofensas / Assédio", "Conteúdo Inadequado", "Outros")
        val builder = AlertDialog.Builder(this, R.style.Theme_TypingFrontier_AdminDialog)
            .setTitle("Denunciar Mensagem Privada")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val txtPreview = TextView(this).apply {
            text = "Mensagem de @$targetUsername:\n\"${msg.message}\""
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(txtPreview)

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@DiscussionActivity, android.R.layout.simple_spinner_dropdown_item, reasons)
        }
        layout.addView(spinner)

        val edtDesc = EditText(this).apply {
            hint = "Descrição opcional (máx 200)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        layout.addView(edtDesc)

        builder.setView(layout)
        builder.setPositiveButton("Enviar Denúncia") { _, _ ->
            val reason = reasons[spinner.selectedItemPosition]
            val desc = edtDesc.text.toString().trim()
            scope.launch {
                try {
                    ModerationRepository.createReport("private_message", msg.id, reason, desc.ifEmpty { null })
                    Toast.makeText(this@DiscussionActivity, "Denúncia enviada com sucesso.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@DiscussionActivity, "Erro ao enviar denúncia: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun isTouchInside(view: View, rawX: Float, rawY: Float): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        return rawX >= x.toFloat() && rawX <= (x + view.width).toFloat() && rawY >= y.toFloat() && rawY <= (y + view.height).toFloat()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun toggleMinimizeChat(session: PrivateChatSession) {
        val panel = session.panel ?: return
        if (!session.isMinimized) {
            session.isMinimized = true
            panel.visibility = View.GONE
            val context = this
            val bar = TextView(context).apply {
                val unreadSuffix = if (session.unreadCount > 0) " (${session.unreadCount})" else ""
                text = "${session.targetUsername}$unreadSuffix"
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setBackgroundColor(Color.parseColor("#286680"))
                setPadding(24, 12, 24, 12)
                elevation = 16f
                layoutParams = CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    val activeBarIndex = activeChatSessions.values.count { it.minimizedBar != null }
                    val baseMarginDp = (24 * resources.displayMetrics.density).toInt()
                    val offsetYDp = (activeBarIndex * 48 * resources.displayMetrics.density).toInt()
                    setMargins(baseMarginDp, baseMarginDp, baseMarginDp, baseMarginDp + offsetYDp)
                }

                var dX = 0f
                var dY = 0f
                var isDragging = false
                val touchSlop = 10f
                var startX = 0f
                var startY = 0f

                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            dX = v.x - event.rawX
                            dY = v.y - event.rawY
                            startX = event.rawX
                            startY = event.rawY
                            isDragging = false
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = Math.abs(event.rawX - startX)
                            val deltaY = Math.abs(event.rawY - startY)
                            if (deltaX > touchSlop || deltaY > touchSlop) {
                                isDragging = true
                            }
                            if (isDragging) {
                                var newX = event.rawX + dX
                                var newY = event.rawY + dY
                                val parent = v.parent as? View ?: return@setOnTouchListener false
                                newX = newX.coerceIn(0f, (parent.width - v.width).toFloat().coerceAtLeast(0f))
                                newY = newY.coerceIn(0f, (parent.height - v.height).toFloat().coerceAtLeast(0f))
                                v.x = newX
                                v.y = newY
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!isDragging) {
                                v.performClick()
                            }
                            true
                        }
                        else -> false
                    }
                }

                setOnClickListener {
                    (parent as? ViewGroup)?.removeView(this)
                    session.minimizedBar = null
                    session.isMinimized = false
                    session.panel?.visibility = View.VISIBLE
                    session.unreadCount = 0
                }
            }
            session.minimizedBar = bar
            binding.root.addView(bar)
        }
    }

    private fun mostrarDialogoVip() {
        val context = this
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (100 * resources.displayMetrics.density).toInt()
            ).apply { topMargin = 16.0f.toInt() }
        }

        val listLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(listLayout)

        var dialog: AlertDialog? = null
        var refreshList: () -> Unit = {}

        refreshList = fun() {
            listLayout.removeAllViews()
            val vipList = VipManager.getVipList(context)
            val onlineSet = PresenceManager.onlineUsers.value.map { it.user_id }.toSet()
            
            // Ordena: online primeiro
            val sortedVips = vipList.sortedByDescending { onlineSet.contains(it.userId) }

            dialog?.setTitle("⭐ Lista VIP (${vipList.size}/50)")

            if (sortedVips.isEmpty()) {
                val emptyTv = TextView(context).apply {
                    text = "Sua lista VIP está vazia."
                    setTextColor(Color.parseColor("#889099"))
                    setPadding(0, 24, 0, 24)
                    gravity = Gravity.CENTER
                }
                listLayout.addView(emptyTv)
                return
            }

            sortedVips.forEach { vip ->
                val isOnline = onlineSet.contains(vip.userId)
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        abrirPainelChatPrivado(vip.userId, vip.username)
                        dialog?.dismiss()
                    }
                }

                val indicator = TextView(context).apply {
                    text = "●"
                    textSize = 16f
                    setTextColor(if (isOnline) Color.parseColor("#4CAF50") else Color.parseColor("#889099"))
                    setPadding(0, 0, 16, 0)
                }
                row.addView(indicator)

                val nameTv = TextView(context).apply {
                    text = vip.username
                    textSize = 16f
                    setTextColor(Color.BLACK)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(nameTv)

                val btnRemove = Button(context).apply {
                    text = "Remover"
                    textSize = 12f
                    setTextColor(Color.parseColor("#FF5252"))
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        VipManager.removeVip(context, vip.userId)
                        refreshList()
                    }
                }
                row.addView(btnRemove)

                listLayout.addView(row)
            }
        }

        val btnAdd = Button(context).apply {
            text = "+ Adicionar VIP"
            setBackgroundColor(Color.parseColor("#286680"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                mostrarDialogoAdicionarVip {
                    refreshList()
                }
            }
        }
        container.addView(btnAdd)
        container.addView(scrollView)

        refreshList()

        dialog = AlertDialog.Builder(context)
            .setTitle("⭐ Lista VIP (${VipManager.getVipList(this).size}/50)")
            .setView(container)
            .setPositiveButton("Fechar", null)
            .create()

        dialog.show()

        val presenceJob = scope.launch {
            PresenceManager.onlineUsers.collect {
                refreshList()
            }
        }
        dialog.setOnDismissListener {
            presenceJob.cancel()
        }
    }

    private fun mostrarDialogoAdicionarVip(onAdded: () -> Unit) {
        val input = EditText(this).apply {
            hint = "Digite o username..."
            setTextColor(Color.BLACK)
            setHintTextColor(Color.parseColor("#889099"))
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Adicionar VIP")
            .setView(input)
            .setPositiveButton("Adicionar") { _, _ ->
                val usernameInput = input.text.toString().trim().removePrefix("@")
                if (usernameInput.isEmpty()) return@setPositiveButton

                val currentUserId = SocialProfileRepository.getCurrentUserId() ?: ""
                
                scope.launch {
                    try {
                        val profile = ModerationRepository.getProfileByUsername(usernameInput)
                        if (profile == null) {
                            Toast.makeText(this@DiscussionActivity, "Jogador não encontrado.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        if (profile.id == currentUserId) {
                            Toast.makeText(this@DiscussionActivity, "Você não pode adicionar a si mesmo.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val added = VipManager.addVip(this@DiscussionActivity, profile.id, profile.username)
                        if (!added) {
                            Toast.makeText(this@DiscussionActivity, "Lista cheia (máx 50) ou jogador já adicionado.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@DiscussionActivity, "${profile.username} adicionado aos VIPs!", Toast.LENGTH_SHORT).show()
                            onAdded()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@DiscussionActivity, "Erro ao buscar jogador: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
