package com.typingfrontier

import com.typingfrontier.economy.ProfessionManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProfessionSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profession_select)
        val p = PlayerManager.player
        Toast.makeText(this, "Jogador: ${p.nome} (${p.sexo})", Toast.LENGTH_SHORT).show()


        findViewById<Button>(R.id.btnPolicial).setOnClickListener {
            escolherProfissao("Policial")
        }

        findViewById<Button>(R.id.btnMedico).setOnClickListener {
            escolherProfissao("Médico")
        }

        findViewById<Button>(R.id.btnEngenheiro).setOnClickListener {
            escolherProfissao("Engenheiro")
        }

        findViewById<Button>(R.id.btnProfessor).setOnClickListener {
            escolherProfissao("Professor")
        }

        findViewById<Button>(R.id.btnDetetive).setOnClickListener {
            escolherProfissao("Detetive")
        }
    }

    private fun escolherProfissao(profissao: String) {

        val player = PlayerManager.player
        player.profissao = profissao

        // 🔥 aplica atributos iniciais e equipamento
        ProfessionManager.aplicarBonusInicial(player)

        // Salva o progresso inicial
        PlayerManager.save(this)

        Toast.makeText(
            this,
            "Profissão: $profissao | Força: ${player.forca}",
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(this, CitySelectActivity::class.java)
        startActivity(intent)
        finish()
    }
}