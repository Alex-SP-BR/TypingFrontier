package com.typingfrontier

import android.content.Context

/**
 * Gerenciador independente para as preferências de exibição do HUD do personagem.
 * Segue o padrão de persistência local via SharedPreferences utilizado no projeto.
 */
object HudSettingsManager {

    private const val PREFS_NAME = "hud_display_settings"

    /**
     * Categorias de telas onde o HUD pode ser personalizado.
     */
    enum class HudCategory {
        EXPLORE,
        ADVENTURE,
        TRAINING_PHYSICAL,
        TRAINING_MENTAL
    }

    // Cache em memória para acesso rápido durante o jogo, similar ao SoundManager
    private val settingsCache = mutableMapOf<String, Boolean>()

    /**
     * Inicializa o gerenciador carregando as preferências salvas.
     * Deve ser chamado na inicialização do aplicativo.
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        HudCategory.values().forEach { category ->
            val prefix = category.name
            settingsCache["${prefix}_SHOW_XP"] = prefs.getBoolean("${prefix}_SHOW_XP", getDefaultXp(category))
            settingsCache["${prefix}_SHOW_HP"] = prefs.getBoolean("${prefix}_SHOW_HP", getDefaultHp(category))
            settingsCache["${prefix}_SHOW_PHYSICAL_ENERGY"] = prefs.getBoolean("${prefix}_SHOW_PHYSICAL_ENERGY", getDefaultPhysical(category))
            settingsCache["${prefix}_SHOW_MENTAL_ENERGY"] = prefs.getBoolean("${prefix}_SHOW_MENTAL_ENERGY", getDefaultMental(category))
        }
    }

    /**
     * Salva as preferências atuais no armazenamento persistente.
     */
    fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        settingsCache.forEach { (key, value) ->
            editor.putBoolean(key, value)
        }
        editor.apply()
    }

    // --- MÉTODOS DE ACESSO ---

    fun isXpVisible(category: HudCategory): Boolean {
        return settingsCache["${category.name}_SHOW_XP"] ?: getDefaultXp(category)
    }

    fun setXpVisible(category: HudCategory, visible: Boolean) {
        settingsCache["${category.name}_SHOW_XP"] = visible
    }

    fun isHealthVisible(category: HudCategory): Boolean {
        return settingsCache["${category.name}_SHOW_HP"] ?: getDefaultHp(category)
    }

    fun setHealthVisible(category: HudCategory, visible: Boolean) {
        settingsCache["${category.name}_SHOW_HP"] = visible
    }

    fun isPhysicalEnergyVisible(category: HudCategory): Boolean {
        return settingsCache["${category.name}_SHOW_PHYSICAL_ENERGY"] ?: getDefaultPhysical(category)
    }

    fun setPhysicalEnergyVisible(category: HudCategory, visible: Boolean) {
        settingsCache["${category.name}_SHOW_PHYSICAL_ENERGY"] = visible
    }

    fun isMentalEnergyVisible(category: HudCategory): Boolean {
        return settingsCache["${category.name}_SHOW_MENTAL_ENERGY"] ?: getDefaultMental(category)
    }

    fun setMentalEnergyVisible(category: HudCategory, visible: Boolean) {
        settingsCache["${category.name}_SHOW_MENTAL_ENERGY"] = visible
    }

    // --- DEFAULTS ---

    private fun getDefaultXp(category: HudCategory): Boolean {
        return when (category) {
            HudCategory.EXPLORE, HudCategory.ADVENTURE -> true
            HudCategory.TRAINING_PHYSICAL, HudCategory.TRAINING_MENTAL -> false
        }
    }

    private fun getDefaultHp(category: HudCategory): Boolean {
        return when (category) {
            HudCategory.EXPLORE, HudCategory.ADVENTURE -> true
            HudCategory.TRAINING_PHYSICAL, HudCategory.TRAINING_MENTAL -> false
        }
    }

    private fun getDefaultPhysical(category: HudCategory): Boolean {
        return when (category) {
            HudCategory.ADVENTURE, HudCategory.TRAINING_PHYSICAL, HudCategory.TRAINING_MENTAL -> true
            HudCategory.EXPLORE -> false
        }
    }

    private fun getDefaultMental(category: HudCategory): Boolean {
        return when (category) {
            HudCategory.ADVENTURE, HudCategory.TRAINING_PHYSICAL, HudCategory.TRAINING_MENTAL -> true
            HudCategory.EXPLORE -> false
        }
    }
}
