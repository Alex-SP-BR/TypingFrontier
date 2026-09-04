package com.typingfrontier.collection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.typingfrontier.PlayerManager
import com.typingfrontier.social.SocialProfileRepository
import com.typingfrontier.databinding.ActivityAvatarCollectionBinding

class AvatarCollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvatarCollectionBinding
    private lateinit var adapter: AvatarAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvatarCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFilters()

        binding.btnVoltar.setOnClickListener {
            finish()
        }

        binding.btnHelpAvatares.setOnClickListener {
            mostrarAjudaAvatares()
        }
    }

    private fun setupFilters() {
        binding.btnFilterTodos.setOnClickListener { carregarLista("TODOS") }
        binding.btnFilterProgressao.setOnClickListener { carregarLista("PROGRESSÃO") }
        binding.btnFilterColecao.setOnClickListener { carregarLista("COLEÇÃO") }
        binding.btnFilterAdmin.setOnClickListener { carregarLista("ADMINISTRATIVOS") }
    }

    private fun carregarLista(filtro: String) {
        val p = PlayerManager.player
        val lista = mutableListOf<Avatar>()
        
        // Avatar Padrão aparece apenas em "TODOS" (no topo) ou conforme lógica específica
        if (filtro == "TODOS") {
            lista.add(CollectionRepository.getAvatarPadrao(p.sexo))
        }

        lista.addAll(CollectionRepository.getAvataresPorCategoria(p.sexo, filtro))
        
        adapter.updateList(lista)
    }

    private fun mostrarAjudaAvatares() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Como funcionam os Avatares?")
            .setMessage("• Avatar Original: Visual padrão sempre disponível.\n" +
                        "• Desbloqueio: Alcance o nível necessário e complete os anúncios solicitados.\n" +
                        "• Equipar: Após desbloqueado, use o botão EQUIPAR para mudar seu visual.\n" +
                        "• Troca: Você pode alternar entre seus avatares ou voltar ao original a qualquer momento.\n" +
                        "• Visualizar: Toque na IMAGEM para ampliar o personagem.\n" +
                        "• Sexo: O sistema filtra automaticamente avatares masculinos e femininos conforme seu perfil.\n\n" +
                        "IMPORTANTE: Tocar na imagem apenas amplia. Para equipar, use o botão EQUIPAR.")
            .setPositiveButton("Entendi", null)
            .show()
    }

    private fun setupRecyclerView() {
        adapter = AvatarAdapter(emptyList()) {
            PlayerManager.save(this)
        }
        
        binding.rvAvatares.layoutManager = GridLayoutManager(this, 2)
        binding.rvAvatares.adapter = adapter

        // Garante que a role esteja carregada antes de popular a lista
        SocialProfileRepository.initializeSocialIdentity {
            runOnUiThread {
                carregarLista("TODOS")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}
