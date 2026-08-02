package com.typingfrontier

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.missions.detetive.DetetiveMissionRepository

class DetetiveMissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detetive_mission)

        val txtTitulo = findViewById<TextView>(R.id.txtTitulo)
        val txtDescricao = findViewById<TextView>(R.id.txtDescricao)
        val btnA = findViewById<Button>(R.id.btnEscolhaA)
        val btnB = findViewById<Button>(R.id.btnEscolhaB)

        val missao = DetetiveMissionRepository.getMissao()

        txtTitulo.text = missao.titulo
        txtDescricao.text = missao.descricao
        btnA.text = missao.escolhaA
        btnB.text = missao.escolhaB

        fun resolver(escolha: Char) {
            if (escolha == missao.correta) {
                GameEngine.dispatch(GameAction.CompleteMission(missao.recompensaXP, 0))
                txtDescricao.text = "✅ Decisão correta!\n\n${missao.explicacao}"
            } else {
                txtDescricao.text = "❌ Decisão arriscada.\n\n${missao.explicacao}"
            }

            btnA.isEnabled = false
            btnB.isEnabled = false
            PlayerManager.save(this)
        }

        btnA.setOnClickListener { resolver('A') }
        btnB.setOnClickListener { resolver('B') }
    }
}
