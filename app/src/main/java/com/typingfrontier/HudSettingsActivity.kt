package com.typingfrontier

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.typingfrontier.HudSettingsManager.HudCategory

class HudSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ajuste da Barra de Status para o tema escuro
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_hud_settings)

        setupCategory(R.id.settingExploreXp, HudCategory.EXPLORE, "Nível e XP", "xp")
        setupCategory(R.id.settingExploreHp, HudCategory.EXPLORE, "Vida", "hp")
        setupCategory(R.id.settingExplorePhysical, HudCategory.EXPLORE, "Energia Física", "physical")
        setupCategory(R.id.settingExploreMental, HudCategory.EXPLORE, "Energia Mental", "mental")

        setupCategory(R.id.settingAdventureXp, HudCategory.ADVENTURE, "Nível e XP", "xp")
        setupCategory(R.id.settingAdventureHp, HudCategory.ADVENTURE, "Vida", "hp")
        setupCategory(R.id.settingAdventurePhysical, HudCategory.ADVENTURE, "Energia Física", "physical")
        setupCategory(R.id.settingAdventureMental, HudCategory.ADVENTURE, "Energia Mental", "mental")

        setupCategory(R.id.settingPhysicalXp, HudCategory.TRAINING_PHYSICAL, "Nível e XP", "xp")
        setupCategory(R.id.settingPhysicalHp, HudCategory.TRAINING_PHYSICAL, "Vida", "hp")
        setupCategory(R.id.settingPhysicalPhysical, HudCategory.TRAINING_PHYSICAL, "Energia Física", "physical")
        setupCategory(R.id.settingPhysicalMental, HudCategory.TRAINING_PHYSICAL, "Energia Mental", "mental")

        setupCategory(R.id.settingMentalXp, HudCategory.TRAINING_MENTAL, "Nível e XP", "xp")
        setupCategory(R.id.settingMentalHp, HudCategory.TRAINING_MENTAL, "Vida", "hp")
        setupCategory(R.id.settingMentalPhysical, HudCategory.TRAINING_MENTAL, "Energia Física", "physical")
        setupCategory(R.id.settingMentalMental, HudCategory.TRAINING_MENTAL, "Energia Mental", "mental")

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            HudSettingsManager.saveSettings(this)
            finish()
        }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                HudSettingsManager.saveSettings(this@HudSettingsActivity)
                finish()
            }
        })
    }

    private fun setupCategory(containerId: Int, category: HudCategory, label: String, type: String) {
        val container = findViewById<View>(containerId)
        val textView = container.findViewById<TextView>(R.id.txtLabel)
        val switch = container.findViewById<SwitchMaterial>(R.id.switchHud)

        textView.text = label
        
        val isVisible = when(type) {
            "xp" -> HudSettingsManager.isXpVisible(category)
            "hp" -> HudSettingsManager.isHealthVisible(category)
            "physical" -> HudSettingsManager.isPhysicalEnergyVisible(category)
            "mental" -> HudSettingsManager.isMentalEnergyVisible(category)
            else -> true
        }
        
        switch.isChecked = isVisible
        
        switch.setOnCheckedChangeListener { _, isChecked ->
            when(type) {
                "xp" -> HudSettingsManager.setXpVisible(category, isChecked)
                "hp" -> HudSettingsManager.setHealthVisible(category, isChecked)
                "physical" -> HudSettingsManager.setPhysicalEnergyVisible(category, isChecked)
                "mental" -> HudSettingsManager.setMentalEnergyVisible(category, isChecked)
            }
        }
    }
}
