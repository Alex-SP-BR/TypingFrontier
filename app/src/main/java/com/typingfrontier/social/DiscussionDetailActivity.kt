package com.typingfrontier.social

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.typingfrontier.databinding.ActivityDiscussionDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DiscussionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiscussionDetailBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var adapter: DiscussionReplyAdapter? = null
    
    private var discussionId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscussionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        discussionId = intent.getStringExtra("EXTRA_DISCUSSION_ID") ?: ""
        val title = intent.getStringExtra("EXTRA_TITLE") ?: ""
        val content = intent.getStringExtra("EXTRA_CONTENT") ?: ""
        val author = intent.getStringExtra("EXTRA_AUTHOR") ?: ""
        val level = intent.getIntExtra("EXTRA_LEVEL", 1)
        val authorId = intent.getStringExtra("EXTRA_AUTHOR_ID") ?: ""
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "general"

        if (category == "general") {
            // Modo Fórum Tradicional
            binding.txtDetailTitle.text = title
            binding.txtDetailAuthor.visibility = View.VISIBLE
            binding.txtDetailAuthor.text = "por @$author · Nível $level"
            binding.txtDetailAuthor.setOnClickListener {
                val pIntent = Intent(this, SocialProfileActivity::class.java)
                pIntent.putExtra("EXTRA_USER_ID", authorId)
                startActivity(pIntent)
            }
        } else {
            // Modo Mural Social
            binding.txtDetailTitle.text = "@$author · Nível $level"
            binding.txtDetailAuthor.visibility = View.GONE
            binding.txtDetailTitle.setOnClickListener {
                val pIntent = Intent(this, SocialProfileActivity::class.java)
                pIntent.putExtra("EXTRA_USER_ID", authorId)
                startActivity(pIntent)
            }
        }

        binding.txtDetailContent.text = content

        val currentDiscussion = Discussion(
            id = discussionId,
            category = category,
            title = title,
            content = content,
            authorId = authorId,
            authorProfile = AuthorProfile(author, level)
        )

        binding.btnDetailMenu.setOnClickListener {
            mostrarMenuModeracaoTopico(currentDiscussion)
        }

        // Adicionar Long Click no card do tópico para ações (Editar, Excluir, Denunciar)
        (binding.txtDetailTitle.parent as View).setOnLongClickListener {
            mostrarMenuModeracaoTopico(currentDiscussion)
            true
        }

        configurarRecycler()
        configurarBotoes()
        carregarRespostas()
    }

    private fun configurarRecycler() {
        binding.recyclerReplies.layoutManager = LinearLayoutManager(this)
    }

    private fun configurarBotoes() {
        binding.btnVoltar.setOnClickListener { finish() }
        binding.btnSendReply.setOnClickListener { enviarResposta() }
    }

    private fun carregarRespostas() {
        scope.launch {
            // Garante autenticação
            SocialProfileRepository.initializeSocialIdentity()
            SocialProfileRepository.awaitInitialization()

            try {
                val replies = DiscussionRepository.getReplies(discussionId)
                adapter = DiscussionReplyAdapter(replies, 
                    onItemLongClick = { reply ->
                        mostrarMenuModeracaoResposta(reply)
                    },
                    onAuthorClick = { profileId ->
                        val pIntent = Intent(this@DiscussionDetailActivity, SocialProfileActivity::class.java)
                        pIntent.putExtra("EXTRA_USER_ID", profileId)
                        startActivity(pIntent)
                    }
                )
                binding.recyclerReplies.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(this@DiscussionDetailActivity, "Não foi possível carregar as respostas.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarMenuModeracaoResposta(reply: DiscussionReply) {
        val profile = SocialProfileRepository.currentProfile
        val role = profile?.role ?: "usuario"
        val isAuthor = reply.authorId == profile?.id
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "general"
        val isForum = category == "general"

        val options = mutableListOf<String>()

        if (isAuthor) {
            // Ações tradicionais de edição/exclusão restritas ao Fórum Geral
            if (isForum) {
                options.add("Editar Resposta")
                options.add("Excluir Resposta")
            }
        } else {
            options.add("Denunciar Resposta")
        }

        options.add("Ver Perfil de @${reply.authorUsername}")

        // Hierarquia de Moderação (Visual)
        val canModerate = !isAuthor && (role == "moderator" || role == "senior_moderator" || role == "administrator")
        val targetRole = reply.authorProfile?.role ?: "usuario"

        if (canModerate) {
            val weightExecutor = getRoleWeight(role)
            val weightTarget = getRoleWeight(targetRole)
            
            if (weightExecutor > weightTarget) {
                options.add("Moderação: Analisar Resposta")
            }
        }

        if (role == "administrator") {
            options.add("Admin: Gerenciar Resposta")
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Opções da Resposta")
        builder.setItems(options.toTypedArray()) { _, which ->
            when (options[which]) {
                "Ver Perfil de @${reply.authorUsername}" -> {
                    val intent = Intent(this, SocialProfileActivity::class.java)
                    intent.putExtra("EXTRA_USER_ID", reply.authorId)
                    startActivity(intent)
                }
                "Editar Resposta" -> {
                    scope.launch {
                        if (ModerationRepository.isCurrentUserBanned()) {
                            Toast.makeText(this@DiscussionDetailActivity, "Sua conta está impedida.", Toast.LENGTH_SHORT).show()
                        } else {
                            mostrarDialogEditarResposta(reply)
                        }
                    }
                }
                "Excluir Resposta" -> {
                    scope.launch {
                        if (ModerationRepository.isCurrentUserBanned()) {
                            Toast.makeText(this@DiscussionDetailActivity, "Sua conta está impedida.", Toast.LENGTH_SHORT).show()
                        } else {
                            mostrarConfirmacaoExcluirResposta(reply.id!!)
                        }
                    }
                }
                "Denunciar Resposta" -> mostrarDialogDenuncia(reply.id!!, "reply")
                else -> Toast.makeText(this, "Selecionado: ${options[which]} (Em desenvolvimento)", Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }

    private fun mostrarMenuModeracaoTopico(discussion: Discussion) {
        val profile = SocialProfileRepository.currentProfile
        val role = profile?.role ?: "usuario"
        val isAuthor = discussion.authorId == profile?.id
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "general"
        val isForum = category == "general"

        val options = mutableListOf<String>()
        
        if (isAuthor) {
            if (isForum) {
                options.add("Editar Tópico")
                options.add("Excluir Tópico")
            }
        } else {
            options.add("Denunciar Tópico")
        }
        
        options.add("Ver Perfil de @${discussion.authorUsername}")

        // Hierarquia de Moderação (Visual)
        val canModerate = !isAuthor && (role == "moderator" || role == "senior_moderator" || role == "administrator")
        val targetRole = discussion.authorProfile?.role ?: "usuario"

        if (canModerate) {
            val weightExecutor = getRoleWeight(role)
            val weightTarget = getRoleWeight(targetRole)
            
            if (weightExecutor > weightTarget) {
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
                            Toast.makeText(this@DiscussionDetailActivity, "Sua conta está impedida.", Toast.LENGTH_SHORT).show()
                        } else {
                            mostrarDialogEditarTopico(discussion)
                        }
                    }
                }
                "Excluir Tópico" -> {
                    scope.launch {
                        if (ModerationRepository.isCurrentUserBanned()) {
                            Toast.makeText(this@DiscussionDetailActivity, "Sua conta está impedida.", Toast.LENGTH_LONG).show()
                        } else {
                            mostrarConfirmacaoExcluirTopico(discussion.id!!)
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

        scope.launch {
            try {
                val report = ModerationRepository.getReportByTarget("discussion", discussionId)
                
                if (report == null) {
                    runOnUiThread { 
                        Toast.makeText(this@DiscussionDetailActivity, "Este tópico não possui denúncias pendentes.", Toast.LENGTH_SHORT).show()
                        isProcessing = false
                    }
                    return@launch
                }

                val status = report.status.lowercase()
                val myUid = SocialProfileRepository.currentProfile?.id

                when (status) {
                    "pending" -> {
                        ModerationRepository.claimReport(report.id)
                        abrirRelatorio(report.id)
                    }
                    "reviewing" -> {
                        if (report.moderator_id == myUid) {
                            abrirRelatorio(report.id)
                        } else {
                            runOnUiThread { Toast.makeText(this@DiscussionDetailActivity, "Esta denúncia já está sendo analisada por outro moderador.", Toast.LENGTH_SHORT).show() }
                        }
                    }
                    "resolved", "dismissed" -> {
                        runOnUiThread { Toast.makeText(this@DiscussionDetailActivity, "Este caso já foi encerrado.", Toast.LENGTH_SHORT).show() }
                    }
                    else -> {
                        runOnUiThread { Toast.makeText(this@DiscussionDetailActivity, "Estado da denúncia desconhecido.", Toast.LENGTH_SHORT).show() }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@DiscussionDetailActivity, "Erro ao carregar análise.", Toast.LENGTH_SHORT).show() }
            } finally {
                isProcessing = false
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
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "general"
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
            val contentInput = edtContent.text.toString().trim()

            if (contentInput.isEmpty()) {
                Toast.makeText(this, "A mensagem não pode estar vazia", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (isForum && titleInput.isEmpty()) {
                Toast.makeText(this, "O título não pode estar vazio", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            scope.launch {
                try {
                    val newTitle = if (isForum) titleInput else {
                        if (contentInput.length > 50) contentInput.take(47) + "..." else contentInput
                    }
                    DiscussionRepository.updateDiscussion(discussion.id!!, newTitle, contentInput)
                    Toast.makeText(this@DiscussionDetailActivity, "Publicação atualizada!", Toast.LENGTH_SHORT).show()
                    
                    // Atualizar UI localmente
                    binding.txtDetailTitle.text = if (isForum) newTitle else "@${discussion.authorUsername} · Nível ${discussion.authorLevel}"
                    binding.txtDetailContent.text = contentInput
                } catch (e: Exception) {
                    Toast.makeText(this@DiscussionDetailActivity, "Erro ao atualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarConfirmacaoExcluirTopico(discussionId: String) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Tópico")
            .setMessage("Tem certeza que deseja excluir permanentemente este tópico e todas as suas respostas?")
            .setPositiveButton("Excluir") { _, _ ->
                scope.launch {
                    try {
                        DiscussionRepository.deleteDiscussion(discussionId)
                        Toast.makeText(this@DiscussionDetailActivity, "Tópico excluído.", Toast.LENGTH_SHORT).show()
                        finish() // Fecha a tela de detalhes pois o tópico não existe mais
                    } catch (e: Exception) {
                        Toast.makeText(this@DiscussionDetailActivity, "Erro ao excluir: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogEditarResposta(reply: DiscussionReply) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar Resposta")

        val edtContent = EditText(this)
        edtContent.setText(reply.content)
        edtContent.setPadding(50, 40, 50, 40)
        
        builder.setView(edtContent)

        builder.setPositiveButton("Salvar") { _, _ ->
            val content = edtContent.text.toString().trim()
            if (content.isEmpty()) return@setPositiveButton
            
            scope.launch {
                try {
                    if (ModerationRepository.isCurrentUserBanned()) {
                        Toast.makeText(this@DiscussionDetailActivity, "Sua conta está impedida.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    DiscussionRepository.updateReply(reply.id!!, content)
                    Toast.makeText(this@DiscussionDetailActivity, "Resposta atualizada!", Toast.LENGTH_SHORT).show()
                    carregarRespostas()
                } catch (e: Exception) {
                    Toast.makeText(this@DiscussionDetailActivity, "Erro ao atualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarConfirmacaoExcluirResposta(replyId: String) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Resposta")
            .setMessage("Deseja excluir permanentemente esta resposta?")
            .setPositiveButton("Excluir") { _, _ ->
                scope.launch {
                    try {
                        if (ModerationRepository.isCurrentUserBanned()) {
                            Toast.makeText(this@DiscussionDetailActivity, "Não foi possível excluir: sua conta está impedida.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        DiscussionRepository.deleteReply(replyId)
                        Toast.makeText(this@DiscussionDetailActivity, "Resposta excluída.", Toast.LENGTH_SHORT).show()
                        carregarRespostas()
                    } catch (e: Exception) {
                        Toast.makeText(this@DiscussionDetailActivity, "Erro ao excluir: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@DiscussionDetailActivity, "Denúncia enviada com sucesso.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@DiscussionDetailActivity, "Erro ao enviar denúncia: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun enviarResposta() {
        val profile = SocialProfileRepository.currentProfile
        if (profile == null) {
            Toast.makeText(this, "Identidade social não carregada.", Toast.LENGTH_SHORT).show()
            return
        }

        val content = binding.edtReply.text.toString().trim()
        if (content.isEmpty()) return

        if (content.length > 2000) {
            Toast.makeText(this, "Resposta muito longa", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                if (ModerationRepository.isCurrentUserBanned()) {
                    Toast.makeText(this@DiscussionDetailActivity, "Sua conta está impedida de participar do fórum.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val newReply = DiscussionReply(
                    discussionId = discussionId,
                    authorId = profile.id,
                    content = content
                )
                DiscussionRepository.createReply(newReply)
                binding.edtReply.setText("")
                carregarRespostas()
            } catch (e: Exception) {
                Toast.makeText(this@DiscussionDetailActivity, "Erro ao responder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
