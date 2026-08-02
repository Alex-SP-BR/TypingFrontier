package com.typingfrontier

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.databinding.ActivityCreateCharacterBinding

class CreateCharacterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCharacterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCharacterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarBotao()
    }

    private fun configurarBotao() {
        binding.btnContinuar.setOnClickListener {

            val nome = binding.edtNome.text.toString().trim()

            if (nome.isEmpty()) {
                Toast.makeText(this, "Digite um nome", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sexo = when (binding.rgSexo.checkedRadioButtonId) {
                binding.rbMasculino.id -> "Masculino"
                binding.rbFeminino.id -> "Feminino"
                else -> {
                    Toast.makeText(this, "Selecione o sexo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Salva no PlayerManager
            val player = PlayerManager.player
            player.nome = nome
            player.sexo = sexo

            // Vai para escolha de profissão
            val intent = Intent(this, ProfessionSelectActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}