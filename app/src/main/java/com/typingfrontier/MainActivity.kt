package com.typingfrontier

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.typingfrontier.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Força o aplicativo a usar sempre o modo claro, ignorando o modo escuro do sistema
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tenta carregar o jogador existente
        PlayerManager.load(this)

        binding.btnStart.setOnClickListener {
            val p = PlayerManager.player
            
            // Lógica de Abertura Única
            if (!p.introConcluida) {
                startActivity(Intent(this, IntroActivity::class.java))
                return@setOnClickListener
            }

            // Se o jogador já tem nome e profissão, vai direto pro jogo
            if (p.nome.isNotEmpty() && p.profissao.isNotEmpty()) {
                startActivity(Intent(this, GameActivity::class.java))
            } else {
                val intent = Intent(this, CreateCharacterActivity::class.java)
                startActivity(intent)
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
