package com.typingfrontier

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.databinding.ActivityCreateCharacterBinding
import com.typingfrontier.social.SocialProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CreateCharacterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCharacterBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCharacterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarBotao()
    }

    private fun configurarBotao() {
        binding.btnContinuar.setOnClickListener {
            val nome = binding.edtNome.text.toString().trim()
            val username = binding.edtUsername.text.toString().trim()

            if (nome.isEmpty()) {
                Toast.makeText(this, "Digite um nome para o personagem", Toast.LENGTH_SHORT).show()
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

            // Se o username estiver vazio, prossegue apenas localmente
            if (username.isEmpty()) {
                prosseguirCriacao(nome, sexo)
                return@setOnClickListener
            }

            if (username.length < 3) {
                Toast.makeText(this, "Username muito curto (min 3)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tenta validar e criar perfil social se houver username
            binding.btnContinuar.isEnabled = false
            Toast.makeText(this, "Validando identidade social...", Toast.LENGTH_SHORT).show()

            scope.launch {
                try {
                    val disponivel = SocialProfileRepository.isUsernameAvailable(username)
                    
                    if (!disponivel) {
                        binding.btnContinuar.isEnabled = true
                        Toast.makeText(this@CreateCharacterActivity, "Este nome social já está em uso. Escolha outro.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    SocialProfileRepository.createSocialProfile(username, nome)
                } catch (e: Exception) {
                    // Erro de conexão/rede: permite prosseguir offline com a mensagem amigável solicitada
                    Toast.makeText(this@CreateCharacterActivity, 
                        "Sem conexão com a internet\n\nNão foi possível registrar sua Identidade Social agora. " +
                        "Você pode continuar jogando normalmente. Quando estiver conectado à internet, " +
                        "poderá criar sua Identidade Social pelo seu Perfil.", 
                        Toast.LENGTH_LONG).show()
                }

                prosseguirCriacao(nome, sexo)
            }
        }
    }

    private fun prosseguirCriacao(nome: String, sexo: String) {
        val player = PlayerManager.player
        player.nome = nome
        player.sexo = sexo

        val intent = Intent(this, ProfessionSelectActivity::class.java)
        startActivity(intent)
        finish()
    }
}
