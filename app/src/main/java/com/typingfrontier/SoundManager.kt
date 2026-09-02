package com.typingfrontier

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * SoundManager v2: Suporta playlists expandidas para evitar repetição.
 */
object SoundManager {

    private var mediaPlayer: MediaPlayer? = null
    private var currentCategory: String? = null
    
    private const val PREFS_NAME = "sound_settings"
    private const val KEY_MUSIC_ENABLED = "music_enabled"
    private const val KEY_VOLUME = "volume"

    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) pause() else resume()
        }
        
    var volume: Float = 0.4f
        set(value) {
            field = value
            try {
                mediaPlayer?.setVolume(value, value)
            } catch (e: Exception) {}
        }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isMusicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
        volume = prefs.getFloat(KEY_VOLUME, 0.4f)
    }

    fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_MUSIC_ENABLED, isMusicEnabled)
            putFloat(KEY_VOLUME, volume)
            apply()
        }
    }

    // Playlists expandidas. 
    // DICA: Se o Android Studio marcar algum nome em vermelho, significa que você ainda não colocou esse arquivo na pasta 'raw'.
    private val playlists = mapOf(
        "aventura" to listOf(
            R.raw.aventura_1, R.raw.aventura_2, R.raw.aventura_3, R.raw.aventura_4, R.raw.aventura_5,
            R.raw.aventura_6, R.raw.aventura_7, R.raw.aventura_8, R.raw.aventura_9, R.raw.aventura_10
        ),
        "suspense" to listOf(
            R.raw.suspense_1, R.raw.suspense_2, R.raw.suspense_3, R.raw.suspense_4, R.raw.suspense_5,
            R.raw.suspense_6, R.raw.suspense_7, R.raw.suspense_8, R.raw.suspense_9, R.raw.suspense_10
        ),
        "foco" to listOf(
            R.raw.foco_1, R.raw.foco_2, R.raw.foco_3, R.raw.foco_4, R.raw.foco_5
        ),
        "acao" to listOf(
            R.raw.acao_1, R.raw.acao_2, R.raw.acao_3, R.raw.acao_4, R.raw.acao_5
        )
    )

    fun play(context: Context, category: String) {
        playWithFadeIn(context, category, 0) // Sem fade por padrão se chamado assim
    }

    /**
     * Toca música com efeito de fade in.
     */
    fun playWithFadeIn(context: Context, category: String, durationMs: Int = 2000) {
        if (currentCategory == category && mediaPlayer?.isPlaying == true) return

        stop()

        val playlist = playlists[category] ?: return
        val randomTrack = playlist.random()

        try {
            currentCategory = category
            mediaPlayer = MediaPlayer.create(context, randomTrack)
            mediaPlayer?.isLooping = true
            
            val currentVol = if (isMusicEnabled) volume else 0f
            
            if (durationMs > 0 && isMusicEnabled) {
                mediaPlayer?.setVolume(0f, 0f)
                mediaPlayer?.start()
                
                val startVolume = 0f
                val endVolume = volume
                val steps = 20
                val interval = (durationMs / steps).toLong()
                
                Thread {
                    for (i in 1..steps) {
                        val v = startVolume + (endVolume - startVolume) * (i.toFloat() / steps)
                        try {
                            mediaPlayer?.setVolume(v, v)
                        } catch (e: Exception) {}
                        Thread.sleep(interval)
                    }
                }.start()
            } else {
                mediaPlayer?.setVolume(currentVol, currentVol)
                if (isMusicEnabled) mediaPlayer?.start()
            }
            
            Log.d("SoundManager", "Playlist: $category | Tocando com fade: $randomTrack")
        } catch (e: Exception) {
            Log.e("SoundManager", "Erro ao carregar áudio.")
        }
    }

    /**
     * Fade out e para a música.
     */
    fun fadeOutAndStop(durationMs: Int = 2000, onComplete: (() -> Unit)? = null) {
        val player = mediaPlayer ?: return
        if (!player.isPlaying) {
            stop()
            onComplete?.invoke()
            return
        }

        val startVolume = 0.4f
        val steps = 20
        val interval = (durationMs / steps).toLong()

        Thread {
            for (i in steps downTo 0) {
                val volume = startVolume * (i.toFloat() / steps)
                try {
                    player.setVolume(volume, volume)
                } catch (e: Exception) {}
                Thread.sleep(interval)
            }
            stop()
            onComplete?.invoke()
        }.start()
    }

    fun isMusicPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        if (isMusicEnabled && mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("SoundManager", "Erro ao parar MediaPlayer")
        } finally {
            mediaPlayer = null
            currentCategory = null
        }
    }
}
