package com.typingfrontier

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class TypingFrontierApp : Application() {

    private var activityCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var pauseRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        
        // Inicializa as configurações de som
        SoundManager.init(this)
        
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
