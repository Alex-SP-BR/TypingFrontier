package com.typingfrontier.social

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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

        configurarRecycler()
        configurarBotoes()
        carregarDiscussoes()
    }

    private fun configurarRecycler() {
        binding.recyclerDiscussions.layoutManager = LinearLayoutManager(this)
        binding.swipeRefresh.setOnRefreshListener { carregarDiscussoes() }
    }

    private fun configurarBotoes() {
        binding.btnVoltar.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { carregarDiscussoes() }
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
        val canModerate = !isAuthor && (role == "moderator" || role == "senior_moderator" || role == "administrator")
        val targetRole = discussion.authorProfile?.role ?: "usuario"
        
        if (canModerate) {
            val weightExecutor = getRoleWeight(role)
            val weightTarget = getRoleWeight(targetRole)
            
            // Só mostra opções se o executor for estritamente superior ao autor
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

            val builder = AlertDialog.Builder(this@DiscussionActivity)
            val isForum = category == "general"
            builder.setTitle(if (isForum) "Novo Tópico" else "Publicar no Mural")

            val layout = android.widget.LinearLayout(this@DiscussionActivity)
            layout.orientation = android.widget.LinearLayout.VERTICAL
            layout.setPadding(50, 40, 50, 10)

            val edtTitle = EditText(this@DiscussionActivity)
            if (isForum) {
                edtTitle.hint = "Título do Tópico"
                layout.addView(edtTitle)
            }

            val edtContent = EditText(this@DiscussionActivity)
            edtContent.hint = if (isForum) "Conteúdo da publicação" else "O que você está pensando? (máx 2000)"
            edtContent.minLines = 3
            layout.addView(edtContent)

            builder.setView(layout)

            builder.setPositiveButton("Publicar") { _, _ ->
                val titleInput = edtTitle.text.toString().trim()
                val content = edtContent.text.toString().trim()

                if (isForum && titleInput.isEmpty()) {
                    Toast.makeText(this@DiscussionActivity, "Digite um título", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (content.isEmpty()) {
                    Toast.makeText(this@DiscussionActivity, "Escreva algo para publicar", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (content.length > 2000) {
                    Toast.makeText(this@DiscussionActivity, "Texto muito longo", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Se for fórum, usa o título digitado. Se for mural, gera automático para compatibilidade.
                val title = if (isForum) titleInput else {
                    if (content.length > 50) content.take(47) + "..." else content
                }

                salvarNovoTopico(title, content, profile)
            }

            builder.setNegativeButton("Cancelar", null)
            builder.show()
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
}
