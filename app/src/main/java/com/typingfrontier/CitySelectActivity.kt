package com.typingfrontier

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.databinding.ActivityCitySelectBinding

class CitySelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCitySelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCitySelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSaoPaulo.setOnClickListener {

            val player = PlayerManager.player
            player.cidadeNascimento = "São Paulo"

            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}