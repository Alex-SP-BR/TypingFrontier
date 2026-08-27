package com.typingfrontier

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.btnWatchIntro).setOnClickListener {
            val intent = Intent(this, IntroActivity::class.java)
            intent.putExtra("isReview", true)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnManual).setOnClickListener {
            startActivity(Intent(this, ManualActivity::class.java))
        }

        findViewById<Button>(R.id.btnAbout).setOnClickListener {
            showAboutDialog()
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // ÁUDIO
        val switchMusic = findViewById<SwitchMaterial>(R.id.switchMusic)
        val sliderVolume = findViewById<Slider>(R.id.sliderVolume)

        switchMusic.isChecked = SoundManager.isMusicEnabled
        sliderVolume.value = SoundManager.volume
        
        switchMusic.setOnCheckedChangeListener { _, isChecked ->
            SoundManager.isMusicEnabled = isChecked
            SoundManager.saveSettings(this)
        }

        sliderVolume.addOnChangeListener { _, value, _ ->
            SoundManager.volume = value
            SoundManager.saveSettings(this)
        }
    }

    private fun showAboutDialog() {
        val version = BuildConfig.VERSION_NAME

        AlertDialog.Builder(this)
            .setTitle("TypingFrontier")
            .setMessage("Desenvolvido por\n© Alex Cardoso Bento\n\nVersão $version")
            .setPositiveButton("OK", null)
            .show()
    }
}
