package com.typingfrontier.collection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.typingfrontier.databinding.ActivityAchievementBinding

class AchievementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        binding.btnVoltar.setOnClickListener {
            finish()
        }

        binding.btnHelpConquistas.setOnClickListener {
            mostrarAjudaConquistas()
        }
    }

    private fun mostrarAjudaConquistas() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Como funcionam as Conquistas?")
            .setMessage("• Marcos: Conquistas celebram sua evolução no jogo.\n" +
                        "• Automático: Ao cumprir o objetivo, a conquista é desbloqueada instantaneamente.\n" +
                        "• Insígnia: Cada marco possui uma imagem única. Toque nela para ver em tamanho maior.\n" +
                        "• Categorias: Exploração, Treino Físico, Treino Mental, Economia e Supremas.\n" +
                        "• Recompensas: Podem conceder Frons ou até avatares exclusivos.\n" +
                        "• Exploração: Requer concluir as 5 etapas da aventura com sucesso.\n\n" +
                        "IMPORTANTE: Visualizar uma insígnia não altera seu progresso.")
            .setPositiveButton("Entendi", null)
            .show()
    }

    private fun setupRecyclerView() {
        val conquistas = CollectionRepository.getAllAchievements()
        val adapter = AchievementAdapter(conquistas)
        
        binding.rvConquistas.layoutManager = LinearLayoutManager(this)
        binding.rvConquistas.adapter = adapter
    }
}
