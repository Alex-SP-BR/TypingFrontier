package com.typingfrontier

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class TypingFrontierApp : Application() {

    companion object {
        private lateinit var instance: TypingFrontierApp
        fun getAppContext(): android.content.Context = instance.applicationContext
    }

    private var activityCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var pauseRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Inicializa as configurações de som
        SoundManager.init(this)

        // Inicializa AdMob
        com.typingfrontier.utils.AdManager.init(this)

        // Carrega o save local
        PlayerManager.load(this)

        // Inicializa Supabase (Camada Social)
        com.typingfrontier.social.SupabaseManager.init()
        
        // Inicializa Identidade Social e Sincroniza Ranking (Offline-first)
        com.typingfrontier.social.SocialProfileRepository.initializeSocialIdentity {
            com.typingfrontier.social.SocialProfileRepository.syncStatistics()
        }
        
        // Inicia o serviço para monitorar se o usuário "limpa" o app dos recentes
        try {
            startService(Intent(this, MusicService::class.java))
        } catch (e: Exception) {}

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            
            override fun onActivityStarted(activity: Activity) {
                // Cancela qualquer pausa pendente pois uma nova tela abriu
                pauseRunnable?.let { handler.removeCallbacks(it) }
                pauseRunnable = null

                if (activityCount == 0) {
                    // App voltando do background para o primeiro plano
                    SoundManager.resume()
                }
                activityCount++
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                // Sincroniza ranking social em background quando o jogador sai de uma tela
                com.typingfrontier.social.SocialProfileRepository.syncStatistics()

                activityCount--
                if (activityCount == 0) {
                    // Se não abrir nenhuma outra tela em 500ms, o app foi para o background
                    pauseRunnable = Runnable {
                        if (activityCount == 0) {
                            SoundManager.pause()
                        }
                    }
                    handler.postDelayed(pauseRunnable!!, 500)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
