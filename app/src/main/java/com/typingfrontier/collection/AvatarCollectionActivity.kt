package com.typingfrontier.collection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.typingfrontier.PlayerManager
import com.typingfrontier.databinding.ActivityAvatarCollectionBinding

class AvatarCollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvatarCollectionBinding
    private lateinit var adapter: AvatarAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvatarCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        binding.btnVoltar.setOnClickListener {
            finish()
        }

        binding.btnHelpAvatares.setOnClickListener {
            mostrarAjudaAvatares()
        }
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
        val p = PlayerManager.player
        val avataresNivel = CollectionRepository.getAvataresPorSexo(p.sexo)
        
        // Adiciona o Avatar Padrão no topo da lista
        val listaCompleta = mutableListOf<Avatar>()
        listaCompleta.add(CollectionRepository.getAvatarPadrao(p.sexo))
        listaCompleta.addAll(avataresNivel)
        
        adapter = AvatarAdapter(listaCompleta) {
            // Callback quando um avatar é equipado
            PlayerManager.save(this)
        }
        
        binding.rvAvatares.layoutManager = GridLayoutManager(this, 2)
        binding.rvAvatares.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}
