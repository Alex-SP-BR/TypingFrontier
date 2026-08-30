package com.typingfrontier.collection

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.typingfrontier.databinding.ActivityCentralBinding

class CentralActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCentralBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCentralBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardAvatares.setOnClickListener {
            startActivity(Intent(this, AvatarCollectionActivity::class.java))
        }

        binding.cardConquistas.setOnClickListener {
            startActivity(Intent(this, AchievementActivity::class.java))
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }
}
