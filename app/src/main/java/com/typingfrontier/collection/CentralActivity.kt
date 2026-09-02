package com.typingfrontier.collection

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.databinding.ActivityCentralBinding
import com.typingfrontier.social.SocialProfileRepository
import com.typingfrontier.social.AdminActivity

class CentralActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCentralBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCentralBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarInterfaceAdmin()

        binding.cardMeuPerfil.setOnClickListener {
            startActivity(Intent(this, com.typingfrontier.social.SocialProfileActivity::class.java))
        }

        binding.cardAvatares.setOnClickListener {
            startActivity(Intent(this, AvatarCollectionActivity::class.java))
        }

        binding.cardConquistas.setOnClickListener {
            startActivity(Intent(this, AchievementActivity::class.java))
        }

        binding.cardRanking.setOnClickListener {
            startActivity(Intent(this, com.typingfrontier.social.RankingActivity::class.java))
        }

        binding.cardForum.setOnClickListener {
            val intent = Intent(this, com.typingfrontier.social.DiscussionActivity::class.java)
            intent.putExtra("EXTRA_CATEGORY", "general")
            startActivity(intent)
        }

        binding.cardAdmin.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        configurarInterfaceAdmin()
    }

    private fun configurarInterfaceAdmin() {
        val profile = SocialProfileRepository.currentProfile
        
        if (profile == null) {
            // Tenta carregar caso ainda não esteja disponível
            SocialProfileRepository.initializeSocialIdentity {
                runOnUiThread { configurarInterfaceAdmin() }
            }
            binding.cardAdmin.visibility = View.GONE
            return
        }
        
        // Verifica se a role é moderator, senior_moderator ou administrator
        val temAcessoAdmin = profile.role == "moderator" || profile.role == "senior_moderator" || profile.role == "administrator"

        if (temAcessoAdmin) {
            binding.cardAdmin.visibility = View.VISIBLE
        } else {
            binding.cardAdmin.visibility = View.GONE
        }
    }
}
