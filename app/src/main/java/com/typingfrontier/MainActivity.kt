package com.typingfrontier

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.typingfrontier.databinding.ActivityMainBinding
import com.typingfrontier.utils.AdManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ajuste da Barra de Status para o novo fundo escuro da Abertura
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        // Força o aplicativo a usar sempre o modo claro, ignorando o modo escuro do sistema
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fluxo de Consentimento UMP (Posterga o início para após o primeiro frame da UI ser desenhado)
        binding.root.post {
            AdManager.iniciarFluxoConsentimento(this)
        }

        binding.btnStart.setOnClickListener {
            GameActivity.startTimeNanos = SystemClock.elapsedRealtimeNanos()
            Log.d("TF_PERF_START", "TF_PERF_START T0: Touch on btnStart (+0.0 ms)")

            lifecycleScope.launch {
                PlayerManager.awaitLoaded()
                val p = PlayerManager.player
                
                // Lógica de Abertura Única
                if (!p.introConcluida) {
                    startActivity(Intent(this@MainActivity, IntroActivity::class.java))
                    return@launch
                }

                // Se o jogador já tem nome e profissão, vai direto pro jogo
                if (p.nome.isNotEmpty() && p.profissao.isNotEmpty()) {
                    val t1 = (SystemClock.elapsedRealtimeNanos() - GameActivity.startTimeNanos) / 1_000_000.0
                    Log.d("TF_PERF_START", "TF_PERF_START T1: Immediately before startActivity (+${String.format(java.util.Locale.US, "%.1f", t1)} ms)")
                    startActivity(Intent(this@MainActivity, GameActivity::class.java))
                } else {
                    val intent = Intent(this@MainActivity, CreateCharacterActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
